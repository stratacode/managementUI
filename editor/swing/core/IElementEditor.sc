IElementEditor {
   int getX();
   int getY();
   int getWidth();
   int getHeight();
   // TODO: rename this prevRow since we use it for vertical positioning?
   IElementEditor getPrev();
   void setPrev(IElementEditor view);
   IElementEditor getPrevCell();
   void setPrevCell(IElementEditor view);
   void setRow(int row);
   int getRow();
   int getCol();
   void setCol(int col);
}

