class ListEditor extends InstanceEditor {
   List<Object> instList;
   List<Object> visList;
   Object componentType;
   String componentTypeName;
   int startIx;
   int maxNum = 10;
   // TODO: add sort-by and filter criteria

   static class SortProp {
      String propName;
      boolean reverseDir;
   }

   ArrayList<SortProp> sortProps = null;

   ListEditor(FormView view, TypeEditor parentEditor, Object parentProperty, Object type, List<Object> insts, int listIx, InstanceWrapper wrapper) {
      super(view, parentEditor, parentProperty, type, insts, listIx, wrapper);
      instList = insts;
      componentType = ModelUtil.getArrayComponentType(type);
      updateComponentTypeName();
      refreshVisibleList();
   }

   @sc.obj.ManualGetSet // NOTE: get/set conversion not performed when this annotation is used
   void updateEditor(Object elem, Object prop, Object propType, Object inst, int ix, InstanceWrapper wrapper) {
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

   int getCurrentListSize() {
      return visList == null ? -1 : visList.size();
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

   void updateInstance() {
      updateInstList();
   }

   void updateInstList() {
      if (instance instanceof List && instance != instList)
         instList = (List) instance;
      else if (instance == null)
         instList = null;
   }

   // Instance change events where the instance itself has not changed might mean that the contents of the list have been changed
   void instanceChanged() {
      updateInstList();
      refreshVisibleList();
      super.instanceChanged();

      parentView.scheduleValidateTree();
   }

   // return 0 - no sort, -1 reverse, 1 forward
   int getSortDir(String propName) {
      if (sortProps == null)
         return 0;
      for (SortProp sp:sortProps) {
         if (sp.propName.equals(propName))
            return sp.reverseDir ? -1 : 1;
      }
      return 0;
   }

   // val = 0 => remove sort, val = -1 reverse, val = 1 normal
   void updateSortDir(String propName, int val) {
      boolean add = false;
      if (sortProps == null) {
         if (val == 0)
            return;
         sortProps = new ArrayList<SortProp>();
         add = true;
      }
      else if (val == 0) {
         for (int i = 0; i < sortProps.size(); i++) {
            SortProp sp = sortProps.get(i);
            if (sp.propName.equals(propName)) {
               sortProps.remove(i);
               if (sortProps.size() == 0)
                  sortProps = null;
               break;
            }
         }
      }
      else
         add = true;
      if (add) {
         SortProp sp;
         int i;
         for (i = 0; i < sortProps.size(); i++) {
            sp = sortProps.get(i);
            if (sp.propName.equals(propName)) {
               sp.reverseDir = val == -1;
               break;
            }
         }
         if (i == sortProps.size()) {
            sp = new SortProp();
            sp.propName = propName;
            sp.reverseDir = val == -1;
            sortProps.add(sp);
         }
      }
      refreshVisibleList();
   }

   List<Object> filterAndSort() {
      if (sortProps == null)
         return instList;
      ArrayList<Object> res = new ArrayList<Object>(instList);
      java.util.Collections.sort(res, new java.util.Comparator() {
         int compare(Object o1, Object o2) {
            int res = 0;
            for (SortProp sp:sortProps) {
               String propName = sp.propName;
               Object pv1 = DynUtil.getPropertyValue(o1, propName);
               Object pv2 = DynUtil.getPropertyValue(o2, propName);
               int revMult = sp.reverseDir ? -1 : 1;
               if (pv1 == pv2)
                  res = 0;
               else {
                  if (pv1 instanceof Comparable) {
                     res = ((Comparable) pv1).compareTo(pv2) * revMult;
                  }
                  else if (pv2 instanceof Comparable) {
                     res = -((Comparable) pv2).compareTo(pv1) * revMult;
                  }
                  else {
                     System.err.println("*** Unable to compare values of property: " + propName);
                     res = 0;
                  }
               }
               if (res != 0)
                  return res;
            }
            return res;
         }
      });
      return res;
   }

   void refreshVisibleList() {
      oldLayer = editorModel == null ? null : editorModel.currentLayer;
      if (instList == null) {
         if (visList == null)
            visList = new ArrayList<Object>();
         else if (visList.size() > 0)
            visList.clear();
      }
      else {
         List<Object> inList = filterAndSort();
         int inSz = inList.size();
         int outSz = Math.min(inSz - startIx, maxNum);

         List<Object> newList;
         if (startIx != 0 || maxNum < inSz) {
            newList = new ArrayList<Object>(outSz);
            for (int i = 0; i < outSz; i++) {
               newList.add(inList.get(i + startIx));
            }
         }
         else
            newList = inList;
         if (visList == null) {
            visList = newList == instList ? new ArrayList<Object>(newList) : newList;
         }
         else {
            int oldSz = visList.size();
            for (int i = 0; i < outSz; i++) {
               Object newVal = newList.get(i);
               if (i >= oldSz)
                  visList.add(newVal);
               else if (visList.get(i) != newVal) {
                  visList.set(i, newVal);
               }
            }
            int toRemove = oldSz - outSz;
            while (toRemove > 0) {
               visList.remove(visList.size()-1);
               toRemove--;
            }
         }
      }
   }

   IElementEditor createElementEditor(Object listVal, int ix, IElementEditor oldTag, String displayMode) {
      Object propInst;
      Object propType;
      BodyTypeDeclaration innerType = null;

      Object compType = DynUtil.getType(listVal);
      compType = ModelUtil.resolveSrcTypeDeclaration(editorModel.system, compType);

      listVal = convertEditorInst(listVal);

      String editorType = getEditorType(parentProperty, compType, listVal, true);
      Object editorClass = getEditorClass(editorType, displayMode);

      int listIx = ix + startIx;
      Object oldClass = oldTag != null ? DynUtil.getType(oldTag) : null;
      if (oldClass == editorClass) {
         IElementEditor oldEditor = (IElementEditor) oldTag;
         oldEditor.updateEditor(null, null, compType, listVal, listIx, null);
         oldEditor.setListIndex(listIx);
         return oldTag;
      }
      else {
         IElementEditor newEditor = (IElementEditor) DynUtil.newInnerInstance(editorClass, null, null, parentView, ListEditor.this, null, compType, listVal, listIx, null);
         newEditor.setElemToEdit(compType);
         newEditor.setRepeatIndex(ix);
         return newEditor;
      }
   }

   void gotoComponentType() {
      if (componentTypeName != null)
         editorModel.changeCurrentType(ModelUtil.findType(editorModel.system, componentTypeName), null, null);
   }

   String getEditorType() {
      return "list";
   }
}
