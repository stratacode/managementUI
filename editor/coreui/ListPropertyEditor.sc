class ListPropertyEditor extends ListGridEditor {

   ListPropertyEditor(FormView view, TypeEditor parentEditor, Object parentProperty, Object type, List<Object> insts, int listIx, InstanceWrapper wrapper, boolean instanceEditor) {
      super(view, parentEditor, parentProperty, type, insts, listIx, wrapper, instanceEditor);
   }

   ListPropertyEditor(FormView view, TypeEditor parentEditor, Object compType, Object instList, boolean instanceEditor) {
      super(view, parentEditor, compType, instList, instanceEditor);
   }
}
