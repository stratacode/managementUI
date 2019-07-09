class CreateProperty extends CreateSubPanel {
   String propertyName;
   String propertyTypeName;
   String ownerTypeName;
   String operator;
   String propertyValue;
   boolean addBefore;
   String relPropertyName;
   String beforeAfterText := relPropertyName == null ? "the " + (addBefore ? "first" : "last")  + " property" : relPropertyName;

   enabled := !TextUtil.isEmpty(propertyName) && !TextUtil.isEmpty(propertyTypeName) && !TextUtil.isEmpty(ownerTypeName);

   propertyTypeName =: displayPropertyError(editorModel.validateTypeText(propertyTypeName, false));

   newTypeSelected =: propertyTypeName;

   submitCount =: displayPropertyError(editorModel.createProperty(ownerTypeName, propertyTypeName, propertyName, operator, propertyValue, addBefore, relPropertyName));

   void init() {
      if (editorModel.currentType == null) {
         createPanel.displayCreateError("Select a type for the new instance");
      }
      else {
         ownerTypeName = editorModel.typeNames[0];
      }
   }

   void displayPropertyError(String err) {
      if (err != null)
         displayNameError(err);
      else
         clearForm();
   }
}