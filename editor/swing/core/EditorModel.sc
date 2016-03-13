EditorModel {
   void invalidateModel() {
      if (modelsValid) {
         modelsValid = false;

         SwingUtilities.invokeLater(new Runnable() {
            public void run() {
               rebuildModel();
               }});

      }
   }

}
