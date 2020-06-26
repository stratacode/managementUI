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
   String outerTypeName;

   @Sync
   List<String> innerChoiceItems := java.util.Arrays.asList(editorModel == null || editorModel.currentType == null || ModelUtil.isLayerType(editorModel.currentType) ? new String[] {"Top level"} : new String[] {"Top level", "Inside"});
   @Sync
   String beforeAfterText := editorModel.currentProperty == null ? "the last property" : editorModel.getPropertyName(editorModel.currentProperty);

   @Sync
   boolean innerType := !TextUtil.equals(innerChoice, "Top level");

   List<String> matchingLayerNames := editorModel.getMatchingLayerNamesForType(!innerType ? null : outerTypeName);
   int currentLayerIndex := matchingLayerNames.indexOf(editorModel.currentLayer.layerName);

   enabled := !TextUtil.isEmpty(newTypeName);

   newTypeSelected =: extendsTypeName;

   submitCount =: displayTypeError(
                    editorModel.createType(createPanel.currentCreateMode, newTypeName, outerTypeName,
                                           extendsTypeName, createPackageName, editorModel.currentLayer));

   newTypeName =: displayNameError(editorModel.validateNameText(newTypeName));
   extendsTypeName =: displayNameError(editorModel.validateTypeText(extendsTypeName, false));

   String getCreatePackageName() {
      return subPackage == null ? editorModel.currentPackage : CTypeUtil.prefixPath(editorModel.currentPackage, subPackage);
   }

   void setMatchingLayerNames(List<String> lns) {
      this.matchingLayerNames = lns;
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

   void init() {
      super.init();

   }
}