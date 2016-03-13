EditFieldOp {
   doEvent =: doEditField();

   void doEditField() {
      String error = editorModel.updateCurrentProperty(opStr, valueStr);
      if (error != null) {
         int ix = error.indexOf(" - "); // Strips off the File and other crap from a normal error
         if (ix != -1)
             error = error.substring(ix + 3);
         errorText = error;
      }
      else
         errorText = "";
   }

   void cancelValue() {
      editorModel.currentPropertyValue = editorModel.savedPropertyValue;
      editorModel.currentPropertyOperator = editorModel.savedPropertyOperator;
      errorText = "";
   }
}
