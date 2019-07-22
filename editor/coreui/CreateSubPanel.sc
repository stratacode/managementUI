//@CompilerSettings(propagateConstructor="sc.editor.CreatePanel,sc.editor.CreateMode")
@CompilerSettings(constructorProperties="createPanel,createMode")
abstract class CreateSubPanel {
   CreatePanel createPanel;
   CreateMode createMode;
   EditorModel editorModel;

   @Sync
   boolean enabled = true;
   boolean needsConfirmButtons = true;

   boolean row2Visible = true;

   @Sync
   String newTypeSelected, newLayerSelected;

   double nameFieldRatio = 0.3;

   //@Sync - don't synchronize since it will cause the method to be caused twice
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
