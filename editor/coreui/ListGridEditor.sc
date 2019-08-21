class ListGridEditor extends ListEditor {
   ListGridEditor(FormView view, TypeEditor parentEditor, Object parentProperty, Object type, List<Object> insts, int listIx, InstanceWrapper wrapper) {
      super(view, parentEditor, parentProperty, type, insts, listIx, wrapper);
   }

   boolean gridView = true;

   Map<String,Integer> cellWidths = new java.util.TreeMap<String,Integer>();

   gridView =: rebuildChildren(); // recreate the list in the new view mode

   Object getEditorClass(String editorType, String displayMode) {
      if (gridView) {
         if (editorType.equals("ref") || editorType.equals("form"))
            return RowEditor.class;
      }
      return super.getEditorClass(editorType, displayMode);
   }

   void setCellWidth(String propName, int cellWidth) {
      cellWidths.put(propName, cellWidth);
      for (IElementEditor childView:childViews) {
         childView.cellWidthChanged(propName);
      }
   }
}
