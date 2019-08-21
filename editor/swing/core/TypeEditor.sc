TypeEditor {
   int columnWidth := (int) ((parentView.size.width - scrollBorder - 2 * borderSize) / parentView.numCols - parentView.xpad);
   int startY := borderTop;

   int scrollBorder = 25; // space for the scroll bar should it be needed

   int borderSize = 2;
   int borderTop = 30;
   int borderBottom = 0;

   int xpad, ypad, baseline, tabSize;

   // Space between children in the horizontal direction - used for listCells where the elements are separated
   int xsep = 0;

   IElementEditor lastView;

   @Bindable
   /*
   int x := col * (xpad + columnWidth) + (parentView.nestWidth/2) + xpad,
       y := prev == null ? ypad + (parentEditor == null ? 0 : parentEditor.startY) : prev.y + prev.height + ypad,
       width := columnWidth - (nestLevel * (parentView.nestWidth + 2*xpad)),
       height := (lastView == null ? startY : lastView.y + lastView.height) + 2 * ypad + borderSize + borderBottom;
    */
   int x := prevCell == null ? xpad : prevCell.x + prevCell.width + formEditor.xsep,
       y := prev == null ? formEditor.startY : prev.y + prev.height,
       width := cellWidth, height := cellHeight;

   int row, col;

   size := SwingUtil.dimension(width, height);
   location := SwingUtil.point(x, y);

   boolean transparentType := !ModelUtil.isTypeInLayer(type, classViewLayer);

   int titleBorderX = 30;

   @Bindable
   Object repeatVar;

   @Bindable
   int repeatIndex; // If we are in a repeat list, the index in that list (which may be a filtered subset of the original list)

   public static Icon getSwingIcon(UIIcon icon) {
      return icon == null ? null : icon.icon;
   }

   void updateComputedValues() {
      xpad = parentView.xpad;
      if (cellChild)
         ypad = 0;
      else
         ypad = parentView.ypad;
      baseline = parentView.baseline;
      tabSize = parentView.tabSize;
   }

   void validateEditorTree() {
   }

   void paint(java.awt.Graphics g) {
      super.paint(g);
   }

   int getCellWidth() {
      if (cellMode) {
         return super.getCellWidth();
      }
      return columnWidth - (nestLevel * (parentView.nestWidth + 2*xpad));
   }

   int getCellHeight() {
      if (cellMode || rowMode) {
         return super.getCellHeight();
      }
      if (lastView instanceof ListEditor) {
         ((ListEditor) lastView).validateEditorTree();
      }

      return (lastView == null ? startY : lastView.y + lastView.height) + 2 * ypad + borderSize + borderBottom;
   }

   void validateSize() {
      super.validateSize();
      Bind.refreshBinding(this, "x");
      Bind.refreshBinding(this, "y");
      Bind.refreshBinding(this, "width");
      Bind.refreshBinding(this, "height");
   }
}