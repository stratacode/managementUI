class RowEditor extends InstanceFieldEditor {
   sc.util.HashMap<Integer,Integer> setWidths = null;

   boolean showIndex = true;
   boolean showId = true;

   List<Object> rowColumns;

   int rowHeight = 34;
   rowMode = true;

   RowEditor(FormView view, TypeEditor parentEditor, Object parentProperty, Object type, Object instance, int listIx) {
      super(view, parentEditor, parentProperty, type, instance, listIx);
   }

   boolean getShowHeader() {
      // TODO: should we look for when the property set is different from the previous one and add a new header?  Or support a "headerRows" list of indexes?
      // Or a way to enable a header row every <n> elements
      return listIndex == 0;
   }

   Object getEditorClass(String editorType, String displayMode) {
      if (displayMode != null && displayMode.equals("header")) {
         return HeaderCellEditor.class;
      }

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
         return TextCellEditor.class;
      System.err.println("*** Unrecognized editorType: " + editorType);
      return TextCellEditor.class;
   }

   void addComputedProperties(List<Object> props) {
      if (showIndex) {
         props.add(new CustomProperty("#", Integer.class, "text", listIndex, 35, null));
      }
      if (showId) {
         props.add(new CustomProperty("Id", editorModel.fetchInstanceType(instance), "ref", instance, 200, null));
      }
   }

   String getEditorType() {
      return "row";
   }

   int getExplicitWidth(int colIx) {
      if (setWidths == null)
         return -1;
      Integer setWidth = setWidths.get(colIx);
      return setWidth == null ? -1 : setWidth;
   }

   int getExplicitHeight(int colIx) {
      return rowHeight;
   }

}