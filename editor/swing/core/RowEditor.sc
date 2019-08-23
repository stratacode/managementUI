RowEditor {
   numCols := DynUtil.getArrayLength(properties);
   borderTop = 0;

   int getCellHeight() {
      return rowHeight;
   }

   int getCellWidth() {
      if (lastView == null)
         return super.getCellWidth();
      return lastView.x + lastView.width;
   }

   void updateComputedValues() {
      super.updateComputedValues();
      xpad = 0; // header cells are direct children of the grid so to line up with them, need to start out at 0
      ypad = 0;
   }
}