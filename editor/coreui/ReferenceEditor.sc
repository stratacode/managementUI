/** The reference editor maintains a type or instance like FormEditor but displays it as a reference rather than by value. */
class ReferenceEditor extends InstanceEditor {
   ReferenceEditor(FormView view, TypeEditor parentEditor, Object parentProperty, Object type, Object inst, int listIx) {
      super(view, parentEditor, parentProperty, type, inst, listIx);
   }

   // There are no children in the reference editor
   void refreshChildren() {
   }

   String getEditorType() {
      return "ref";
   }
}
