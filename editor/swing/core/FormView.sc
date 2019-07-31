import sc.lang.java.ModelUtil;
import sc.lang.java.ExecutionContext;
import sc.lang.java.BodyTypeDeclaration;
import sc.lang.java.TypeDeclaration;
import sc.lang.java.JavaModel;
import sc.lang.java.JavaSemanticNode;
import sc.lang.sc.PropertyAssignment;
import sc.lang.EditorContext;
import sc.lang.InstanceWrapper;
import sc.type.RTypeUtil;
import sc.util.StringUtil;

import sc.type.IBeanMapper;

import java.util.Iterator;

import java.awt.Rectangle;

FormView {
   static int FORM_NUM_STATIC_COMPONENTS = 0;

   Object getDefaultCurrentObj(Object type) {
      return editorModel.ctx.getDefaultCurrentObj(type);
   }

   void setDefaultCurrentObj(Object type, Object obj) {
      editorModel.ctx.setDefaultCurrentObj(type, obj);
   }

   void focusChanged(JComponent component, Object prop, Object inst, boolean focus) {
      if (focus) {
         if (editorModel.currentProperty != prop || editorModel.currentInstance != inst) {
            if (component instanceof JTextField)
               currentTextField = (JTextField) component;
            else
               currentTextField = null;

            editorModel.changeFocus(prop, inst);
         }
      }
      else if (!focus && editorModel.currentProperty == prop) {
         // Switching focus to the status panel should not alter the current property.
         //currentProperty = null;
         //currentTextField = null;
      }
   }

   // Some editor operations we make from the UI will change the model and cause a form rebuild.  A quick way to avoid those - just set this to true before making those types of changes
   boolean disableFormRebuild = false;

   int x = numCols;

   // For FormView, this is a copy of the repeatComponents list.  As we add children as a swing child, we add it to childViews.
   List<IElementEditor> childViews = new ArrayList<IElementEditor>();

   List<IElementEditor> getChildViews() {
      return childViews;
   }

   void invalidateModel() {
      super.invalidateModel();
   }

   void rebuildForm() {
      contentPanel.childList.rebuildList();
      contentPanel.childList.validateTree();
   }

   contentPanel {
      int maxChildWidth, maxChildHeight;
      preferredSize := SwingUtil.dimension(maxChildWidth, maxChildHeight);
      size := SwingUtil.dimension(maxChildWidth, maxChildHeight);

      object childList extends RepeatComponent<FormEditor> {
         manageChildren = false; // We call SwingUtil.addChild explicitly when synchronizing the childViews array

         repeat := editorModel.visibleTypes;

         parentComponent = contentPanel;

         int numRows := (DynUtil.getArrayLength(repeat) + numCols-1) / numCols;

         int oldNumRows, oldNumCols;

         // No need to refresh
         disableRefresh := !FormView.this.visible;

         FormEditor[][] viewsGrid;

         public FormEditor createRepeatElement(Object listElem, int ix, Object oldComp) {
            BodyTypeDeclaration currentType = (BodyTypeDeclaration) listElem;
            Object currentObj = getObjectForListElement(ix);
            InstanceWrapper wrapper = getWrapperForListElement(ix);
            /*
            Object currentObj = ModelUtil.isObjectType(currentType) ? editorModel.system.resolveName(currentType.getFullTypeName(), false) :
                                                                      getDefaultCurrentObj(currentType);

             */
            FormEditor editor = new FormEditor(FormView.this, null, null, currentType, currentObj, ix, wrapper);
            updateCell(editor, ix);

            return editor;
         }
         
         public Object getRepeatVar(FormEditor component) {
            return component.getElemToEdit();
         }

         public void setRepeatIndex(FormEditor component, int ix) {
         }

         private void updateCell(FormEditor fed, int ix) {
            fed.row = ix / numCols;
            fed.col = ix % numCols;
            if (fed.row != 0)
               fed.prev = viewsGrid[fed.row-1][fed.col];
            else
               fed.prev = null;

            viewsGrid[fed.row][fed.col] = fed;
         }

         public boolean refreshList() {
             int size = DynUtil.getArrayLength(repeat);

             // Controls the number of columns used to display for the number of types we have selected
             numCols = size <= 1 ? 1 : size <= 4 ? 2 : size <= 9 ? 3 : 4;

             boolean gridChanged = false;
             if (oldNumRows != numRows || oldNumCols != numCols) {
                gridChanged = true;
                viewsGrid = new FormEditor[numRows][numCols];
             }

             boolean anyChanges = super.refreshList();

             if (gridChanged || anyChanges) {
                 int ix = 0;
                 for (Object elem:repeatComponents) {
                     FormEditor fed = (FormEditor) elem;
                     updateCell(fed, ix);
                     ix++;
                 }
             }

             return anyChanges;
         }

         void listChanged() {
            validateTree();
            // TODO: and remove explicit repaint/stuff below?
            // super.listChanged();
         }

         public void validateTree() {
            int newMaxChildX = 0;
            int newMaxChildY = 0;

            int curIx = 0;
            for (Object elem:repeatComponents) {
               FormEditor fed = (FormEditor) elem;

               if (childViews.size() <= curIx) {
                  childViews.add(fed);
                  SwingUtil.addChild(parentComponent, fed);
               }
               else if (childViews.get(curIx) != fed) {
                  remove(curIx);
                  childViews.set(curIx, fed);
                  SwingUtil.addChild(parentComponent, fed);
               }
               fed.updateListeners(true);
               curIx++;
            }
            while (curIx < childViews.size()) {
                remove(curIx);
                childViews.remove(curIx);
            }
            for (Object elem:repeatComponents) {
                if (elem instanceof TypeEditor) {
                   ((TypeEditor) elem).validateEditorTree();
                }
                Rectangle bounds = SwingUtil.getBoundingRectangle(elem);
                int newX = (int) (bounds.x + bounds.width + xpad);
                int newY = (int) (bounds.y + bounds.height + ypad);
                if (newX > newMaxChildX)
                   newMaxChildX = newX;
                if (newY > newMaxChildY)
                   newMaxChildY = newY;
            }
            if (maxChildWidth != newMaxChildX)
               maxChildWidth = newMaxChildX;
            if (maxChildHeight != newMaxChildY)
               maxChildHeight = newMaxChildY;

            // TODO: replace with super.listChanged call above?
            invalidate();
            FormView.this.invalidate();
            doLayout();
            FormView.this.doLayout();
            validate();
            FormView.super.validate();
            FormView.this.repaint();

            currentTextField = null;
         }
      }
   }
}
