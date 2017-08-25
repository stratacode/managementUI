CompositeEditor extends JPanel {
   @Bindable
   IElementEditor prev;

   @Bindable
   IElementEditor prevCell;

   // TODO: for now we are forcing the get/set conversion of the methods in IElementEditor but maybe we should detect this case - where a field overrides an abstract getX/setX methods?
   @Bindable
   int row, col;
}