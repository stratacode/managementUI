class HeaderCellEditor extends ElementEditor {
   cellMode = true;
   headerCell = true;

   int sortDir;

   ListGridEditor listEditor;

   Object getElementValue() {
      return null;
   }

   void updateComputedValues() {
      super.updateComputedValues();

      // HeaderCellEditor's should be in inside of a ListGridEditor
      listEditor = (ListGridEditor) formEditor;
      refreshSortDir();
   }

   void refreshSortDir() {
      sortDir = listEditor.getSortDir(propertyName);
   }

   void toggleSortDir(int dir, boolean append) {
      int oldDir = sortDir;
      int newDir = oldDir == 0 || oldDir != dir ? dir : 0;
      this.sortDir = newDir;
      listEditor.updateSortDir(propertyName, newDir, append);
      Bind.sendChange(this, "sortDir", sortDir);
   }

   // Do not try to update listeners for the header cells since we do not display the value anyway and the properties on the ListGrid are properties of the list items, not the list itself
   void updateListeners(boolean add) {
   }
}