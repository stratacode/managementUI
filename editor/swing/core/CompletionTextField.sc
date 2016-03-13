class CompletionTextField extends JTextField {
   // Auto completion support
   object completionProvider extends SCCompletionProvider {
   }

   AutoCompletion autoCompletion = new AutoCompletion(completionProvider);
   {
      autoCompletion.install(this);
   }

}
