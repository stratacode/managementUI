import java.util.Arrays;

ChoiceFieldEditor {
   formComponent = choiceField;

   object elementLabel extends ElementLabel {}

   object choiceField extends JComboBox {
      location := SwingUtil.point(elementLabel.location.x + elementLabel.size.width + xpad, ChoiceFieldEditor.this.y);
      size := SwingUtil.dimension(ChoiceFieldEditor.this.width - elementLabel.size.width - elementLabel.location.x - 2*xpad-formEditor.borderSize, preferredSize.height);
      items := Arrays.asList(choices);

      selectedIndex :=: ChoiceFieldEditor.this.selectedIndex;

      userSelectedItem =: updateInstanceProperty();
   }

   Object getElementValue() {
      return choiceField.selectedItem;
   }
}