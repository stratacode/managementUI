class TextFieldEditor extends ElementEditor {
    abstract String getElementStringValue();

    public Object getElementValue() {
       if (instanceMode)
          return sc.type.Type.propertyStringToValue(ModelUtil.getPropertyType(propC), elementStringValue);
       else
          return elementStringValue;
    }
}
