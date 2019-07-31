class EditorFrame {
   EditorModel editorModel := editorPanel.editorModel;
   EditorPanel editorPanel;

   void doDeleteCurrentSelection() {
      if (editorModel.currentProperty != null) {
          editorModel.deleteCurrentProperty();
      }
      else if (editorModel.currentTypeIsLayer) {
         Layer toDelete = editorModel.currentLayer;
         List<Layer> depLayers = toDelete.getDependentLayers();
         if (depLayers.size() == 0) {
             editorModel.deleteCurrentLayer();
         }
         else
            showConfirmDeleteList(toDelete, depLayers);
      }
      else {
         editorModel.deleteCurrentType();
      }
   }

   void showConfirmDeleteList(Layer toDelete, List<Layer> deps) {
      Object[] options = {"Delete All", "Cancel"};
      int n = UIUtil.showOptionDialog(this,
          "Layer: " + toDelete.layerName + " extended by layers: " + deps,
          "Delete all layers?", 
          options,  //the titles of buttons
          options[0]); //default button title
       if (n == 0) {
          ArrayList<Layer> allLayers = new ArrayList<Layer>(deps.size() + 1);
          allLayers.add(toDelete);
          allLayers.addAll(deps);

          editorModel.removeLayers(allLayers);
          editorModel.clearCurrentType();
       }
   }

   void enableCreateMode(String type) {
      editorModel.createMode = true;
      editorPanel.statusPanel.createPanel.createModeName = type;
   }
}
