class TextFieldEditor extends ElementEditor {
   // Want value set in the constructor both so it is not null and does not send an extra set of change events during initialization
   TextFieldEditor(FormEditor editor, Object prop) {
      super(editor, prop);
   }
}