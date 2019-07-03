class CreateInstance extends CreateSubPanel {
   String pendingCreateError := editorModel.pendingCreateError;
   pendingCreateError =: createPanel.displayCreateError(pendingCreateError);

   String propertyTypeName;

   enabled := pendingCreateError == null;

   newTypeSelected =: propertyTypeName;

   void init() {
      if (editorModel.currentType == null) {
         createPanel.displayCreateError("Select a type for the new instance");
      }
      else {
         propertyTypeName = CTypeUtil.getClassName(editorModel.typeNames[0]);
      }
   }

   void createInstance() {
      if (!editorModel.pendingCreate) {
         String err = editorModel.createInstance(propertyTypeName);
         if (err instanceof String) {
            displayNameError(err);
         }
         else {
            displayNameError(err);
            createPanel.displayCreateError(pendingCreateError);
            // Reset the fields for the next time but don't reset the mode
            if (pendingCreateError == null)
               createPanel.clearTextFields();
         }
      }
      else {
         String err = editorModel.completeCreateInstance(true);
         if (err != null) {
            displayNameError(err);
         }
         else {
            createPanel.clearForm();
            editorModel.createMode = false; // Set this back to non-create state
            editorModel.invalidateModel(); // Rebuild it so it says instance instead of new
         }
      }
   }

   void clearForm() {
      if (editorModel.pendingCreate) {
          editorModel.cancelCreate();
      }
   }

   void doSubmit() {
      createInstance();
   }

   void clearFields() {
      propertyTypeName = "";
   }
}
