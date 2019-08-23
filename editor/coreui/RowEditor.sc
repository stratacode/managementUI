class RowEditor extends InstanceFieldEditor {
   List<Object> rowColumns;

   int rowHeight = 34;
   rowMode = true;

   boolean showHeader := listIndex == 0;

   ListGridEditor listEditor;

   RowEditor(FormView view, TypeEditor parentEditor, Object parentProperty, Object type, Object instance, int listIx, InstanceWrapper wrapper) {
      super(view, parentEditor, parentProperty, type, instance, listIx, wrapper);
      listEditor = (ListGridEditor) parentEditor;
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

   String getEditorType() {
      return "row";
   }

   int getExplicitWidth(String propName) {
      return parentEditor.getExplicitWidth(propName);
   }

   int getExplicitHeight(String propName) {
      return rowHeight;
   }

   void cellWidthChanged(String propName) {
      for (IElementEditor childView:childViews) {
         childView.cellWidthChanged(propName);
      }
   }

   // Allow rows should show the same list derived from the parent grid.
   void updateProperties() {
      if (parentList != null) {
         ArrayList props = new ArrayList();
         props.addAll(java.util.Arrays.asList(parentList.properties));
         properties = props.toArray();
         for (int i = 0; i < properties.length; i++) {
            if (properties[i] instanceof ComputedProperty) {
               ComputedProperty prop = (ComputedProperty) properties[i];
               properties[i] = prop = prop.clone();
               if (prop.name.equals("#"))
                  prop.value = listIndex;
               else if (prop.name.equals("Id")) {
                  prop.propertyType = editorModel.fetchInstanceType(instance);
                  prop.value = instance;
               }
            }
         }
      }
   }

}