class ReferenceFieldEditor extends ReferenceEditor {
   ReferenceFieldEditor(FormView view, TypeEditor parentEditor, Object parentProperty, Object type, Object inst, int listIx, InstanceWrapper wrapper) {
      super(view, parentEditor, parentProperty, type, inst, listIx, wrapper);
   }

   instancesOfType := getInstancesOfType(type, true, "<unset>", false);
}
