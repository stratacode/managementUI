import sc.sync.SyncManager;

class CreatePanel {
   EditorModel editorModel;

   // Incremented each time an operation finishes
   @Sync int opComplete = 0;

   @Sync String createModeName = CreateMode.Instance.toString();
   createModeName =: currentCreateMode = CreateMode.valueOf(createModeName);
   createModeName =: clearErrors();

   @Sync CreateMode currentCreateMode :=: editorModel.currentCreateMode;
   @Sync boolean createMode := editorModel.createMode;

   createMode =: onModeChange();
   currentCreateMode =: onModeChange();

   CreateSubPanel createSubPanel = null;

   @Sync String newLayerSelected =: createSubPanel.newLayerSelected;
   @Sync String newTypeSelected =: createSubPanel.newTypeSelected;

   @Sync String createLabelText := editorModel.pendingCreate ? (createSubPanel.enabled ? "Create instance" : "Provide required fields for instance") :  "Add";
   List<String> createModeNames := java.util.Arrays.asList(editorModel == null || editorModel.currentType == null ?
                                                            CreateMode.getNoCurrentTypeNames() : CreateMode.getAllNames());

   @Sync boolean addLayerMode := createSubPanel instanceof CreateLayer && ((CreateLayer) createSubPanel).addLayerMode;
   @Sync boolean createLayerMode := createSubPanel instanceof CreateLayer && !addLayerMode;

   @Sync String row1ErrorText = "", row2ErrorText = "";

   @Sync boolean confirmButtonsEnabled := createSubPanel.enabled && row2ErrorText.length() == 0 && row1ErrorText.length() == 0;

   void onModeChange() {
      if (createMode) {
         if (createSubPanel != null) {
            if (createSubPanel.createMode == currentCreateMode)
               return;
            removeSubPanel(createSubPanel);
         }

         if (currentCreateMode == null)
            System.out.println("***");
         // Need to queue sync events here because addSubPanel calls register and that needs to happen before we process
         // the addSyncInst call inside of the createSubPanel call. Otherwise, the remote side will try to serialize the create
         // of the type panel, rather than just synchronizing changes made to subcomponents.
         // TODO: should this be replaced by a new 'registered' flag in @Sync(registered)
         boolean flush = SyncManager.beginSyncQueue();
         try {
            createSubPanel = currentCreateMode.createSubPanel(this);
            addSubPanel(createSubPanel);
         }
         finally {
            if (flush) SyncManager.flushSyncQueue();
         }
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
      // TODO: should we have a SyncMode.Register so that this is done via an annotation?
      // It supports the ability to have sync'd objects as children of the panel but where the tree is
      // created automatically on both sides so we can just send property changes back and forth.
      sc.sync.SyncManager.registerSyncTree(createSubPanel);
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
