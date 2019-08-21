// Used to display a list of values inside of a single cell - organized as one list in a row
class ListCellEditor extends ListEditor {
   cellMode = true;

   ListCellEditor(FormView view, TypeEditor parentEditor, Object parentProperty, Object type, List<Object> insts, int listIx, InstanceWrapper wrapper) {
      super(view, parentEditor, parentProperty, type, insts, listIx, wrapper);
   }

   Object getEditorClass(String editorType, String displayMode) {
      if (editorType.equals("text"))
         return TextCellEditor.class;
      else if (editorType.equals("textArea"))
         return TextCellEditor.class;
      else if (editorType.equals("ref"))
         return ReferenceCellEditor.class;
      else if (editorType.equals("toggle"))
         return ToggleCellEditor.class;
      else if (editorType.equals("choice"))
         return ChoiceCellEditor.class;
      else if (editorType.equals("form"))
         return TextCellEditor.class;
      else if (editorType.equals("list"))
         return ListCellEditor.class;
      System.err.println("*** Unrecognized editorType: " + editorType);
      return TextCellEditor.class;
   }

   int getDefaultCellWidth(String editorType, Object prop, Object propType) {
      return 120;
   }
}
