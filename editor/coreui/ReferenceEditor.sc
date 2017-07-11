/** The reference editor maintains a type or instance like FormEditor but displays it as a reference rather than by value. */
class ReferenceEditor extends InstanceEditor {
   ReferenceEditor(FormView view, TypeEditor parentEditor, Object parentProperty, Object type, Object inst) {
      super(view, parentEditor, parentProperty, type, inst);
   }

   // There are no children in the reference editor
   void refreshChildren() {
   }

   boolean getHasReference() {
      return instance != null;
   }

   public String getReferenceId() {
      return DynUtil.getInstanceName(instance);
   }

   public void gotoReference() {
      Object useType = type;
      if (instance != null)
         useType = DynUtil.getType(instance);
      editorModel.changeCurrentType(useType, instance);
   }
}