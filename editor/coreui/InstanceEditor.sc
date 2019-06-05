/** Editor base class for FormEditor and ReferenceEditor - both need to manage an instance and listen for changes */
abstract class InstanceEditor extends TypeEditor {
   Object instance;
   Object oldInstance;

   Object oldParentProperty;
   Object oldParentInst;

   InstanceWrapper wrapper;

   List<InstanceWrapper> instancesOfType;

   //Object instType := instance == null ? null : ModelUtil.resolveSrcTypeDeclaration(editorModel.system, DynUtil.getType(instance));

   instance =: instanceChanged();

   String referenceId := computeReferenceId(instance);

   // Is this a valid reference we should allow to be a link or do we just display the instanceName.
   boolean isReferenceable() {
      return instance == null || ModelUtil.isObjectType(type) || !(type instanceof java.util.Collection);
   }

   InstanceEditor(FormView view, TypeEditor parentEditor, Object parentProperty, Object type, Object inst, int listIx, InstanceWrapper wrapper) {
      super(view, parentEditor, parentProperty, type, inst, listIx, wrapper);
      instance = inst;
      this.wrapper = wrapper;
   }

   @sc.obj.ManualGetSet
   public void setTypeAndInstance(Object parentProperty, Object type, Object inst, InstanceWrapper wrapper) {
      setTypeNoChange(parentProperty, type);
      this.instance = inst;
      this.wrapper = wrapper;
      // Notify any bindings on instance and parentProperty that the value is changed but don't validate those bindings before we've refreshed the children.
      Bind.sendInvalidate(this, "instance", inst);
      Bind.sendInvalidate(this, "parentProperty", parentProperty);
      Bind.sendInvalidate(this, "wrapper", wrapper);
      // This both invalidates and validates for type
      Bind.sendChange(this, "type", type);
      refreshChildren();
      // Now we're ready to validate the bindings on the instance
      Bind.sendValidate(this, "parentProperty", parentProperty);
      Bind.sendValidate(this, "instance", inst);
      Bind.sendValidate(this, "wrapper", wrapper);
   }

   void updateEditor(Object elem, Object prop, Object propType, Object inst, int ix, InstanceWrapper wrapper) {
      listIndex = ix;
      setTypeAndInstance(prop, propType, inst, wrapper);
   }

   String computeReferenceId(Object instance) {
      if (instance == null)
         return "<unset>";
      else
         return CTypeUtil.getClassName(DynUtil.getInstanceName(instance));
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

   // When our instance changes, what do we need to do for the child views?
   void updateChildInsts() {
   }

   void updateParentPropListener(boolean add) {
      if (oldParentProperty == parentProperty)
         return;

      if (oldParentProperty != null && oldParentInst != null) {
         Bind.removeDynamicListener(oldParentInst, ModelUtil.getPropertyName(oldParentProperty), parentPropListener, IListener.VALUE_CHANGED);
      }

      if (!(parentEditor instanceof InstanceEditor) || parentProperty instanceof CustomProperty)
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
      if (parentInst != null) {
         String propName = ModelUtil.getPropertyName(parentProperty);
         Object newInst = DynUtil.getPropertyValue(parentInst, propName);
         instance = newInst;
      }
   }

   void changeFocus(boolean focus) {
      editorModel.changeFocus(parentProperty, instance);
   }

   String getInstanceId() {
      return instance == null ? "null" : DynUtil.getObjectId(instance, type, displayName);
   }

   IElementEditor createElementEditor(Object elem, int ix, IElementEditor oldTag, String displayMode) {
      throw new UnsupportedOperationException();
   }

   void gotoReference() {
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
      editorModel.changeCurrentType(useType, instance, null);
   }

   int getExplicitWidth(int colIx) {
      return -1;
   }

   int getExplicitHeight(int colIx) {
      return -1;
   }

   List<InstanceWrapper> getInstancesOfType(Object type, boolean addNull, String nullLabelName, boolean selectToCreate) {
      return editorModel.ctx.getInstancesOfType(type, 10, addNull, nullLabelName, selectToCreate);
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
      return -1;
   }
}
