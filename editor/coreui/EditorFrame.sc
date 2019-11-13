class EditorFrame {
   EditorModel editorModel := editorPanel.editorModel;
   EditorPanel editorPanel;

   /** Error text for when a delete or other operation fails and needs a modal */
   String opErrorText;

   void enableCreateMode(String type) {
      editorModel.createMode = true;
      editorPanel.statusPanel.createPanel.createModeName = type;
   }
}
