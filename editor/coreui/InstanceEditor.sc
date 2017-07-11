/** Editor base class for FormEditor and ReferenceEditor - both need to manage an instance and listen for changes */
abstract class InstanceEditor extends TypeEditor {
   Object instance;
   Object oldInstance;

   instance =: instanceChanged();

   InstanceEditor(FormView view, TypeEditor parentEditor, Object parentProperty, Object type, Object inst) {
      super(view, parentEditor, parentProperty, type, inst);
      instance = inst;
   }

   @sc.obj.ManualGetSet
   public void setTypeAndInstance(Object type, Object inst) {
      setTypeNoChange(type);
      this.instance = inst;
      // Notify any bindings on 'instance' that the value is changed but don't validate those bindings before we've refreshed the children.
      Bind.sendInvalidate(this, "instance", inst);
      // This both invalidates and validates for type
      Bind.sendChange(this, "type", type);
      refreshChildren();
      // Now we're ready to validate the bindings on the instance
      Bind.sendValidate(this, "instance", inst);
   }

   void updateEditor(Object elem, Object prop, Object propType, Object inst) {
      setTypeAndInstance(elem, inst);
   }

   abstract void refreshChildren();

   void init() {

      super.init();
      instanceChanged();
   }

   void instanceChanged() {
      if (removed)
         return;
      if (instance != oldInstance) {
         childViewsChanged();
         oldInstance = instance;
      }
   }

   void childViewsChanged() {
      updateListeners();
      updateChildInsts();
   }

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