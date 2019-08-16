RowEditor {
   int headerHeight = 25;
   numCols := DynUtil.getArrayLength(properties);
   borderTop := showHeader ? headerHeight : 0;
   object headerList extends ChildList {
      displayMode = "header";
      repeat := properties;
      visible := showHeader;
   }

   void validateEditorTree() {
      boolean needsRefresh = headerList.refreshList();
      if (childList.refreshList())
         needsRefresh = true;
      if (needsRefresh) {
         validateChildLists();
      }
      /*
      for (int i = 0; i < childViews.size(); i++) {
         IElementEditor childView = childViews.get(i);
         if (childView instanceof JComponent)
            ((JComponent) childView).validate();
      }
      */
   }

   void refreshChildren() {
      headerList.refreshList();
      super.refreshChildren();
   }

   void validateChildLists() {
      validateChildList(0, headerList.repeatComponents, false);
      validateChildList(headerList.repeatComponents.size(), childList.repeatComponents, true);
   }

   int getCellHeight() {
      return (showHeader ? headerHeight : 0) + rowHeight;
   }

   int getCellWidth() {
      if (lastView == null)
         return super.getCellWidth();
      return lastView.x + lastView.width;
   }
}