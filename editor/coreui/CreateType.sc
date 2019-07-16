class CreateType extends CreateSubPanel {
   @Sync
   String innerChoice;
   @Sync
   String newTypeName;
   @Sync
   String extendsTypeName;
   @Sync
   String subPackage;

   @Sync
   List<String> innerChoiceItems := java.util.Arrays.asList(editorModel == null || editorModel.currentType == null || ModelUtil.isLayerType(editorModel.currentType) ? new String[] {"Top level"} : new String[] {"Inside", "Top level"});
   @Sync
   String beforeAfterText := editorModel.currentProperty == null ? "the last property" : ModelUtil.getPropertyName(editorModel.currentProperty);

   @Sync
   boolean innerType := !TextUtil.equals(innerChoice, "Top level");

   enabled := !TextUtil.isEmpty(newTypeName);

   newTypeSelected =: extendsTypeName;

   submitCount =: displayTypeError(
                    editorModel.createType(createPanel.currentCreateMode, newTypeName, editorModel.currentType,
                                           extendsTypeName, createPackageName, editorModel.currentLayer));

   String getCreatePackageName() {
      return subPackage == null ? editorModel.currentPackage : CTypeUtil.prefixPath(editorModel.currentPackage, subPackage);
   }

   void displayTypeError(String err) {
      if (err != null)
         displayNameError(err);
      else
         createPanel.clearForm();
   }

   void clearFields() {
      extendsTypeName = "";
      newTypeName = "";
      subPackage = "";
   }

}