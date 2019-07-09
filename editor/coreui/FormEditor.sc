class FormEditor extends InstanceFieldEditor {
   int instSelectedIndex = 0;

   instancesOfType := getInstancesOfType(type, true, "<type>", false);

   boolean showInstanceSelect := parentProperty == null && instanceMode && !editorModel.pendingCreate && type != null && !ModelUtil.isObjectType(type);

   String extendsTypeLabel := extTypeName == null ? "" : (parentProperty == null ? "extends" : "type");
   String extendsTypeName := extTypeName == null ? "" : CTypeUtil.getClassName(extTypeName);

   int refreshInstancesCt := editorModel.refreshInstancesCt;
   refreshInstancesCt =: editorModel.refreshInstancesCheck(this);
   
   instance =: operatorChanged();

   FormEditor(FormView view, TypeEditor parentEditor, Object parentProperty, Object type, Object instance, int listIx, InstanceWrapper wrapper) {
      super(view, parentEditor, parentProperty, type, instance, listIx, wrapper);
   }

   void gotoExtendsType() {
      editorModel.changeCurrentType(ModelUtil.findType(editorModel.system, extTypeName), null, null);
   }

   String getEditorType() {
      return "form";
   }

   void addComputedProperties(List<Object> props, Object[] allProps) {
      if (editorModel.pendingCreate && ModelUtil.sameTypes(editorModel.currentType, type)) {
         List<ConstructorProperty> cprops = editorModel.getConstructorProperties(wrapper, (TypeDeclaration)editorModel.currentType);
         if (cprops != null) {
            for (ConstructorProperty cprop:cprops) {
               boolean found = false;
               if (allProps != null) {
                  for (int i = 0; i < allProps.length; i++) {
                     String propName = ModelUtil.getPropertyName(allProps[i]);
                     if (TextUtil.equals(propName, cprop.name)) {
                        found = true;
                        break;
                     }
                  }
               }
               if (!found)
                  props.add(cprop);
            }
         }
      }
   }

   String getFixedOperatorName() {
      return editorModel.pendingCreate ? "new" : (instance != null ? "instance" : null);
   }
}
