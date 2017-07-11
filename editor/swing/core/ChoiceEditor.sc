import java.util.Arrays;

ChoiceEditor {
   formComponent = choiceField;

   object choiceField extends JComboBox {
      location := SwingUtil.point(elementLabel.location.x + elementLabel.size.width + xpad, ChoiceEditor.this.y);
      size := SwingUtil.dimension(ChoiceEditor.this.width - elementLabel.size.width - elementLabel.location.x - 2*xpad-formEditor.borderSize, preferredSize.height);
      items := Arrays.asList(choices);

      selectedIndex :=: ChoiceEditor.this.selectedIndex;

      userSelectedItem =: updateInstanceProperty();
   }

   Object getElementValue() {
      return choiceField.selectedItem;
   }
}