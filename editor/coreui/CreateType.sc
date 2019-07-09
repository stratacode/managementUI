class CreateType extends CreateSubPanel {
   String innerChoice;
   String newTypeName;
   String extendsTypeName;
   String subPackage;

   List<String> innerChoiceItems := java.util.Arrays.asList(editorModel == null || editorModel.currentType == null || ModelUtil.isLayerType(editorModel.currentType) ? new String[] {"Top level"} : new String[] {"Inside", "Top level"});
   String beforeAfterText := editorModel.currentProperty == null ? "the last property" : ModelUtil.getPropertyName(editorModel.currentProperty);

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