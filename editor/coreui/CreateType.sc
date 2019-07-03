class CreateType extends CreateSubPanel {
   String innerChoice;
   String newTypeName;
   String extendsTypeName;
   String subPackage;

   boolean innerType := !StringUtil.equalStrings(innerChoice, "Top level");

   enabled := !StringUtil.isEmpty(newTypeName);

   newTypeSelected =: extendsTypeName;

   void createType() {
      CreateMode mode = createPanel.currentCreateMode;
      Object currentType = editorModel.currentType;
      String pkg = subPackage == null ? editorModel.currentPackage : CTypeUtil.prefixPath(editorModel.currentPackage, subPackage);
      String err = editorModel.createType(mode, newTypeName, currentType, extendsTypeName, pkg, editorModel.currentLayer);
      if (err != null) {
         displayNameError(err);
      }
      else
         createPanel.clearForm();
   }

   void doSubmit() {
      createType();
   }

   void clearFields() {
      extendsTypeName = "";
      newTypeName = "";
      subPackage = "";
   }

}