class TextCellEditor extends ElementEditor {
   cellMode = true;

   abstract String getElementStringValue();

   public Object getElementValue() {
      return sc.type.Type.propertyStringToValue(ModelUtil.getPropertyType(propC), elementStringValue);
   }
}