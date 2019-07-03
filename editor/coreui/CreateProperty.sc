class CreateProperty extends CreateSubPanel {
   String propertyName;
   String propertyTypeName;
   String ownerTypeName;
   String operator;
   String propertyValue;
   boolean addBefore;
   String relPropertyName;

   enabled := !StringUtil.isEmpty(propertyName) && !StringUtil.isEmpty(propertyTypeName) && !StringUtil.isEmpty(ownerTypeName);

   newTypeSelected =: propertyTypeName;

   void init() {
      if (editorModel.currentType == null) {
         createPanel.displayCreateError("Select a type for the new instance");
      }
      else {
         ownerTypeName = editorModel.typeNames[0];
      }
   }

   void createProperty() {
      String name = propertyName.trim();
      if (StringUtil.isEmpty(ownerTypeName)) {
         displayNameError("Select a type to hold the new property");
         return;
      }
      if (StringUtil.isEmpty(propertyTypeName)) {
         displayNameError("Select a data type for the new property");
         return;
      }
      Object ownerType = editorModel.findType(ownerTypeName);
      if (ownerType == null) {
         displayNameError("No type: " + ownerTypeName);
         return;
      }
      String err = editorModel.ctx.addProperty(ownerType, propertyTypeName, name, operator, propertyValue, addBefore, relPropertyName);
      if (err != null) {
         displayNameError(err);
      }
      else
         clearForm();
   }

}