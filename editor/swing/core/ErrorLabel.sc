class ErrorLabel extends JLabel {
   foreground = new Color(0xc7, 0x41, 0x41);
   size := preferredSize;
   JComponent errorComponent;
   location := SwingUtil.point(errorComponent.location.x, errorComponent.location.y + errorComponent.size.height + 5);
}