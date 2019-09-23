class EditFieldOp {
   String opStr;
   String valueStr;
   //int updateValueCount;
   int updateTypeCount;
   @Sync(syncMode=SyncMode.Disabled)
   EditorModel editorModel;
   String errorText;

   public void fieldValueSubmitted() {
      // TODO: when either the enter or checkbox is pressed this is called.  Should we do something
      // different in instance mode when the value is "=" - like prompt - update the instance or the default value
      // for the type?
      updateTypeCount++;
   }
}
