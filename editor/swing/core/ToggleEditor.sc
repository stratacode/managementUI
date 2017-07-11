ToggleEditor {
   formComponent = toggle;

   object toggle extends JCheckBox {
      location := SwingUtil.point(elementLabel.location.x + elementLabel.size.width + xpad, ToggleEditor.this.y);
      size := SwingUtil.dimension(ToggleEditor.this.width - elementLabel.size.width - elementLabel.location.x - 2*xpad-formEditor.borderSize, preferredSize.height);

      selected :=: checkedValue;

      userEnteredCount =: updateInstanceProperty();

      focus =: focusChanged(this, focus);
   }

   Object getElementValue() {
      return toggle.selected;
   }
}