class ConfirmButtons extends ComponentGroup implements PanelStyle {
   boolean enabled = true;
   int x;
   int y;
   int width := (int) (okButton.size.width + cancelButton.size.width + 2*icxpad);
   int icxpad = 2;
   object okButton extends StatusButton {
      location := SwingUtil.point(ConfirmButtons.this.x + icxpad, ConfirmButtons.this.y);
      icon = new ImageIcon(ConfirmButtons.class.getResource("images/ok.png"), "OK");
      visible := ConfirmButtons.this.visible;
      enabled := ConfirmButtons.this.enabled;
      focusPainted = false;
      borderPainted = false;
   }
   object cancelButton extends StatusButton {
      location := SwingUtil.point(okButton.location.x + okButton.size.width + icxpad, ConfirmButtons.this.y);
      icon = new ImageIcon(ConfirmButtons.class.getResource("images/cancel.png"), "Cancel");
      visible := ConfirmButtons.this.visible;
      enabled := ConfirmButtons.this.enabled;
      focusPainted = false;
      borderPainted = false;
   }
}
