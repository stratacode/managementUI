class CreatePanel {
   EditorModel editorModel;

   // Incremented each time an operation finishes
   int opComplete = 0;

   String createModeName = CreateMode.Instance.toString();
   createModeName =: currentCreateMode = CreateMode.valueOf(createModeName);
   createModeName =: clearErrors();

   CreateMode currentCreateMode :=: editorModel.currentCreateMode;
   boolean createMode := editorModel.createMode;

   createMode =: onModeChange();
   currentCreateMode =: onModeChange();

   CreateSubPanel createSubPanel = null;

   String newLayerSelected =: createSubPanel.newLayerSelected;
   String newTypeSelected =: createSubPanel.newTypeSelected;

   String createLabelText := editorModel.pendingCreate ? (createSubPanel.enabled ? "Create instance" : "Provide required fields for instance") :  "Add";
   List<String> createModeNames := java.util.Arrays.asList(editorModel == null || editorModel.currentType == null ?
                                                            CreateMode.getNoCurrentTypeNames() : CreateMode.getAllNames());

   boolean addLayerMode := createSubPanel instanceof CreateLayer && ((CreateLayer) createSubPanel).addLayerMode;
   boolean createLayerMode := createSubPanel instanceof CreateLayer && !addLayerMode;

   String row1ErrorText = "", row2ErrorText = "";

   boolean confirmButtonsEnabled := createSubPanel.enabled && row2ErrorText.length() == 0 && row1ErrorText.length() == 0;

   void onModeChange() {
      if (createMode) {
         if (createSubPanel != null) {
            if (createSubPanel.createMode == currentCreateMode)
               return;
            removeSubPanel(createSubPanel);
         }

         createSubPanel = currentCreateMode.createSubPanel(this);
         addSubPanel(createSubPanel);

         createSubPanel.init();
      }
      else if (createSubPanel != null) {
         removeSubPanel(createSubPanel);
         createSubPanel = null;
      }
      modeChangeComplete();
   }

   void removeSubPanel(CreateSubPanel panel) {
      DynUtil.dispose(createSubPanel);
   }

   void addSubPanel(CreateSubPanel panel) {
   }

   void modeChangeComplete() {}

   void displayCreateError(String err) {
      row1ErrorText = err;
   }

   void displayComponentError(String errorText, Object component) {
      row1ErrorText = errorText == null ? "" : errorText;
   }

   void displayNameError(String errorText, Object component) {
      row2ErrorText = errorText == null ? "" : errorText;
   }

   void clearErrors() {
      displayComponentError("", null);
      displayNameError("", null);
   }

   void clearTextFields() {
      if (createSubPanel == null) {
         System.err.println("***");
         return;
      }
      createSubPanel.clearFields();
   }

   void clearForm() {
      if (createSubPanel == null) {
         System.err.println("***");
         return;
      }
      clearTextFields();

      createSubPanel.clearForm();

      editorModel.createMode = false;

      displayComponentError("", null);
      row2ErrorText = "";

      opComplete++;
   }

}
