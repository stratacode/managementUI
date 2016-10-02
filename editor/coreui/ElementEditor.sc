class ElementEditor extends PrimitiveEditor {
   FormEditor formEditor;
   String propertyName := getPropertyNameString(propC);
   String propertySuffix = "";

   ElementEditor(FormEditor formEditor) {
      this.formEditor = formEditor;
   }

   public BaseView getParentView() {
      return formEditor.parentView;
   }

   String getPropertyNameString(Object val) {
      return val == null ? "<null>" : ModelUtil.getPropertyName(val) + propertySuffix;
   }

   Object propC;
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
         changeCt++; // A signal to call getPropertyStringValue again as the value has changed.
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

   /** Because this tag has component="true", it can override the component's stop method to remove the listener */
   void stop() {
      //propC = null;
      formEditor.instance = null;
      updateListeners(); // Dispose has already been called at this point
   }

   void updateListeners() {
      String propName = varInit == null ? null : varInit.variableName;
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

      if (oldListenerInstance == formEditor.instance && propName == oldPropName)
         return;

      if (oldPropName != null && !oldPropName.equals("<null>")) {
         if (oldListenerInstance != null) {
            Bind.removeDynamicListener(oldListenerInstance, formEditor.type, simpleProp, valueEventListener, IListener.VALUE_CHANGED);
            oldListenerInstance = null;
         }
      }

      if (propName != null && !propName.equals("<null>")) {
         if (formEditor.instance != null) {
            Bind.addDynamicListener(formEditor.instance, formEditor.type, simpleProp, valueEventListener, IListener.VALUE_CHANGED);
            oldListenerInstance = formEditor.instance;
         }
      }
      oldPropName = propName;
   }

   void removeListeners() {
      formEditor.instance = null;
      updateListeners();
   }
}
