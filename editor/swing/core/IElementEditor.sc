IElementEditor {
   int getX();
   int getY();
   int getWidth();
   int getHeight();
   IElementEditor getPrev();
   void setPrev(IElementEditor view);
   void setRow(int row);
   int getRow();
   int getCol();
   void setCol(int col);
}

