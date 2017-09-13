ReferenceCellEditor {
   object nameButton extends LinkButton {
      location := SwingUtil.point(xpad, ypad + baseline);
      size := SwingUtil.dimension(Math.min(preferredSize.width, cellWidth-2*xpad), preferredSize.height);
      text := referenceId;
      clickCount =: gotoReference();
      foreground := transparentType ? GlobalResources.transparentTextColor : GlobalResources.normalTextColor;
      enabled := referenceable;

      void repaint() {
         super.repaint();
      }

      public void paint(java.awt.Graphics g) {
         super.paint(g);
      }
   }

   public void paint(java.awt.Graphics g) {
      super.paint(g);
   }

   border = ElementEditor.createCellBorder();
   height := cellHeight;
}