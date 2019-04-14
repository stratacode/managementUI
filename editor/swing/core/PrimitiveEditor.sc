PrimitiveEditor extends ComponentGroup {
   @Bindable
   IElementEditor prev;

   @Bindable
   IElementEditor prevCell;

   @Bindable
   int row, col;

   @Bindable
   Object elemToEdit;

   @Bindable
   int repeatIndex;
}
