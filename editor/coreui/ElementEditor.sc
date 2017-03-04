import sc.lang.java.ModelUtil;

class ElementEditor extends PrimitiveEditor implements sc.obj.IStoppable {
   FormEditor formEditor;
   Object propC;
   String propertyName := getPropertyNameString(propC);
   String propertySuffix = "";
   EditorModel editorModel;

   ElementEditor(FormEditor formEditor, Object prop) {
      this.formEditor = formEditor;
      this.propC = prop;
      editorModel = parentView.editorModel;
   }

   public BaseView getParentView() {
      return formEditor == null ? null : formEditor.parentView;
   }

   String getPropertyNameString(Object val) {
      return val == null ? "<null>" : ModelUtil.getPropertyName(val) + propertySuffix;
   }

   IVariableInitializer varInit := propC instanceof IVariableInitializer ? (IVariableInitializer) propC : null;
   UIIcon icon := propC == null ? null : GlobalResources.lookupUIIcon(propC, ModelUtil.isDynamicType(formEditor.type));
   String errorText;
   String oldPropName;
   int changeCt = 0;
   Object oldListenerInstance = null;
   public String currentValue := getPropertyStringValue(propC, formEditor.instance, changeCt);

   String getOperatorDisplayStr(Object instance, IVariableInitializer varInit) {
      return instance == null && varInit != null ? (varInit.operatorStr == null ? " = " : varInit.operatorStr) : "";
   }

   boolean getPropVisible(Object instance, IVariableInitializer varInit) {
      // Hide reverse only bindings when displaying instance since they are not settable
      return varInit != null && (instance == null || !DynUtil.equalObjects(varInit.operatorStr, "=:"));
   }

   object valueEventListener extends AbstractListener {
      public boolean valueValidated(Object obj, Object prop, Object eventDetail, boolean apply) {
         changeCt++; // Sends a change event so getPropertyStringValue is called again to update the changed value.
         return true;
      }
   }

   // Using these values as parameters so we get change events for them
   String getPropertyStringValue(Object prop, Object instance, int changeCt) {
      if (prop == null)
         return "";
      if (prop instanceof IVariableInitializer) {
         IVariableInitializer varInit = (IVariableInitializer) prop;

         if (instance == null)
            return varInit.initializerExprStr == null ? "" : varInit.initializerExprStr;
         else {
            Object val = DynUtil.getPropertyValue(instance, varInit.variableName);
            if (val == null)
               return "";
            return val.toString();
         }
      }
      return ModelUtil.getPropertyName(prop);
   }

   /** Part of the IStoppable implementation */
   void stop() {
      removeListeners(); // Dispose has already been called at this point
      visible = false;
   }

   private String getVariableName() {
      return varInit == null ? null : varInit.variableName;
   }

   private String getSimplePropName() {
      String propName = getVariableName();
      String simpleProp;
      if (propName != null) {
         int ix = propName.indexOf("[");
         if (ix == -1)
            simpleProp = propName;
         else
            simpleProp = propName.substring(0, ix);
      }
      else
         simpleProp = propName;

      return simpleProp;
   }

   boolean getInstanceMode() {
      return formEditor.parentFormView.instanceMode;
   }

   public void updateInstanceProperty() {
      if (formEditor.instance != null) {
         Object propType = ModelUtil.getPropertyType(propC);
         try {
            Object newVal = sc.type.Type.propertyStringToValue(ModelUtil.getPropertyType(propC), textFieldValue);
            DynUtil.setPropertyValue(formEditor.instance, propertyName, newVal);
         }
         catch (IllegalArgumentException exc) {
            errorText = exc.toString();
         }
         catch (UnsupportedOperationException exc1) {
            errorText = exc1.toString();
         }
      }
      else
         errorText = "No instance to update";
   }


   abstract String getTextFieldValue();

   void updateListeners() {
      String simpleProp = getSimplePropName();
      String propName = getVariableName();

      if (oldListenerInstance == formEditor.instance && propName == oldPropName)
         return;

      removeListeners();

      if (propName != null && !propName.equals("<null>")) {
         if (formEditor.instance != null) {
            Bind.addDynamicListener(formEditor.instance, formEditor.type, simpleProp, valueEventListener, IListener.VALUE_CHANGED);
            oldListenerInstance = formEditor.instance;
         }
      }
      oldPropName = propName;
   }

   void removeListeners() {
      if (oldPropName != null && !oldPropName.equals("<null>")) {
         if (oldListenerInstance != null) {
            Bind.removeDynamicListener(oldListenerInstance, formEditor.type, getSimplePropName(), valueEventListener, IListener.VALUE_CHANGED);
            oldListenerInstance = null;
         }
      }
   }
}
