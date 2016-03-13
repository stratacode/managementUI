class EditorFrame {
   EditorModel editorModel := editorPanel.editorModel;
   EditorPanel editorPanel;

   void doAddLayer() {
       editorModel.createMode = true;
   }

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

   void enableTypeCreateMode(String type) {
      if (editorModel.currentPackage != null) {
         editorModel.createMode = true;
         editorPanel.createTypeModeName = type;
      }
      else {
         UIUtil.showErrorDialog(this, "Select a layer or type to choose the destination package before clicking 'Add " + type + "'", "No package selected");
      }
   }

   void enablePropCreateMode() {
      if (editorModel.currentType != null) {
         editorModel.createMode = true;
         editorPanel.createTypeModeName = "Property";
      }
      else {
         UIUtil.showErrorDialog(this, "Select a type for the property before clicking 'Add'", "No type selected");
      }
   }

   boolean isCodeFunction(ArrayList<CodeFunction> codeFunctions, CodeFunction func) {
      return codeFunctions.size() == 1 && codeFunctions.contains(func);
   }

   void changeCodeFunction(CodeFunction func) {
      editorModel.changeCodeFunctions(EnumSet.of(func));
   }
}
