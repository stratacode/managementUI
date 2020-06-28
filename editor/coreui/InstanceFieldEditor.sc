
/**
 * Base class for FormEditor and RowEditor - contains the functionality to manage a list of fields in the instance
 */
abstract class InstanceFieldEditor extends InstanceEditor {
   InstanceFieldEditor(FormView view, TypeEditor parentEditor, Object parentProperty, Object type, Object instance, int listIx, InstanceWrapper wrapper, boolean instanceEditor) {
      super(view, parentEditor, parentProperty, type, instance, listIx, wrapper, instanceEditor);
   }

   /* moved to InstanceEditor
   void parentInstanceChanged(Object parentInst) {
      if (parentInst == null)
         instance = null;
      else {
         String propName = parentProperty == null ? CTypeUtil.getClassName(ModelUtil.getInnerTypeName(type)) : ModelUtil.getPropertyName(parentProperty);
         if (DynUtil.hasProperty(parentInst, propName))
            instance = DynUtil.getProperty(parentInst, propName);
         else
            instance = null;
      }
   }
   */

   void updateChildInsts() {
      if (childViews != null) {
         for (IElementEditor view:childViews) {
            if (view instanceof TypeEditor) {
               TypeEditor te = ((TypeEditor) view);
               te.parentInstanceChanged(instance);
            }
         }
      }
   }

}
