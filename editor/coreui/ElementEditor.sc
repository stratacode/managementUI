import sc.lang.java.ModelUtil;
import sc.lang.java.VariableDefinition;

@CompilerSettings(propagateConstructor="sc.editor.FormView,sc.editor.FormEditor,Object,Object,Object")
abstract class ElementEditor extends PrimitiveEditor implements sc.obj.IStoppable {
   FormEditor formEditor;
   Object propC;
   String propertySuffix = "";
   String propertyName := getPropertyNameString(propC);
   EditorModel editorModel;

   IVariableInitializer varInit := propC instanceof IVariableInitializer ? (IVariableInitializer) propC : null;
   UIIcon icon := propC == null ? null : GlobalResources.lookupUIIcon(propC, ModelUtil.isDynamicType(formEditor.type));
   String errorText;
   String oldPropName;
   int changeCt = 0;
   Object oldListenerInstance = null;
   Object currentValue := getPropertyValue(propC, formEditor.instance, changeCt);
   String currentStringValue := currentValue == null ? "" : String.valueOf(currentValue); // WARNING: using currentValue.toString() does not work because data binding has a bug and won't listen for changes on 'currentValue' in that case
   Object propType;

   boolean instanceMode;
   boolean editable := !instanceMode || !editorModel.isConstantProperty(propC);

   visible := getPropVisible(formEditor.instance, varInit);

   ElementEditor(FormView parentView, FormEditor formEditor, Object prop, Object propType, Object propInst) {
      instanceMode = formEditor.instanceMode;
      this.formEditor = formEditor;
      editorModel = parentView.editorModel;
      this.propC = prop;
      if (instanceMode)
          this.currentValue = propInst;
      this.propType = propType;
   }

   public BaseView getParentView() {
      return formEditor == null ? null : formEditor.parentView;
   }

   String getPropertyNameString(Object prop) {
      return prop == null ? "<null>" : EditorModel.getPropertyName(prop);
   }

   String getOperatorDisplayStr(Object instance, IVariableInitializer varInit) {
      return instance == null && varInit != null ? (varInit.operatorStr == null ? " = " : varInit.operatorStr) : "";
   }

   void updateEditor(Object elem, Object prop, Object propType, Object inst) {
      propC = elem;
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
            return null;
      }
      else if (prop instanceof String)
         return DynUtil.getPropertyValue(instance, (String)prop);
      else if (prop instanceof sc.type.IBeanMapper)
         return DynUtil.getPropertyValue(instance, ((sc.type.IBeanMapper) prop).getPropertyName());
      return null;
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

   public void updateInstanceProperty() {
      if (formEditor.instance != null) {
         Object propType = ModelUtil.getPropertyType(propC);
         try {
            DynUtil.setPropertyValue(formEditor.instance, propertyName, elementValue);
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

   abstract Object getElementValue();

   void updateListeners(boolean add) {
      String simpleProp = getSimplePropName();
      String propName = getVariableName();

      if (oldListenerInstance == formEditor.instance && propName == oldPropName)
         return;

      removeListeners();

      if (add && propName != null && !propName.equals("<null>")) {
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

