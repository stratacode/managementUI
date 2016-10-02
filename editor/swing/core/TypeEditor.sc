TypeEditor {
   int columnWidth := (int) ((parentView.size.width - scrollBorder - 2 * borderSize) / parentView.numCols - parentView.xpad);
   int startY := parentView.ypad + borderTop;

   int scrollBorder = 25; // space for the scroll bar should it be needed

   int borderSize = 2;
   int borderTop = 25;
   int borderBottom = 0;

   int xpad := parentView.xpad;
   int ypad := parentView.ypad;

   int baseline := parentView.baseline;

   int tabSize := parentView.tabSize;
}