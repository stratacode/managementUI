class CreateLayer extends CreateSubPanel {
   String layerMode;
   String newLayerName;
   String newLayerPackage;
   String newLayerExtends;

   boolean isPublic, isDynamic, isTransparent;

   boolean addLayerMode := StringUtil.equalStrings(layerMode, "Include");

   newTypeSelected =: newLayerExtends;
   newLayerSelected =: addLayerMode ? newLayerName : newLayerExtends;

   enabled := !StringUtil.isEmpty(newLayerName);

   void doSubmit() {
      if (addLayerMode)
         addLayer();
      else
         createLayer();
   }

   void addLayer() {
      String err = editorModel.addLayer(newLayerName, isDynamic);
      if (err == null)
         clearForm();
      else
         displayNameError(err);
   }

   void createLayer() {
      String err = editorModel.createLayer(newLayerName, newLayerPackage, newLayerExtends, isPublic, isDynamic, isTransparent);
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