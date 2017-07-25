/** The reference editor maintains a type or instance like FormEditor but displays it as a reference rather than by value. */
class ReferenceEditor extends InstanceEditor {
   ReferenceEditor(FormView view, TypeEditor parentEditor, Object parentProperty, Object type, Object inst) {
      super(view, parentEditor, parentProperty, type, inst);
   }

   String referenceId := DynUtil.getInstanceName(instance);

   // There are no children in the reference editor
   void refreshChildren() {
   }

   // Is this a valid reference we should allow to be a link or do we just display the instanceName.
   boolean isReferenceable() {
      return instance == null || ModelUtil.isObjectType(type) || !(type instanceof java.util.Collection);
   }

   public void gotoReference() {
      Object useType = type;
      if (instance != null) {
         if (DynUtil.isObject(instance)) {
            String objName = DynUtil.getObjectName(instance);
            Object instType = ModelUtil.findTypeDeclaration(editorModel.system, objName, null, false);
            if (instType != null)
               useType = instType;
         }
      }
      useType = ModelUtil.resolveSrcTypeDeclaration(editorModel.system, useType);
      editorModel.changeCurrentType(useType, instance);
   }
}