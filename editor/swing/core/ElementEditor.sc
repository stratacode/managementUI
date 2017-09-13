import sc.lang.java.JavaSemanticNode;

ElementEditor {
   // Pointer to either component which defines the 'field' or 'cell'
   @Constant JComponent formComponent;

   //Object propertyType := propC == null ? null : ModelUtil.getVariableTypeDeclaration(propC);
   String propertyTypeName := propType == null ? "<no type>" : String.valueOf(propType);

   propertySuffix := (ModelUtil.isArray(propType) || propType instanceof List ? "[]" : "");

   String propertyOperator := propertyOperator(formEditor.instance, propC);

   boolean propertyInherited := EditorModel.getLayerForMember(propC) != formEditor.classViewLayer;

   int tabSize;
   int xpad;
   int ypad;
   int baseline;

   class ElementLabel extends JLabel {
      int prefW := ((int) preferredSize.width);
      int labelExtra := prefW >= tabSize ? 0 : (tabSize - prefW % tabSize);
      int labelWidth := prefW + labelExtra;
      location := SwingUtil.point(ElementEditor.this.x+xpad+labelExtra, ElementEditor.this.y + baseline);
      size := preferredSize;

      foreground := propertyInherited ? GlobalResources.transparentTextColor : GlobalResources.normalTextColor;

      text := propertyName + propertySuffix + propertyOperator;

      icon := GlobalResources.lookupIcon(propC);

      toolTipText := "Property of type: " + ModelUtil.getPropertyType(propC) + (propC instanceof PropertyAssignment ? " set " : " defined ") + "in: " + ModelUtil.getEnclosingType(propC);
   }

   object errorLabel extends JLabel {
      override @sc.bind.NoBindWarn
      location := SwingUtil.point(formComponent.location.x, formComponent.location.y + formComponent.size.height + ypad);
      size := preferredSize;
      foreground := GlobalResources.errorTextColor;
   }

   propertyName =: updateListeners(true);
   JavaModel oldListenerModel = null;

   @Bindable
   int x := prevCell == null ? xpad : prevCell.x + prevCell.width,
       y := prev == null ? formEditor.startY : prev.y + prev.height,
       width := cellWidth, height := cellHeight;


   static Border createCellBorder() {
      return new javax.swing.border.CompoundBorder(BorderFactory.createLineBorder(Color.black), new javax.swing.border.EmptyBorder(8, 8, 8, 8));
   }

   void updateComputedValues() {
      super.updateComputedValues();

      tabSize = parentView.tabSize;
      xpad = parentView.xpad;
      ypad = parentView.ypad;
      baseline = parentView.baseline;
   }

   void focusChanged(JComponent component, boolean newFocus) {
      if (propC instanceof CustomProperty)
         return;
      formEditor.parentView.focusChanged(component, propC, formEditor.instance, newFocus);
   }

   String propertyOperator(Object instance, Object val) {
      return val == null ? "" : (instance != null ? "" : ModelUtil.getOperator(val) != null ? " " + ModelUtil.getOperator(val) : " =");
   }

   void displayFormError(String msg) {
      int ix = msg.indexOf(" - ");
      if (ix != -1) {
         msg = msg.substring(ix + 3);
         errorLabel.text = msg;
      }
   }

   String getTextFieldValue() {
      return "";
   }

   public boolean isVisible() {
      return false;
   }

   public void setVisible(boolean vis) {}

   void removeListeners() {
      updateListeners(false);

      DynUtil.dispose(this);
   }

   void updateListeners(boolean add) {
      if (propC instanceof CustomProperty)
         return;
      String propName = propertyName;
      String simpleProp;
      int ix = propName.indexOf("[");
      if (ix == -1)
         simpleProp = propName;
      else
         simpleProp = propName.substring(0, ix);
      JavaModel javaModel = formEditor.instance == null ? ModelUtil.getJavaModel(formEditor.type) : null;
      if (oldListenerInstance == formEditor.instance && propName == oldPropName && oldListenerModel == javaModel)
         return;

      if (oldPropName != null && !oldPropName.equals("<null>")) {
         if (oldListenerInstance != null) {
            Bind.removeDynamicListener(oldListenerInstance, formEditor.type, simpleProp, valueEventListener, IListener.VALUE_CHANGED);
            oldListenerInstance = null;
         }
         else if (oldListenerModel != null) {
            Bind.removeListener(oldListenerModel, null, valueEventListener, IListener.VALUE_CHANGED);
            oldListenerModel = null;
         }
      }

      if (add && propName != null && !propName.equals("<null>")) {
         if (formEditor.instance != null) {
            Bind.addDynamicListener(formEditor.instance, formEditor.type, simpleProp, valueEventListener, IListener.VALUE_CHANGED);
            oldListenerInstance = formEditor.instance;
         }
         else if (javaModel != null) {
                 Bind.addListener(javaModel, null, valueEventListener, IListener.VALUE_CHANGED);
                 oldListenerModel = javaModel;
         }
      }
      oldPropName = propName;
   }

   void setElementValue(Object type, Object inst, Object prop, String text) {
      if (type == null || prop == null)
         return;
      try {
         if (type instanceof ClientTypeDeclaration)
            type = ((ClientTypeDeclaration) type).getOriginal();

         errorLabel.text = "";
         //disableFormRebuild = true;
         boolean instMode = formEditor.parentView.instanceMode;
         String error = editorModel.setElementValue(type, inst, prop, text, !instMode, instMode, inst == null);
         // Refetch the member since we may have just defined one for this type
         propC = ModelUtil.definesMember(type, ModelUtil.getPropertyName(prop), JavaSemanticNode.MemberType.PropertyAnySet, null, null, null);
         // Update this manually since we change it when moving the operator into the label
         propertyName = getPropertyNameString(propC);
         propertyOperator = propertyOperator(formEditor.instance, propC);
         if (error != null)
            displayFormError(error);
      }
      catch (RuntimeException exc) {
         displayFormError(exc.getMessage());
      }
      finally {
      }
   }

   String propertyValueString(Object instance, Object prop, int changeCt) {
      if (prop instanceof CustomProperty)
         return ((CustomProperty) prop).value.toString();

      if (formEditor == null)
         System.err.println("*** Error - no formEditor for propValue");
      if (editorModel == null)
         System.err.println("*** Error - no editor model for propvalue");
      else if (editorModel.ctx == null)
              System.err.println("*** Error - No context for prop value");
      return editorModel.ctx.propertyValueString(formEditor.type, instance, prop);
   }
}
