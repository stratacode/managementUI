import sc.lang.EditorContext;
import sc.util.StringUtil;
import sc.lang.java.JavaModel;
import sc.layer.SrcEntry;

import javax.swing.text.DefaultCaret;

import javax.swing.ToolTipManager;

ModelEditor extends RTextScrollPane {
   viewportView = textArea;

   caretPosition :=: textArea.caretPosition;

   void modelChanged() {
      if (model == null || ctx == null)
         return;

      super.modelChanged();

      SrcEntry srcFile = model.getSrcFile();

      String pendingText = ctx.getMemoryEditSessionText(srcFile);
      String newText;
      if (pendingText != null) {

         String newModelText = model.toLanguageString();
         String origText = ctx.getMemoryEditSessionOrigText(srcFile);

         // Only if the model has changed from the original...
         if (!StringUtil.equalStrings(origText, newModelText)) {
            if (origText != null && !StringUtil.equalStrings(newModelText, pendingText)) {
               System.err.println("*** CONFLICT: " + model.getSrcFile() + " changed both in code view and on the file system - abandoning code view changes: " + pendingText);
               ctx.changeMemoryEditSession(newModelText, model, 0);
            }
         }
      }
      int saveCaretPos = textArea.caretPosition;
      textArea.text = ctx.getModelText(model);
      caretPosition = saveCaretPos;
   }

   object textArea extends RSyntaxTextArea {
      syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_JAVA;
      antiAliasingEnabled = true;
      font = UIManager.getFont("Label.font");

      object completionProvider extends SCCompletionProvider {
         ctx := ModelEditor.this.ctx;
         completionType = CompletionTypes.EntireFile;
         fileModel := model;
      }

      AutoCompletion autoCompletion = new AutoCompletion(completionProvider);
      {
         autoCompletion.install(textArea);

         //ToolTipManager.sharedInstance().registerComponent(textArea);
      }

      //toolTipSupplier = completionProvider;

      textChangedCount =: modelTextEdited();
   }

   void modelTextEdited() {
      ctx.changeMemoryEditSession(textArea.text, model, textArea.caretPosition);
   }

   void centerLineInView() {
      SwingUtil.centerLineInScrollPane(textArea);
   }
}
