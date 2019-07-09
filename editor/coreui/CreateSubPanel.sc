@CompilerSettings(propagateConstructor="sc.editor.CreatePanel,sc.editor.CreateMode")
abstract class CreateSubPanel {
   CreatePanel createPanel;
   CreateMode createMode;
   EditorModel editorModel;
   
   boolean enabled = true;
   boolean needsConfirmButtons = true;

   boolean row2Visible = true;

   String newTypeSelected, newLayerSelected;

   double nameFieldRatio = 0.3;

   int submitCount;

   CreateSubPanel(CreatePanel panel,CreateMode mode) {
      this.createPanel = panel;
      this.createMode = mode;
      this.editorModel = panel.editorModel;
   }

   void init() {
   }

   void clearFields() {
   }

   void clearForm() {
   }

   void displayComponentError(String error) {
      createPanel.displayComponentError(error, null);
   }

   void displayNameError(String error) {
      createPanel.displayNameError(error, null);
   }

   abstract void requestFocus();

   void doSubmit() {
      submitCount++;
   }
}
