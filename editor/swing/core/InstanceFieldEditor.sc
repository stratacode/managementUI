InstanceFieldEditor {

   void validateEditorTree() {
      if (childList.refreshList())
         validateChildLists();
   }

   void refreshChildren() {
      if (childList.refreshList())
         validateChildLists();
   }

   object childList extends ChildList {
      repeat := properties;
   }

   void validateChildLists() {
      validateChildList(0, childList.repeatComponents, true);
      height = cellHeight;
   }

   childViews = new ArrayList<IElementEditor>();
}