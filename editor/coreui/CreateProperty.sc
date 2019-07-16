class CreateProperty extends CreateSubPanel {
   @Sync String propertyName, propertyTypeName, ownerTypeName, operator, propertyValue, relPropertyName;

   @Sync boolean addBefore;

   @Sync String beforeAfterText := relPropertyName == null ? "the " + (addBefore ? "first" : "last")  + " property" : relPropertyName;

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