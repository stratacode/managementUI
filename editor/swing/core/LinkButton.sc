class LinkButton extends JButton implements MouseListener {
   borderPainted = false;
   opaque = false;
   border = BorderFactory.createEmptyBorder();
   rolloverEnabled = true;
   Color defaultColor;
   cursor = new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR);
   boolean mouseIn = false;

   defaultColor =: foreground = (mouseIn ? Color.BLUE : defaultColor);

   {
      addMouseListener(this);
   }

   public void mouseClicked(MouseEvent e) {}

   public void mousePressed(MouseEvent e) {}

   public void mouseReleased(MouseEvent e) {}

   public void mouseEntered(MouseEvent e) {
      if (defaultColor == null)
         defaultColor = foreground;
      foreground = Color.BLUE;
      mouseIn = true;
   }

   public void mouseExited(MouseEvent e) {
      if (defaultColor != null)
         foreground = defaultColor;
      mouseIn = false;
   }
}