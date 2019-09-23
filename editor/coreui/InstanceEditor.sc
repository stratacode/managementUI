import sc.lang.java.DeclarationType;

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
      updateInstance();
   }

   void updateInstance() {
      oldInstance = instance;
   }

   void instanceChanged() {
      if (removed)
         return;
      if (instance != oldInstance) {
         childViewsChanged(false);
         oldInstance = instance;
      }
   }

   void childViewsChanged(boolean fromParent) {
      updateListeners(true);
      // When we are rebuilding the child list anyway don't up the child instances. If this is only an update
      // to the instance for this editor, we need to let any child views know that the parent instance has
      // changed.
      if (!fromParent)
         updateChildInsts();
   }

   void updateListeners(boolean add) {
      super.updateListeners(add);
      updateParentPropListener(add);
   }

   void removeListeners() {
      super.removeListeners();
      updateParentPropListener(false);
   }

   // When our instance changes, what do we need to do for the child views?
   void updateChildInsts() {
   }

   void updateParentPropListener(boolean add) {
      Object parentInst;
      if (!(parentEditor instanceof InstanceEditor) || parentProperty instanceof CustomProperty)
         parentInst = null;
      else
         parentInst = ((InstanceEditor)parentEditor).instance;

      // No changes to the listener
      if (oldParentProperty == parentProperty && oldParentInst == parentInst && add)
         return;

      if (oldParentProperty != null && oldParentInst != null) {
         Bind.removeDynamicListener(oldParentInst, ModelUtil.getPropertyName(oldParentProperty), parentPropListener, IListener.VALUE_CHANGED);
         oldParentProperty = null;
         oldParentInst = null;
      }

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
         if (parentProperty instanceof CustomProperty) {
            return;
         }
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

   boolean hasInnerTypeInstance(BodyTypeDeclaration subType) {
      if (InstanceEditor.this.instance != null && subType.getDeclarationType() == DeclarationType.OBJECT) {
         String scopeName = DynUtil.getScopeNameForType(subType);
         if (scopeName != null)
            return false;
         return true;
      }
      return false;
   }

   Object getInnerTypeInstance(BodyTypeDeclaration subType) {
      Object subInst = null;
      if (hasInnerTypeInstance(subType)) {
         subInst = DynUtil.getProperty(InstanceEditor.this.instance, subType.typeName);
      }
      return subInst;
   }

   IElementEditor createElementEditor(Object elem, int ix, IElementEditor oldTag, String displayMode) {
      Object propInst;
      Object propType;
      BodyTypeDeclaration innerType = null;
      boolean headerMode = displayMode != null && displayMode.equals("header");

      Object prop = null;
      // A property
      if (elem instanceof CustomProperty) {
         prop = elem;
         CustomProperty cust = (CustomProperty) elem;
         propType = cust.propertyType;
         propInst = cust.value;
      }
      else if (ModelUtil.isProperty(elem)) {
         prop = elem;
         propType = ModelUtil.getPropertyType(elem, editorModel.system);
         propInst = ElementEditor.getPropertyValue(elem, InstanceEditor.this.instance, 0, instanceMode, headerMode);
      }

      else if (elem instanceof BodyTypeDeclaration) {
         innerType = (BodyTypeDeclaration) elem;
         if (!headerMode)
            propInst = getInnerTypeInstance(innerType);
         else
            propInst = null;
         propType = innerType;
      }
      else {
         propInst = null;
         propType = null;
      }

      propInst = convertEditorInst(propInst);

      String editorType = getEditorType(prop, propType, propInst, instanceMode);
      Object editorClass = getEditorClass(editorType, displayMode);

      Object oldClass = oldTag != null ? DynUtil.getType(oldTag) : null;
      if (oldClass == editorClass) {
         IElementEditor oldEditor = (IElementEditor) oldTag;
         oldEditor.updateEditor(elem, prop, propType, propInst, ix, null);
         oldEditor.setElemToEdit(elem);
         return oldTag;
      }
      else {
         IElementEditor newEditor = (IElementEditor) DynUtil.newInnerInstance(editorClass, null, null, parentView, InstanceEditor.this, prop, propType, propInst, ix, null);
         newEditor.setElemToEdit(elem);
         //newEditor.setRepeatIndex(ix);
         return newEditor;
      }
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

   void parentInstanceChanged(Object parentInst) {
      if (parentInst == null)
         instance = null;
      else {
         if (parentProperty instanceof CustomProperty)
            return;
         String propName = parentProperty == null ? CTypeUtil.getClassName(ModelUtil.getInnerTypeName(type)) : ModelUtil.getPropertyName(parentProperty);
         if (DynUtil.hasProperty(parentInst, propName))
            instance = DynUtil.getProperty(parentInst, propName);
         else
            instance = null;
      }
   }
}
