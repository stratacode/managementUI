import java.util.Arrays;

ChoiceCellEditor {
   formComponent = choiceField;

   object choiceField extends JComboBox {
      location := SwingUtil.point(ChoiceCellEditor.this.x, ChoiceCellEditor.this.y);
      size := SwingUtil.dimension(ChoiceCellEditor.this.width, ChoiceCellEditor.this.height);
      items := Arrays.asList(choices);

      selectedIndex :=: ChoiceCellEditor.this.selectedIndex;

      userSelectedItem =: updateInstanceProperty();
   }

   Object getElementValue() {
      return choiceField.selectedItem;
   }
}