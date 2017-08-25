class FormEditor extends InstanceFieldEditor {
   int instSelectedIndex = 0;

   List<InstanceWrapper> instancesOfType := parentView.editorModel.ctx.getInstancesOfType(type, 10, true);

   boolean showInstanceSelect := parentProperty == null && instanceMode && type != null && !ModelUtil.isObjectType(type);

   String extendsTypeLabel := extTypeName == null ? "" : (parentProperty == null ? "extends" : "type");
   String extendsTypeName := extTypeName == null ? "" : CTypeUtil.getClassName(extTypeName);

   int refreshInstancesCt := editorModel.refreshInstancesCt;
   refreshInstancesCt =: editorModel.refreshInstancesCheck(this);

   FormEditor(FormView view, TypeEditor parentEditor, Object parentProperty, Object type, Object instance, int listIx) {
      super(view, parentEditor, parentProperty, type, instance, listIx);
   }

   int getInstSelectedIndex(Object inst, List<InstanceWrapper> instsOfType) {
      int i = 0;
      if (instancesOfType != null) {
         for (InstanceWrapper wrap:instancesOfType) {
            if (wrap.instance == inst)
               return i;
            i++;

         }
      }
      //if (inst == null)  needed for js version?
      //   return 0;
      return -1;
   }

   void gotoExtendsType() {
      editorModel.changeCurrentType(ModelUtil.findType(editorModel.system, extTypeName), null);
   }

   String getEditorType() {
      return "form";
   }
}
