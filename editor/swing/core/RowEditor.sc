RowEditor {
   int headerHeight = 25;
   numCols := DynUtil.getArrayLength(properties);
   borderTop := showHeader ? headerHeight : 0;
   object headerList extends ChildList {
      displayMode = "header";
      repeat := properties;
      visible := showHeader;
   }

   void validateTree() {
      headerList.refreshList();
      super.validateTree();
   }

   void refreshChildren() {
      headerList.refreshList();
      super.refreshChildren();
   }

   int getExplicitWidth(int colIx) {
      if (setWidths == null)
         return -1;
      Integer setWidth = setWidths.get(colIx);
      return setWidth == null ? -1 : setWidth;
   }

   int getExplicitHeight(int colIx) {
      return rowHeight;
   }

}