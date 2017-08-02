class ListEditor extends InstanceEditor {
   List<Object> instList;
   List<Object> visList;
   Object componentType;
   String componentTypeName;
   int startIx;
   int maxNum = 10;
   // TODO: add sort-by and filter criteria

   ListEditor(FormView view, TypeEditor parentEditor, Object parentProperty, Object type, List<Object> insts) {
      super(view, parentEditor, parentProperty, type, insts);
      instList = insts;
      componentType = ModelUtil.getArrayComponentType(type);
      updateComponentTypeName();
      refreshVisibleList();
   }

   @sc.obj.ManualGetSet // NOTE: get/set conversion not performed when this annotation is used
   void updateEditor(Object elem, Object prop, Object propType, Object inst) {
      Object compType = ModelUtil.getArrayComponentType(elem);
      setTypeNoChange(prop, compType);
      componentType = compType;
      updateComponentTypeName();
      this.instList = (List<Object>)inst;
      // Notify any bindings on 'instance' that the value is changed but don't validate those bindings before we've refreshed the children.
      Bind.sendInvalidate(this, "instList", inst);
      // This both invalidates and validates for type
      Bind.sendChange(this, "type", elem);
      refreshChildren();

      // Now we're ready to validate the bindings on the instance
      Bind.sendValidate(this, "instList", inst);
   }

   void updateComponentTypeName() {
      if (type != null && componentType != java.lang.Object.class)
         componentTypeName = ModelUtil.getTypeName(componentType);
   }

   String getFixedOperatorName() {
      return "list";
   }

   void refreshChildren() {
      refreshVisibleList();
   }

   // Instance change events where the instance itself has not changed might mean that the contents of the list have been changed
   void instanceChanged() {
      refreshVisibleList();
      super.instanceChanged();
   }

   void refreshVisibleList() {
      if (instList == null) {
         visList = new ArrayList();
      }
      else {
         int inSz = instList.size();
         int outSz = Math.min(inSz - startIx, maxNum);
         if (visList == null) {
            visList = new ArrayList<Object>(outSz);
         }
         else
            visList.clear();
         for (int i = 0; i < outSz; i++) {
            visList.add(instList.get(i + startIx));
         }
      }
   }

   IElementEditor createListElementEditor(Object listVal, int ix, IElementEditor oldTag) {
      Object propInst;
      Object propType;
      BodyTypeDeclaration innerType = null;

      Object compType = DynUtil.getType(listVal);
      compType = ModelUtil.resolveSrcTypeDeclaration(editorModel.system, compType);

      String editorType = getEditorType(parentProperty, compType, listVal, true);
      Object editorClass = getEditorClass(editorType);

      Object oldClass = oldTag != null ? DynUtil.getType(oldTag) : null;
      if (oldClass == editorClass) {
         IElementEditor oldEditor = (IElementEditor) oldTag;
         oldEditor.updateEditor(compType, null, compType, listVal);
         oldEditor.setListIndex(ix + startIx);
         return oldTag;
      }
      else {
         IElementEditor newEditor = (IElementEditor) DynUtil.newInnerInstance(editorClass, null, null, parentView, ListEditor.this, null, compType, listVal);
         newEditor.setRepeatVar(compType);
         newEditor.setRepeatIndex(ix);
         newEditor.setListIndex(ix + startIx);
         return newEditor;
      }
   }

   void gotoComponentType() {
      if (componentTypeName != null)
         editorModel.changeCurrentType(ModelUtil.findType(editorModel.system, componentTypeName), null);
   }
}
