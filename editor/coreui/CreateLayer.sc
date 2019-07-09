class CreateLayer extends CreateSubPanel {
   String layerMode;
   String newLayerName;
   String newLayerPackage;
   String newLayerExtends;

   boolean isPublic, isDynamic, isTransparent;

   boolean addLayerMode := TextUtil.equals(layerMode, "Include");

   newTypeSelected =: newLayerExtends;
   newLayerSelected =: addLayerMode ? newLayerName : newLayerExtends;

   enabled := TextUtil.length(newLayerName) != 0;

   submitCount =: displayLayerError(addLayerMode ?
              editorModel.addLayer(newLayerName, isDynamic) :
              editorModel.createLayer(newLayerName, newLayerPackage, newLayerExtends, isPublic, isDynamic, isTransparent));

   void displayLayerError(String err) {
      if (err == null)
         clearForm();
      else
         displayNameError(err);
   }

   void clearFields() {
      newLayerExtends = "";
      newLayerPackage = "";
      newLayerName = "";
   }

}