import sc.lang.java.DeclarationType;

/**
 * Base class for FormEditor and RowEditor - contains the functionality to manage a list of fields in the instance
 */
abstract class InstanceFieldEditor extends InstanceEditor {
   InstanceFieldEditor(FormView view, TypeEditor parentEditor, Object parentProperty, Object type, Object instance, int listIx, InstanceWrapper wrapper) {
      super(view, parentEditor, parentProperty, type, instance, listIx, wrapper);
   }

   boolean hasInnerTypeInstance(BodyTypeDeclaration subType) {
      if (InstanceFieldEditor.this.instance != null && subType.getDeclarationType() == DeclarationType.OBJECT) {
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
         subInst = DynUtil.getProperty(InstanceFieldEditor.this.instance, subType.typeName);
      }
      return subInst;
   }

   IElementEditor createElementEditor(Object elem, int ix, IElementEditor oldTag, String displayMode) {
      Object propInst;
      Object propType;
      BodyTypeDeclaration innerType = null;

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
         propInst = ElementEditor.getPropertyValue(elem, InstanceFieldEditor.this.instance, 0, instanceMode);
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
         IElementEditor newEditor = (IElementEditor) DynUtil.newInnerInstance(editorClass, null, null, parentView, InstanceFieldEditor.this, prop, propType, propInst, ix, null);
         newEditor.setElemToEdit(elem);
         //newEditor.setRepeatIndex(ix);
         return newEditor;
      }
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
