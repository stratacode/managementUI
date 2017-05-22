ElementEditor {
   Object propertyType := propC == null ? null : ModelUtil.getVariableTypeDeclaration(propC);
   String propertyTypeName := propC == null ? "<no type>" : String.valueOf(propertyType);

   propertySuffix := (ModelUtil.isArray(propertyType) || propertyType instanceof List ? "[]" : "");

   String propertyOperator := propertyOperator(formEditor.instance, propC);

   boolean propertyInherited := propC != null && ModelUtil.getLayerForMember(null, propC) != formEditor.classViewLayer;

   int tabSize := parentView.tabSize;
   int xpad := parentView.xpad;
   int ypad := parentView.ypad;
   int baseline := parentView.baseline;

   String propertyOperator(Object instance, Object val) {
      return val == null ? "" : (instance != null ? "" : ModelUtil.getOperator(val) != null ? " " + ModelUtil.getOperator(val) : " =");
   }

   @Bindable
   int x := formEditor.columnWidth * col + xpad,
       y := prev == null ? ypad + formEditor.startY : prev.y + prev.height,
       width := formEditor.columnWidth - (parentView.nestWidth + 2*xpad) * formEditor.nestLevel, height;

   String getTextFieldValue() {
      return "";
   }

   public boolean isVisible() {
      return false;
   }

   public void setVisible(boolean vis) {}
}
