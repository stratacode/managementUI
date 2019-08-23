class ListGridEditor extends ListEditor {
   boolean showIndex = true;
   boolean showId = true;

   ListGridEditor(FormView view, TypeEditor parentEditor, Object parentProperty, Object type, List<Object> insts, int listIx, InstanceWrapper wrapper) {
      super(view, parentEditor, parentProperty, type, insts, listIx, wrapper);
   }

   void updateProperties() {
      if (componentType != null) {
         Object[] newProps = editorModel.getPropertiesForType(componentType);
         ArrayList<Object> visProps = new ArrayList<Object>();
         addComputedComponentProperties(visProps);
         filterProperties(componentType, visProps, newProps);
         properties = visProps.toArray();

         if (childViews != null) {
            for (IElementEditor childView:childViews) {
               if (childView instanceof RowEditor)
                  ((RowEditor)childView).updateProperties();
            }
         }
      }
   }

   abstract List<Object> getHeaderCellList();

// TODO: Synchronize this, cellWidths, sort properties
   boolean gridView = true;

   Map<String,Integer> cellWidths = new java.util.TreeMap<String,Integer>();

   gridView =: rebuildChildren(); // recreate the list in the new view mode

   Object getEditorClass(String editorType, String displayMode) {
      if (displayMode != null && displayMode.equals("header")) {
         return HeaderCellEditor.class;
      }

      if (gridView) {
         if (editorType.equals("ref") || editorType.equals("form"))
            return RowEditor.class;
      }
      return super.getEditorClass(editorType, displayMode);
   }

   void setCellWidth(String propName, int cellWidth) {
      cellWidths.put(propName, cellWidth);
      List<Object> headerViews = getHeaderCellList();
      if (headerViews != null) {
         for (Object hv:headerViews) {
            if (hv instanceof IElementEditor)
               ((IElementEditor) hv).cellWidthChanged(propName);
         }
      }
      for (IElementEditor childView:childViews) {
         childView.cellWidthChanged(propName);
      }
   }

   void addComputedComponentProperties(List<Object> props) {
      if (showIndex) {
         props.add(new ComputedProperty("#", Integer.class, "text", null, 35, null));
      }
      if (showId) {
         props.add(new ComputedProperty("Id", null, "ref", null, 200, null));
      }
   }

   int getExplicitWidth(String propName) {
      if (cellWidths == null)
         return -1;
      Integer cellWidth = cellWidths.get(propName);
      return cellWidth == null ? -1 : cellWidth;
   }
}
