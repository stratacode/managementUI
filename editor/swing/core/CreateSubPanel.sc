CreateSubPanel extends ComponentGroup implements EditorPanelStyle {
   JComponent followComponent = createPanel.createTypeChoice;
   JComponent lastComponent;
   int row2y;

   void validateName(String text) {
      String err = editorModel.validateNameText(text);
      if (err == null) {
         displayNameError("");
         return;
      }
      else
         displayNameError(err);
   }

}