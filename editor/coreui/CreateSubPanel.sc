@CompilerSettings(propagateConstructor="sc.editor.CreatePanel,sc.editor.CreateMode")
abstract class CreateSubPanel {
   CreatePanel createPanel;
   CreateMode createMode;
   EditorModel editorModel;

   boolean enabled = true;
   boolean needsConfirmButtons = true;

   String newTypeSelected, newLayerSelected;

   double nameFieldRatio = 0.3;

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

   abstract void doSubmit();

   abstract void displayNameError(String error);

   abstract void requestFocus();
}
