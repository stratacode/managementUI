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
   /** Current selected widget (if any) */
   JTextField currentTextField;

   static int FORM_NUM_STATIC_COMPONENTS = 0;

   // Some editor operations we make from the UI will change the model and cause a form rebuild.  A quick way to avoid those - just set this to true before making those types of changes
   boolean disableFormRebuild = false;

   int x = numCols;

   contentPanel {
      int maxChildWidth, maxChildHeight;
      preferredSize := SwingUtil.dimension(maxChildWidth, maxChildHeight);

      object childList extends RepeatComponent {
         repeat := editorModel.visibleTypes;

         int numRows := (DynUtil.getArrayLength(repeat) + numCols-1) / numCols;

         int oldNumRows, oldNumCols;

         // No need to refresh
         disableRefresh := !visible;

         FormEditor[][] viewsGrid;

         public Object createRepeatElement(Object listElem, int ix, Object oldComp) {
            FormEditor editor = new FormEditor(FormView.this, null, (BodyTypeDeclaration) listElem);

            updateCell(editor, ix);

            return editor;
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

         public void refreshList() {
             int size = DynUtil.getArrayLength(repeat);
             numCols = size <= 1 ? 1 : size <= 4 ? 2 : size <= 9 ? 3 : 4;

             boolean gridChanged = false;
             if (oldNumRows != numRows || oldNumCols != numCols) {
                gridChanged = true;
                viewsGrid = new FormEditor[numRows][numCols];
             }

             super.refreshList();

             if (gridChanged) {
                 int ix = 0;
                 for (Object elem:repeatComponents) {
                     FormEditor fed = (FormEditor) elem;
                     updateCell(fed, ix);
                     ix++;
                 }
             }
             validateTree();
         }

         public void validateTree() {
            int newMaxChildWidth = 0;
            int newMaxChildHeight = 0;
            for (Object elem:repeatComponents) {
               FormEditor fed = (FormEditor) elem;
               fed.updateListeners();
            }
            for (Object elem:repeatComponents) {
                Rectangle bounds = SwingUtil.getBoundingRectangle(elem);
                int newW = bounds.x + bounds.width + xpad;
                int newH = bounds.y + bounds.height + ypad;
                if (newW > newMaxChildWidth)
                   newMaxChildWidth = newW;
                if (newH > newMaxChildHeight)
                   newMaxChildHeight = newH;
            }
            if (maxChildWidth != newMaxChildWidth)
               maxChildWidth = newMaxChildWidth;
            if (maxChildHeight != newMaxChildHeight)
               maxChildHeight = newMaxChildHeight;

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

  ClassView newFormView(Object type, ExecutionContext ctx, Object parentType, ClassView parentView) {
     Object[] props = editorModel.getPropertiesForType(type);
     Object parentObj = parentView == null ? ctx.getCurrentObject() : parentView.instance;
     boolean checkCurrentObject = parentType == null || parentObj != null;

     String typeName = ModelUtil.getTypeName(type);
     ClassView view;
     Object currentObj = null, currentType;
     if (ModelUtil.isObjectProperty(type)) {  // Do not include non-static scopes here...
        currentObj = parentObj == null ? (checkCurrentObject ? editorModel.system.resolveName(typeName, true) : getDefaultCurrentObj(type)) : DynUtil.getPropertyPath(parentObj, CTypeUtil.getClassName(ModelUtil.getInnerTypeName(type)));

        // resolveName can return a type, not an instance
        if (checkCurrentObject && DynUtil.isType(currentObj)) {
           currentType = currentObj;
           currentObj = null;
        }
        if (ModelUtil.isLayerType(type))
           view = new LayerView(parentView, type, props, currentObj);
        else
           view = new InstanceView(parentView, type, props, currentObj);
     }
     else {
        view = new ClassView(parentView, type, props, getDefaultCurrentObj(type));
     }

/*
     currentType = type;
     try {
        if (currentObj != null)
           ctx.pushCurrentObject(currentObj);
        else
           ctx.pushStaticFrame(currentType);
        view.validateGroup(ctx);
     }
     finally {
        if (currentObj != null)
           ctx.popCurrentObject();
        else
           ctx.popStaticFrame();
     }
*/
     return view;
  }

   void focusChanged(JComponent component, Object prop, Object inst, boolean focus) {
      if (focus) {
         if (editorModel.currentProperty != prop || editorModel.currentInstance != inst) {
            if (component instanceof JTextField)
               currentTextField = (JTextField) component;
            else
               currentTextField = null;

            editorModel.currentProperty = prop;
            editorModel.currentInstance = inst;
         }
      }
      else if (!focus && editorModel.currentProperty == prop) {
         // Switching focus to the status panel should not alter the current property.
         //currentProperty = null;
         //currentTextField = null;
      }
   }

   void stop() {
      contentPanel.removeForm();
   }
}
