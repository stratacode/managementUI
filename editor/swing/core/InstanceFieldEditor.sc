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
   }

   childViews = new ArrayList<IElementEditor>();
}