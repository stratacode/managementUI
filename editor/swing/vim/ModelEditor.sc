ModelEditor extends VIMPanel {
   fileNames := {file == null ? null : file.file.absFileName};

   void startEditor() {
      startCommand();
   }
}
