class ListEditor extends InstanceEditor {
   List<Object> instList;
   List<Object> visList;
   Object componentType;
   String componentTypeName;
   int startIx;
   int maxNum = 10;
   boolean gridView = true;

   gridView =: rebuildChildren(); // recreate the list in the new view mode

   // TODO: add sort-by and filter criteria

   ListEditor(FormView view, TypeEditor parentEditor, Object parentProperty, Object type, List<Object> insts, int listIx) {
      super(view, parentEditor, parentProperty, type, insts, listIx);
      instList = insts;
      componentType = ModelUtil.getArrayComponentType(type);
      updateComponentTypeName();
      refreshVisibleList();
   }

   @sc.obj.ManualGetSet // NOTE: get/set conversion not performed when this annotation is used
   void updateEditor(Object elem, Object prop, Object propType, Object inst, int ix) {
      Object compType = ModelUtil.getArrayComponentType(elem);
      setTypeNoChange(prop, compType);
      componentType = compType;
      updateComponentTypeName();
      updateListIndex(ix);
      this.instList = (List<Object>)inst;
      // Notify any bindings on 'instance' that the value is changed but don't validate those bindings before we've refreshed the children.
      Bind.sendInvalidate(this, "instList", inst);
      // This both invalidates and validates for type
      Bind.sendChange(this, "type", elem);
      refreshChildren();

      // Now we're ready to validate the bindings on the instance
      Bind.sendValidate(this, "instList", inst);
   }

   void updateListIndex(int ix) {
      listIndex = ix;
   }

   void updateComponentTypeName() {
      if (componentType != null && componentType != java.lang.Object.class)
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
      if (instance instanceof List && instance != instList)
         instList = (List) instance;
      refreshVisibleList();
      super.instanceChanged();
   }

   void refreshVisibleList() {
      oldLayer = editorModel == null ? null : editorModel.currentLayer;
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

   IElementEditor createElementEditor(Object listVal, int ix, IElementEditor oldTag, String displayMode) {
      Object propInst;
      Object propType;
      BodyTypeDeclaration innerType = null;

      Object compType = DynUtil.getType(listVal);
      compType = ModelUtil.resolveSrcTypeDeclaration(editorModel.system, compType);

      String editorType = getEditorType(parentProperty, compType, listVal, true);
      Object editorClass = getEditorClass(editorType, displayMode);

      int listIx = ix + startIx;
      Object oldClass = oldTag != null ? DynUtil.getType(oldTag) : null;
      if (oldClass == editorClass) {
         IElementEditor oldEditor = (IElementEditor) oldTag;
         oldEditor.updateEditor(compType, null, compType, listVal, listIx);
         oldEditor.setListIndex(listIx);
         return oldTag;
      }
      else {
         IElementEditor newEditor = (IElementEditor) DynUtil.newInnerInstance(editorClass, null, null, parentView, ListEditor.this, null, compType, listVal, listIx);
         newEditor.setRepeatVar(compType);
         newEditor.setRepeatIndex(ix);
         return newEditor;
      }
   }

   void gotoComponentType() {
      if (componentTypeName != null)
         editorModel.changeCurrentType(ModelUtil.findType(editorModel.system, componentTypeName), null);
   }

   Object getEditorClass(String editorType, String displayMode) {
      if (gridView) {
         if (editorType.equals("ref") || editorType.equals("form"))
            return RowEditor.class;
      }
      return super.getEditorClass(editorType, displayMode);
   }

   String getEditorType() {
      return "list";
   }
}
