import sc.type.RTypeUtil;

LabelCellEditor {
   formComponent = textLabel;

   object textLabel extends JLabel {
      location := SwingUtil.point(LabelCellEditor.this.x, LabelCellEditor.this.y);
      size := SwingUtil.dimension(LabelCellEditor.this.width, LabelCellEditor.this.height);

      border = createCellBorder();

      text := currentStringValue;

      // TODO: is there a way to select a label in swing?
      //focus =: focusChanged(this, focus);
   }

   String getElementStringValue() {
      return currentStringValue;
   }
}
