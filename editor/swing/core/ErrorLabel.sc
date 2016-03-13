class ErrorLabel extends JLabel {
   foreground = new Color(0xc7, 0x41, 0x41);
   size := preferredSize;
   JTextField errorField;
   location := SwingUtil.point(errorField.location.x, errorField.location.y + errorField.size.height + 5);
}