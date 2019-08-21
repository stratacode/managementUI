ListCellEditor {
   numRows = 1;
   numCols := DynUtil.getArrayLength(visList);
   borderTop = 6;
   background = Color.WHITE;
   xsep = 5;
   border = BorderFactory.createLineBorder(Color.BLACK);

   int getCellWidth() {
      return super.getCellWidth();
   }

   int getDefaultCellHeight(String editorType, Object prop) {
      return super.getDefaultCellHeight(editorType, prop) - 8;
   }
}