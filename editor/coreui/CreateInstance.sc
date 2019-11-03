class CreateInstance extends CreateSubPanel {
   @Sync
   String pendingCreateError := editorModel.pendingCreateError;
   pendingCreateError =: createPanel.displayCreateError(pendingCreateError);

   @Sync
   String propertyTypeName = "";

   // Comparing propertyTypeName to "" to avoid the remote method call in the client/server version to validateTypeText.
   // Also a good test of remote method calls in ? expressions
   propertyTypeName =: displayComponentError(propertyTypeName.equals("") ? "" : editorModel.validateTypeText(propertyTypeName, true));

   enabled := pendingCreateError == null;

   newTypeSelected =: propertyTypeName;

   submitCount =: createPanel.ensureViewType(ViewType.DataViewType);
   submitCount =: displayCreateError(editorModel.startOrCompleteCreate(propertyTypeName), editorModel.pendingCreate);

   row2Visible = false;

   int cancelCreateCount;

   cancelCreateCount =: editorModel.cancelCreate();

   void init() {
      if (editorModel.currentType == null) {
         createPanel.displayCreateError("Select a type for the new instance");
      }
      else {
         propertyTypeName = CTypeUtil.getClassName(editorModel.typeNames[0]);
      }
   }

   void displayCreateError(String err, boolean pendingCreate) {
      if (err == null || err.length() == 0) {
         if (pendingCreate) {
            createPanel.clearTextFields();
         }
         else {
            createPanel.clearForm();
         }
      }
      else
         displayComponentError(err);
   }

   void clearForm() {
      if (editorModel.pendingCreate) {
         // Run remote server method editorModel.cancelCreate() from a binding
         cancelCreateCount++;
      }
   }

   void clearFields() {
      propertyTypeName = "";
   }
}
