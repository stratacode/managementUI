InstanceFieldEditor {
   void validateEditorTree() {
      if (childList.refreshList())
         validateChildLists();
      else {
         validateSize();
      }
   }

   void refreshChildren() {
      if (childList.refreshList())
         validateChildLists();
   }

   object childList extends ChildList {
      repeat := properties;
      listStart = InstanceFieldEditor.this.listStart;
   }

   void validateChildLists() {
      validateChildList(0, childList.repeatComponents, true);
      validateSize();
      // If the root element changes in size, the parent view needs to update it's size
      if (formEditor == null)
         parentView.scheduleValidateSize();
   }

   childViews = new ArrayList<IElementEditor>();
}