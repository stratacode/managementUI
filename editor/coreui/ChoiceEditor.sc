class ChoiceEditor extends ElementEditor {
   Object[] choices := ModelUtil.getEnumConstants(propType);

   int selectedIndex := indexOf(choices, currentValue);
   selectedIndex =: updateSelection();

   private void updateSelection() {
      if (selectedIndex == -1 || choices == null || selectedIndex > choices.length)
         return;
      if (currentValue != choices[selectedIndex])
         currentValue = choices[selectedIndex];
   }

   private int indexOf(Object[] arr, Object val) {
      for (int i = 0; i < arr.length; i++)
         if (arr[i] == val)
            return i;
      return -1;
   }
}
