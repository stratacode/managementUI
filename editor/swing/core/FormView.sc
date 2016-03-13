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


FormView {
   /** Current selected widget (if any) */
   JTextField currentTextField;

   Object getDefaultCurrentObj(Object type) {
      return editorModel.ctx.getDefaultCurrentObj(type);
   }

   void setDefaultCurrentObj(Object type, Object obj) {
      editorModel.ctx.setDefaultCurrentObj(type, obj);
   }

   static int FORM_NUM_STATIC_COMPONENTS = 0;

   int tabSize = 140;

   static class LinkButton extends JButton implements MouseListener {
      borderPainted = false;
      opaque = false;
      border = BorderFactory.createEmptyBorder();
      rolloverEnabled = true;
      Color defaultColor;
      cursor = new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR);


      {
         addMouseListener(this);
      }

      public void mouseClicked(MouseEvent e) {}

      public void mousePressed(MouseEvent e) {}

      public void mouseReleased(MouseEvent e) {}

      public void mouseEntered(MouseEvent e) {
         if (defaultColor == null)
            defaultColor = foreground;
         foreground = Color.BLUE;
      }

      public void mouseExited(MouseEvent e) {
         if (defaultColor != null)
            foreground = defaultColor;
      }
   }

   // Some editor operations we make from the UI will change the model and cause a form rebuild.  A quick way to avoid those - just set this to true before making those types of changes
   boolean disableFormRebuild = false;

   // The form view panel for classes, objects, etc.
   ClassView implements IElementView {
     ClassView parentView;
     Object[] properties;
     String operatorName := type == null ? null : ModelUtil.isEnumType(type) ? "enum" : ModelUtil.isEnum(type) ? "enum constant" : ModelUtil.isInterface(type) ? "interface" : "class";
     IElementView lastView;
     IElementView prev;

     ArrayList<IElementView> childViews = new ArrayList<IElementView>();

     int numStaticComponents = 5;

     boolean removed = false;

     @Bindable
     int x := col * (xpad + columnWidth) + (nestWidth/2) + xpad,
         y := prev == null ? ypad + (parentView == null ? 0 : parentView.startY) : prev.y + prev.height + ypad,
         width := columnWidth - (nestLevel * (nestWidth + 2*xpad)),
         height := (lastView == null ? startY : lastView.y + lastView.height) + 2 * ypad + borderSize + borderBottom;

     int row, col;

     size := SwingUtil.dimension(width, height);
     location := SwingUtil.point(x, y);

     boolean transparentType := !ModelUtil.isTypeInLayer(type, classViewLayer);

     int titleBorderX = 30;

     Object extType := ModelUtil.getExtendsClass(type);

     String title := (type == null ? "" : operatorName + " " + ModelUtil.getClassName(type)) +
                    (extType == null ? "" : " extends " + ModelUtil.getClassName(extType));

     // A border around the contents of the group with the group name
     //border := BorderFactory.createCompoundBorder (BorderFactory.createEmptyBorder(borderSize, borderSize, borderSize, borderSize), BorderFactory.createTitledBorder(title));
     //border := BorderFactory.createTitledBorder(title);
     object border extends GapBorder {
        border = BorderFactory.createEtchedBorder();
        gapX := titleBorderX + xpad;
        gapW := (int) (objectLabel.size.width + nameButton.size.width + extendsLabel.size.width + extendsTypeButton.size.width + instanceList.size.width + 6*xpad);
     }

     int borderTitleY = 0;

     object objectLabel extends JLabel {
        location := SwingUtil.point(2*xpad + borderSize + titleBorderX, borderTitleY + baseline);
        icon := GlobalResources.lookupIcon(type);
        size := preferredSize;
        text := operatorName;
        border = BorderFactory.createEmptyBorder();
     }

     object nameButton extends LinkButton {
        location := SwingUtil.point(objectLabel.location.x + objectLabel.size.width + xpad, borderTitleY + baseline);
        size := preferredSize;
        text := type == null ? "<null>" : ModelUtil.getClassName(type);
        clickCount =: editorModel.changeCurrentType(type);
        foreground := transparentType ? GlobalResources.transparentTextColor : GlobalResources.normalTextColor;
     }

     object extendsLabel extends JLabel {
        location := SwingUtil.point(nameButton.location.x + nameButton.size.width + xpad, borderTitleY + baseline);
        size := preferredSize;
        text := extType == null ? "" : "extends";
        border = BorderFactory.createEmptyBorder();
     }

     object extendsTypeButton extends LinkButton {
        location := SwingUtil.point(extendsLabel.location.x + extendsLabel.size.width + xpad, borderTitleY + baseline);
        size := preferredSize;
        text := extType == null ? "" : ModelUtil.getClassName(extType);
        clickCount =: editorModel.changeCurrentType(extType);
     }

     object instanceList extends JComboBox {
        location := SwingUtil.point(extendsTypeButton.location.x + extendsTypeButton.size.width + xpad, borderTitleY);
        size := preferredSize;
        items := editorModel.ctx.getInstancesOfType(type, 10, true);
        userSelectedItem =: userSelectedInstance((InstanceWrapper) selectedItem);
     }

     ClassView(ClassView parentView, Object type, Object[] props, Object instance) {
        this.parentView = parentView;
        this.type = type;
        this.properties = props;
        this.instance = instance;
        if (parentView != null)
           this.nestLevel = parentView.nestLevel + 1;
     }

     void userSelectedInstance(InstanceWrapper wrapper) {
        setSelectedInstance(wrapper.instance);
     }

     void setSelectedInstance(Object inst) {
        instance = inst;
        // When the user explicitly chooses <type> we need to mark that they've chosen nothing for this type to keep the current object from coming back
        if (instance == null)
           setDefaultCurrentObj(type, EditorContext.NO_CURRENT_OBJ_SENTINEL);
        else {
           setDefaultCurrentObj(type, instance);
        }

        for (int i = 0; i < childViews.size(); i++) {
           IElementView view = childViews.get(i);
           if (view instanceof ClassView) {
              ClassView cview = (ClassView) view;
              if (cview.type instanceof BodyTypeDeclaration) {
                 BodyTypeDeclaration btd = (BodyTypeDeclaration) cview.type;
                 if (btd.getDefinesCurrentObject()) {
                    Object childInst = instance == null ? null : DynUtil.getProperty(instance, btd.getTypeName());
                    cview.setSelectedInstance(childInst);
                 }
              }
           }
        }
     }


     abstract class LabeledView extends ElementView {
        object label extends JLabel {
           int prefW := ((int) preferredSize.width);
           int labelExtra := prefW >= tabSize ? 0 : (tabSize - prefW % tabSize);
           int labelWidth := prefW + labelExtra;
           location := SwingUtil.point(LabeledView.this.x+xpad+labelExtra, LabeledView.this.y + baseline);
           size := preferredSize;

           foreground := propertyInherited ? GlobalResources.transparentTextColor : GlobalResources.normalTextColor;

           text := propertyName + propertyOperator;

           icon := GlobalResources.lookupIcon(propC);

           toolTipText := "Property of type: " + ModelUtil.getPropertyType(propC) + (propC instanceof PropertyAssignment ? " set " : " defined ") + "in: " + ModelUtil.getEnclosingType(propC);
        }
        height := (int) label.size.height;
     }



     // view types: text field view {int, string}, enum, sub-object, list/array
     class TextFieldView extends LabeledView {
        // Want value set in the constructor both so it is not null and does not send an extra set of change events during initialization
        TextFieldView(Object prop) {
           propC = prop;
        }

        void displayFormError(String msg) {
           int ix = msg.indexOf(" - ");
           if (ix != -1)
              msg = msg.substring(ix + 3);
           errorLabel.text = msg;
        }

        //Color saveHighlightColor;
        void setElementValue(Object type, Object inst, Object val, String text) {
           // We misfire during init because this method is on an object which sends change events
           if (type == null || val == null)
              return;
           try {
              //if (saveHighlightColor != null)
              //   textField.highlight = saveHighlightColor;
              if (type instanceof ClientTypeDeclaration)
                 type = ((ClientTypeDeclaration) type).getOriginal();

              errorLabel.text = "";
              disableFormRebuild = true;
              String error = editorModel.setElementValue(type, inst, val, text, updateInstances, inst == null);
              // Refetch the member since we may have just defined one for this type
              propC = ModelUtil.definesMember(type, ModelUtil.getPropertyName(val), JavaSemanticNode.MemberType.PropertyAnySet, null, null);
              // Update this manually since we change it when moving the operator into the label
              propertyName = getPropertyNameString(propC);
              propertyOperator = propertyOperator(instance, propC);
              textField.enteredText = propertyValueString(instance, propC, changeCt);
              if (error != null)
                 displayFormError(error);
           }
           catch (RuntimeException exc) {
              //saveHighlightColor = textField.highlight;
              displayFormError(exc.getMessage());
           }
           finally {
              disableFormRebuild = false;
           }
        }

        String propertyValueString(Object instance, Object val, int changeCt) {
           return editorModel.ctx.propertyValueString(type, instance, val);
        }

        propertyName =: updateListeners();
        Object oldListenerInstance = null;
        JavaModel oldListenerModel = null;
        String oldPropName = null;

        void removeListeners() {
            removed = true;
            instance = null;
            updateListeners();

            DynUtil.dispose(this);
         }

        void updateListeners() {
           String propName = propertyName;
           String simpleProp;
           int ix = propName.indexOf("[");
           if (ix == -1)
              simpleProp = propName;
           else
              simpleProp = propName.substring(0, ix);
           JavaModel javaModel = instance == null ? ModelUtil.getJavaModel(ClassView.this.type) : null;
           if (oldListenerInstance == instance && propName == oldPropName && oldListenerModel == javaModel)
              return;

           if (oldPropName != null && !oldPropName.equals("<null>")) {
              if (oldListenerInstance != null) {
                 Bind.removeDynamicListener(oldListenerInstance, type, simpleProp, valueEventListener, IListener.VALUE_CHANGED);
                 oldListenerInstance = null;
              }
              else if (oldListenerModel != null) {
                 Bind.removeListener(oldListenerModel, null, valueEventListener, IListener.VALUE_CHANGED);
                 oldListenerModel = null;
              }
           }

           if (propName != null && !propName.equals("<null>")) {
              if (instance != null) {
                 Bind.addDynamicListener(instance, type, simpleProp, valueEventListener, IListener.VALUE_CHANGED);
                 oldListenerInstance = instance;
              }
              else if (javaModel != null) {
                 Bind.addListener(javaModel, null, valueEventListener, IListener.VALUE_CHANGED);
                 oldListenerModel = javaModel;
              }
           }
           oldPropName = propName;
        }


        object textField extends CompletionTextField {
           location := SwingUtil.point(label.location.x + label.size.width + xpad, TextFieldView.this.y);
           size := SwingUtil.dimension(TextFieldView.this.width - label.size.width - label.location.x - 2*xpad-borderSize, preferredSize.height);

           completionProvider {
              ctx := editorModel.ctx;
           }

           // If we have a fromString converter registered, we can edit this guy in value mode
           boolean settable := instance == null || RTypeUtil.canConvertTypeFromString(ModelUtil.getPropertyType(propC));
           foreground := settable ? ComponentStyle.defaultForeground : SwingUtil.averageColors(ComponentStyle.defaultForeground, ComponentStyle.defaultBackground);

           // instance could be retrieved through type hierarchy but we need to update the binding when the instance changes
           enteredText := propertyValueString(instance, propC, changeCt);

           // Only trigger the change to the model when the user enters the text.  Not when we set enteredText because the value changed
           userEnteredCount =: settable ? setElementValue(type, ClassView.this.instance, propC, enteredText) : errorLabel.text = "This view shows the toString output of property: " + propertyName + " No string conversion for type: " ;

           focus =: focusChanged(this, propC, ClassView.this.instance, focus);
        }
        object errorLabel extends JLabel {
          location := SwingUtil.point(textField.location.x, textField.location.y + textField.size.height + ypad);
          size := preferredSize;
          foreground := GlobalResources.errorTextColor;
        }
        height := (int) (errorLabel.size.height + textField.size.height + ypad);
     }

     void validateGroup(ExecutionContext ctx) {
        for (int i = getComponentCount()-1; i >= numStaticComponents; i--) {
           remove(i);
        }
        for (int i = childViews.size() - 1; i >= 0; i--) {
           childViews.get(i).removeListeners();
        }
        childViews.clear();
        IElementView prevView = null;
        IElementView newView;
        if (properties != null) {
           for (int i = 0; i < properties.length; i++) {
              Object prop = properties[i];
              if (prop != null) {
                 // Skipping non-sc properties for now
                 if (prop instanceof java.lang.reflect.Member)
                    continue;
                 if (editorModel.filteredProperty(type, prop, false))
                    continue;

                 if (prop instanceof BodyTypeDeclaration) {
                    prop = editorModel.processVisibleType(prop);
                    if (prop == null)
                       continue;
                    ClassView newGroup = newFormView(prop, ctx, type, this);
                    newView = newGroup;
                 }
                 else if (ModelUtil.isProperty(prop)) {
                    // If there is a current layer set and this object is not defined in that layer (or merged), skip it.
                    // If we get a def for a prop which is after the specified layer, see if there's a previous definition
                    // we should be using first.
                    if (editorModel.currentLayer != null) {
                       Layer propLayer = ModelUtil.getPropertyLayer(prop);

                       // If this property is not defined or modified in this layer or one which should be merged continue.
                       while (propLayer != null && !editorModel.currentLayerMatches(propLayer)) {
                          Object prevProp = ModelUtil.getPreviousDefinition(prop);
                          if (prevProp == null) {
                             prop = null;
                             break;
                          }
                          prop = prevProp;
                          propLayer = ModelUtil.getPropertyLayer(prop);
                          if (propLayer == null)
                             prop = null;
                       }
                       if (prop == null)
                          continue;
                    }
                    ElementView elemView = newPropertyView(prop, this);
                    newView = elemView;
                 }
                 else if (prop instanceof Class) {
                    // Skipping compiled classes now... could show them in a read-only view?
                    continue;
                 }
                 else {
                    System.err.println("*** unrecognized property type!");
                    continue;
                 }
                 SwingUtil.addChild(this, newView);
                 newView.prev = prevView;
                 prevView = newView;

                 if (newView instanceof ClassView)
                    ((ClassView) newView).validateGroup(ctx);

                 childViews.add(newView);
              }
           }
        }
        lastView = prevView;
     }

     ElementView newPropertyView(Object prop, ClassView parentView) {
        ElementView res = new TextFieldView(prop);
        return res;
     }

     void removeListeners() {
        for (IElementView view:childViews)
           view.removeListeners();
        childViews.clear();

        DynUtil.dispose(this);
     }

     void updateListeners() {
        for (IElementView view:childViews)
           view.updateListeners();
     }

     void updateChildren() {
        for (IElementView view:childViews) {
          if (view instanceof ClassView) {
             ClassView v = ((ClassView) view);
             if (instance == null)
                v.instance = null;
             else if (ModelUtil.isObjectProperty(v.type) && !ModelUtil.hasModifier(v.type, "static")) {
                v.instance = DynUtil.getPropertyPath(instance, CTypeUtil.getClassName(ModelUtil.getInnerTypeName(v.type)));
             }
          }
        }
     }

     instance := getDefaultCurrentObj(type);
     instance =: validateInstance();

     void init() {
        // Bindings not set before we set the instance property so need to do this once up front
        validateInstance();
     }

     void validateInstance() {
        if (removed)
           return;
        if (instanceList.selectedItem == null || ((InstanceWrapper)instanceList.selectedItem).instance != instance) {
           instanceList.selectedItem = new InstanceWrapper(editorModel.ctx, instance);
        }
        if (instance != oldInstance) {
           updateListeners();
           updateChildren();
           oldInstance = instance;
        }
    }

     startY := ypad + borderTop;
  }

   contentPanel {
      int maxChildWidth, maxChildHeight;
      preferredSize := SwingUtil.dimension(maxChildWidth, maxChildHeight);

      boolean formValid = true; // start out true so the first invalidate triggers the refresh
      void invalidateForm() {
         if (formValid && !disableFormRebuild) {
            // Remove the form when we invalidate it so we do not refresh the old form unnecessarily before rebuildForm's stuff runs
            removeForm();

            SwingUtilities.invokeLater(new Runnable() {
               public void run() {
                  rebuildForm();
                  }});

         }
      }

      List<ClassView> childViews = new ArrayList<ClassView>();

      void removeForm() {
         formValid = false;

         for (int i = getComponentCount()-1; i >= FORM_NUM_STATIC_COMPONENTS; i--) {
            remove(i);
         }
         for (int i = childViews.size() - 1; i >= 0; i--) {
            ClassView view = childViews.get(i);
            view.removeListeners();
         }
         childViews.clear();
      }

      void rebuildForm() {
         if (formValid)
            return;

         formValid = true;

         // remove stuff before we return when invisible.  Otherwise, our widgets still respond to events
         // even when this guy is not visible.
         if (!visible)
            return;

         ExecutionContext ctx = new ExecutionContext();

         int newMaxChildWidth = 0;
         int newMaxChildHeight = 0;

         if (editorModel != null && editorModel.visibleTypes != null) {

            ArrayList<Object> visibleTypes = editorModel.visibleTypes;
            ClassView prevView = null;

            numCols = visibleTypes.size() <= 1 ? 1 : visibleTypes.size() <= 4 ? 2 : visibleTypes.size() <= 9 ? 3 : 4;

            int numRows = (editorModel.types.size() + numCols-1) / numCols;
            ClassView[][] views = new ClassView[numRows][numCols];

            int typeIndex = 0;
            for (Object type:visibleTypes) {
               ClassView newView = newFormView(type, ctx, null, null);
               newView.row = typeIndex / numCols;
               newView.col = typeIndex % numCols;

               if (newView.row != 0)
                  newView.prev = views[newView.row-1][newView.col];
               prevView = newView;
               views[newView.row][newView.col] = newView;

               SwingUtil.addChild(this, newView);

               childViews.add(newView);

               newView.validateGroup(ctx);

               int newWidth = newView.x + newView.width + xpad;

               if (newMaxChildWidth < newWidth)
                  newMaxChildWidth = newWidth;

               int newHeight = newView.y + newView.height + ypad;
               if (newMaxChildHeight < newHeight)
                  newMaxChildHeight = newHeight;

               typeIndex++;
            }
         }

         for (int i = 0; i < childViews.size(); i++) {
            ClassView view = childViews.get(i);
            view.updateListeners();
         }

         this.maxChildWidth = newMaxChildWidth;
         this.maxChildHeight = newMaxChildHeight;
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

  class InstanceView extends ClassView {
     operatorName := "object";

     InstanceView(ClassView parent, Object type, Object[] props, Object instance) {
        super(parent, type, props, instance);
     }
  }

  class LayerView extends InstanceView {
     operatorName := "layer";

     LayerView(ClassView parent, Object type, Object[] props, Object instance) {
        super(parent, type, props, instance);
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
