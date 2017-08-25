ToggleCellEditor {
   formComponent = toggle;

   object toggle extends JCheckBox {
      location := SwingUtil.point(ToggleCellEditor.this.x, ToggleCellEditor.this.y);
      size := SwingUtil.dimension(ToggleCellEditor.this.width, ToggleCellEditor.this.height);

      selected :=: checkedValue;

      userEnteredCount =: updateInstanceProperty();

      focus =: focusChanged(this, focus);
   }

   Object getElementValue() {
      return toggle.selected;
   }
}