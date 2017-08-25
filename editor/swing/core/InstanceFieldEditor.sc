InstanceFieldEditor {

   void validateTree() {
      childList.refreshList();
   }

   void refreshChildren() {
      childList.refreshList();
   }

   object childList extends ChildList {
      repeat := properties;
   }

   childViews = new ArrayList<IElementEditor>();
}