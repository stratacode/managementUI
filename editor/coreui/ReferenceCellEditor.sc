class ReferenceCellEditor extends ReferenceEditor {
   cellMode = true;
   ReferenceCellEditor(FormView view, TypeEditor parentEditor, Object parentProperty, Object type, Object inst, int listIx, InstanceWrapper wrapper, boolean instanceEditor) {
      super(view, parentEditor, parentProperty, type, inst, listIx, wrapper, instanceEditor);
   }
}
