/** Editor base class for FormEditor and ReferenceEditor - both need to manage an instance and listen for changes */
abstract class InstanceEditor extends TypeEditor {
   Object instance;
   Object oldInstance;

   Object oldParentProperty;
   Object oldParentInst;

   //Object instType := instance == null ? null : ModelUtil.resolveSrcTypeDeclaration(editorModel.system, DynUtil.getType(instance));

   instance =: instanceChanged();

   InstanceEditor(FormView view, TypeEditor parentEditor, Object parentProperty, Object type, Object inst) {
      super(view, parentEditor, parentProperty, type, inst);
      instance = inst;
   }

   @sc.obj.ManualGetSet
   public void setTypeAndInstance(Object parentProperty, Object type, Object inst) {
      setTypeNoChange(parentProperty, type);
      this.instance = inst;
      // Notify any bindings on instance and parentProperty that the value is changed but don't validate those bindings before we've refreshed the children.
      Bind.sendInvalidate(this, "instance", inst);
      Bind.sendInvalidate(this, "parentProperty", parentProperty);
      // This both invalidates and validates for type
      Bind.sendChange(this, "type", type);
      refreshChildren();
      // Now we're ready to validate the bindings on the instance
      Bind.sendValidate(this, "parentProperty", parentProperty);
      Bind.sendValidate(this, "instance", inst);
   }

   void updateEditor(Object elem, Object prop, Object propType, Object inst) {
      setTypeAndInstance(prop, propType, inst);
   }

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
      updateListeners(true);
      updateChildInsts();
   }

   void updateListeners(boolean add) {
      super.updateListeners(add);
      updateParentPropListener(add);
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

   void updateParentPropListener(boolean add) {
      if (oldParentProperty == parentProperty)
         return;

      if (oldParentProperty != null && oldParentInst != null) {
         Bind.removeDynamicListener(oldParentInst, ModelUtil.getPropertyName(oldParentProperty), parentPropListener, IListener.VALUE_CHANGED);
      }

      if (!(parentEditor instanceof InstanceEditor))
         return;

      Object parentInst = ((InstanceEditor)parentEditor).instance;
      if (add && parentProperty != null && parentInst != null) {
         Bind.addDynamicListener(parentInst, ModelUtil.getPropertyName(parentProperty), parentPropListener, IListener.VALUE_CHANGED);
         oldParentProperty = parentProperty;
         oldParentInst = parentInst;
      }
      else {
         oldParentProperty = null;
         oldParentInst = null;
      }
   }

   object parentPropListener extends AbstractListener {
      public boolean valueValidated(Object obj, Object prop, Object eventDetail, boolean apply) {
         parentPropValueChanged();
         return true;
      }
   }

   void parentPropValueChanged() {
      Object parentInst = ((InstanceEditor) parentEditor).instance;
      String propName = ModelUtil.getPropertyName(parentProperty);
      Object newInst = DynUtil.getPropertyValue(parentInst, propName);
      instance = newInst;
   }

   void changeFocus(boolean focus) {
      editorModel.changeFocus(parentProperty, instance);
   }
}