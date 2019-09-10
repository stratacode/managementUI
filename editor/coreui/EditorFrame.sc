class EditorFrame {
   EditorModel editorModel := editorPanel.editorModel;
   EditorPanel editorPanel;

   void doDeleteCurrentSelection() {
      if (editorModel.currentProperty != null) {
          editorModel.deleteCurrentProperty();
      }
      else if (editorModel.currentTypeIsLayer) {
         Layer toDelete = editorModel.currentLayer;
         List<String> usedByLayerNames = toDelete.getUsedByLayerNames();
         if (usedByLayerNames.size() == 0) {
             editorModel.deleteCurrentLayer();
         }
         else
            showConfirmDeleteList(toDelete, usedByLayerNames);
      }
      else {
         editorModel.deleteCurrentType();
      }
   }

   void showConfirmDeleteList(Layer toDelete, List<String> usedByLayerNames) {
      Object[] options = {"Delete All", "Cancel"};
      int n = UIUtil.showOptionDialog(this,
          "Layer: " + toDelete.layerName + " extended by layers: " + usedByLayerNames,
          "Delete all layers?", 
          options,  //the titles of buttons
          options[0]); //default button title
       if (n == 0) {
          ArrayList<String> allLayerNames = new ArrayList<String>(usedByLayerNames.size() + 1);
          allLayerNames.add(toDelete.getLayerName());

          editorModel.removeLayers(allLayerNames);
          editorModel.clearCurrentType();
       }
   }

   void enableCreateMode(String type) {
      editorModel.createMode = true;
      editorPanel.statusPanel.createPanel.createModeName = type;
   }
}
