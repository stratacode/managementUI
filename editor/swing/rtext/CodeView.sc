CodeView {
   CodeEditor {
      object confirmButtons extends ConfirmButtons {
         enabled := editorHasChanges;
         cancelEnabled := enabled;
         x := (int) (CodeEditor.this.size.width - width);
         y = 0;
         cancelButton {
            clickCount =: editorModel.ctx.cancelMemorySessionChanges();
         }
         okButton {
            clickCount =: editorModel.commitMemorySessionChanges();
         }
      }
   }
}
