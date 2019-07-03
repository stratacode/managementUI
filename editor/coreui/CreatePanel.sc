class CreatePanel {
   EditorModel editorModel;

   // Incremented each time an operation finishes
   int opComplete = 0;

   CreateMode currentCreateMode :=: editorModel.currentCreateMode;
   boolean createMode := editorModel.createMode;

   createMode =: onModeChange();
   currentCreateMode =: onModeChange();

   CreateSubPanel createSubPanel = null;

   String newLayerSelected =: createSubPanel.newLayerSelected;
   String newTypeSelected =: createSubPanel.newTypeSelected;

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

   abstract void displayCreateError(String err);

   abstract void clearTextFields();

   abstract void clearErrors();

   abstract void displayComponentError(String errorText, Object component);
}
