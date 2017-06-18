import sc.lang.java.ModelUtil;
import sc.lang.java.VariableDefinition;

abstract class ElementEditor extends PrimitiveEditor implements sc.obj.IStoppable {
   FormEditor formEditor;
   Object propC;
   String propertyName := getPropertyNameString(propC);
   String propertySuffix = "";
   EditorModel editorModel;

   IVariableInitializer varInit := propC instanceof IVariableInitializer ? (IVariableInitializer) propC : null;
   UIIcon icon := propC == null ? null : GlobalResources.lookupUIIcon(propC, ModelUtil.isDynamicType(formEditor.type));
   String errorText;
   String oldPropName;
   int changeCt = 0;
   Object oldListenerInstance = null;
   Object currentValue := getPropertyValue(propC, formEditor.instance, changeCt);
   String currentStringValue := currentValue == null ? "" : String.valueOf(currentValue); // WARNING: using currentValue.toString() does not work because data binding has a bug and won't listen for changes on 'currentValue' in that case

   visible := getPropVisible(formEditor.instance, varInit);

   ElementEditor(FormEditor formEditor, Object prop) {
      this.formEditor = formEditor;
      editorModel = parentView.editorModel;
      this.propC = prop;
   }

   public BaseView getParentView() {
      return formEditor == null ? null : formEditor.parentView;
   }

   String getPropertyNameString(Object prop) {
      return prop == null ? "<null>" : EditorModel.getPropertyName(prop) + propertySuffix;
   }

   String getOperatorDisplayStr(Object instance, IVariableInitializer varInit) {
      return instance == null && varInit != null ? (varInit.operatorStr == null ? " = " : varInit.operatorStr) : "";
   }


   boolean getPropVisible(Object instance, IVariableInitializer varInit) {
      // Hide reverse only bindings when displaying instance since they are not settable
      return varInit != null && (instance == null || !DynUtil.equalObjects(varInit.operatorStr, "=:"));
   }

   static boolean isIndexedProperty(Object prop) {
      return prop instanceof VariableDefinition && ((VariableDefinition) prop).indexedProperty;
   }

   object valueEventListener extends AbstractListener {
      public boolean valueValidated(Object obj, Object prop, Object eventDetail, boolean apply) {
         changeCt++; // Sends a change event so getPropertyStringValue is called again to update the changed value.
         return true;
      }
   }

   // Using these values as parameters so we get change events for them
   static Object getPropertyValue(Object prop, Object instance, int changeCt) {
      if (prop == null)
         return null;
      if (prop instanceof IVariableInitializer) {
         IVariableInitializer varInit = (IVariableInitializer) prop;

         if (instance == null)
            return varInit.initializerExprStr == null ? "" : varInit.initializerExprStr;
         else if (!isIndexedProperty(prop)) {
            Object val = DynUtil.getPropertyValue(instance, varInit.variableName);
            if (val == null)
               return null;
            return val;
         }
         else
            return "[]"; // TODO: maybe have a ListEditor for these?  
      }
      return "<unknown property type>";
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
            Object newVal = sc.type.Type.propertyStringToValue(ModelUtil.getPropertyType(propC), elementValue);
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


   abstract String getElementValue();

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
   public String getIconPath() {
      return icon == null ? "" : icon.path;
   }

   public String getIconAlt() {
      return icon == null ? "" : icon.desc;
   }
}
