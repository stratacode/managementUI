ListEditor {
   int getCellWidth() {
      if (lastView == null)
         return super.getCellWidth();
      return lastView.x + lastView.width;
   }

   void validateEditorTree() {
      if (childList.refreshList())
         validateChildLists();
   }

   void refreshChildren() {
      if (childList.refreshList())
         validateChildLists();
   }

   void validateChildLists() {
      validateChildList(0, childList.repeatComponents, true);
      validateSize();
   }

   object childList extends ChildList {
      repeat := visList;
   }

   int borderTitleY = 0;

   childViews = new ArrayList<IElementEditor>();
}