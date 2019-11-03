class CreateProperty extends CreateSubPanel {
   @Sync String propertyName, propertyTypeName, ownerTypeName, operator, propertyValue, relPropertyName;

   @Sync boolean addBefore;

   @Sync String beforeAfterText := relPropertyName == null ? "the " + (addBefore ? "first" : "last")  + " property" : relPropertyName;

   enabled := !TextUtil.isEmpty(propertyName) && !TextUtil.isEmpty(propertyTypeName) && !TextUtil.isEmpty(ownerTypeName);

   newTypeSelected =: propertyTypeName;

   submitCount =: handlePropertySubmitResult(editorModel.createProperty(ownerTypeName, propertyTypeName, propertyName, operator, propertyValue, addBefore, relPropertyName));

   propertyTypeName =: displayNameError(editorModel.validatePropertyTypeName(propertyTypeName));

   relPropertyName := editorModel.currentPropertyName;

   propertyName =: displayNameError(editorModel.validateNameText(propertyName));

   void init() {
      if (editorModel.currentType == null) {
         createPanel.displayCreateError("Select a type for the new instance");
      }
      else {
         ownerTypeName = CTypeUtil.getClassName(editorModel.typeNames[0]);
      }
   }

   void handlePropertySubmitResult(String err) {
      if (err != null && err.length() > 0)
         displayNameError(err);
      else
         clearForm();
   }

   void clearForm() {
      editorModel.createMode = false;
      clearFields();
   }
}