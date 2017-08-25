ToggleFieldEditor {
   formComponent = toggle;

   object elementLabel extends ElementLabel {}

   object toggle extends JCheckBox {
      location := SwingUtil.point(elementLabel.location.x + elementLabel.size.width + xpad, ToggleFieldEditor.this.y);
      size := SwingUtil.dimension(ToggleFieldEditor.this.width - elementLabel.size.width - elementLabel.location.x - 2*xpad-formEditor.borderSize, preferredSize.height);

      selected :=: checkedValue;

      userEnteredCount =: updateInstanceProperty();

      focus =: focusChanged(this, focus);
   }

   Object getElementValue() {
      return toggle.selected;
   }
}