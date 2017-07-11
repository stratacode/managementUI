import sc.lang.java.DeclarationType;

class FormEditor extends InstanceEditor {

   int instSelectedIndex = 0;

   List<InstanceWrapper> instancesOfType := parentView.editorModel.ctx.getInstancesOfType(type, 10, true);

   FormEditor(FormView view, TypeEditor parentEditor, Object parentProperty, Object type, Object instance) {
      super(view, parentEditor, parentProperty, type, instance);
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

   boolean hasInnerTypeInstance(BodyTypeDeclaration subType) {
      if (FormEditor.this.instance != null && subType.getDeclarationType() == DeclarationType.OBJECT) {
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
         subInst = DynUtil.getProperty(FormEditor.this.instance, subType.typeName);
      }
      return subInst;
   }

   IElementEditor createElementEditor(Object elem, int ix, IElementEditor oldTag) {
      Object propInst;
      Object propType;
      BodyTypeDeclaration innerType = null;

      Object prop = null;
      // A property
      if (ModelUtil.isProperty(elem)) {
         prop = elem;
         propType = ModelUtil.getPropertyType(elem);
         propInst = parentView.instanceMode ? ElementEditor.getPropertyValue(elem, FormEditor.this.instance, 0) : null;
      }
      // An inner type
      else if (elem instanceof BodyTypeDeclaration) {
         innerType = (BodyTypeDeclaration) elem;
         propInst = getInnerTypeInstance(innerType);
         propType = innerType;
      }
      else {
         propInst = null;
         propType = null;
      }

      String editorType = getEditorType(elem, prop, propType, propInst, instanceMode);
      Object editorClass = getEditorClass(editorType);

      Object oldClass = oldTag != null ? DynUtil.getType(oldTag) : null;
      if (oldClass == editorClass) {
         ((IElementEditor) oldTag).updateEditor(elem, prop, propType, propInst);
         return oldTag;
      }
      else {
         IElementEditor newEditor = (IElementEditor) DynUtil.newInnerInstance(editorClass, null, null, parentView, FormEditor.this, prop, propType, propInst);
         newEditor.setRepeatVar(elem);
         newEditor.setRepeatIndex(ix);
         return newEditor;
      }
   }
}
