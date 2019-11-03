class TextFieldEditor extends ElementEditor {
   abstract String getElementStringValue();

   String errorStringValue; // The value at the time the error was generated so we can clear it

   errorText =: errorStringValue = elementStringValue;

   public Object getElementValue() {
       if (instanceMode) {
          try {
             return sc.type.Type.propertyStringToValue(ModelUtil.getPropertyType(propC), elementStringValue);
          }
          catch (IllegalArgumentException exc) {
             return elementStringValue;
          }
       }
       else
          return elementStringValue;
    }
}
