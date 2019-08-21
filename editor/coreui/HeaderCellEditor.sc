class HeaderCellEditor extends ElementEditor {
   cellMode = true;

   int sortDir;

   ListEditor listEditor;

   Object getElementValue() {
      return null;
   }

   void updateComputedValues() {
      super.updateComputedValues();

      // HeaderCellEditor's should be in a RowEditor inside of a ListEditor
      listEditor = (ListEditor) formEditor.formEditor;
      refreshSortDir();
   }

   void refreshSortDir() {
      sortDir = listEditor.getSortDir(propertyName);
   }

   void toggleSortDir(int dir) {
      int oldDir = sortDir;
      int newDir = oldDir == 0 || oldDir != dir ? dir : 0;
      this.sortDir = newDir;
      listEditor.updateSortDir(propertyName, newDir);
      Bind.sendChange(this, "sortDir", sortDir);
   }
}