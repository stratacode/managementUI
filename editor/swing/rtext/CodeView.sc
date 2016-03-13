CodeView {
   CodeEditor {
      object confirmButtons extends ConfirmButtons {
         enabled := editorModel.ctx.hasAnyMemoryEditSession(editorModel.ctx.memorySessionChanged); // Need modelChanged param to fire hasAnyMemoryEditSessions change event
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
