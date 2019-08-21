class RowEditor extends InstanceFieldEditor {
   boolean showIndex = true;
   boolean showId = true;

   List<Object> rowColumns;

   int rowHeight = 34;
   rowMode = true;

   boolean showHeader := listIndex == 0;

   RowEditor(FormView view, TypeEditor parentEditor, Object parentProperty, Object type, Object instance, int listIx, InstanceWrapper wrapper) {
      super(view, parentEditor, parentProperty, type, instance, listIx, wrapper);
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
         return ListCellEditor.class;
      System.err.println("*** Unrecognized editorType: " + editorType);
      return TextCellEditor.class;
   }

   void addComputedProperties(List<Object> props, Object[] allProps) {
      if (showIndex) {
         props.add(new ComputedProperty("#", Integer.class, "text", listIndex, 35, null));
      }
      if (showId) {
         props.add(new ComputedProperty("Id", editorModel.fetchInstanceType(instance), "ref", instance, 200, null));
      }
   }

   String getEditorType() {
      return "row";
   }

   int getExplicitWidth(String propName) {
      if (!(parentEditor instanceof ListGridEditor))
         return -1;
      Map<String,Integer> cellWidths = ((ListGridEditor) parentEditor).cellWidths;
      if (cellWidths == null)
         return -1;
      Integer cellWidth = cellWidths.get(propName);
      return cellWidth == null ? -1 : cellWidth;
   }

   int getExplicitHeight(String propName) {
      return rowHeight;
   }

   void cellWidthChanged(String propName) {
      for (IElementEditor childView:childViews) {
         childView.cellWidthChanged(propName);
      }
   }
}