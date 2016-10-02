
@Component
class TypeEditor extends CompositeEditor {
   BaseView parentView;
   TypeEditor parentEditor;

   boolean removed = false;

   BodyTypeDeclaration type;

   EditorModel editorModel = parentView.editorModel;
   Layer classViewLayer = editorModel.currentLayer;  // Gets set to the current layer when we are created

   Object[] properties;

   String operatorName;
   String extTypeName;

   List<IElementEditor> childViews;

   String title;

   int nestLevel = 0;

   type =: typeChanged();

   TypeEditor(BaseView view, TypeEditor parentEditor, BodyTypeDeclaration type, Object instance) {
      parentView = view;
      this.parentEditor = parentEditor;
      this.type = type;
      if (parentEditor != null)
         this.nestLevel = parentEditor.nestLevel + 1;
   }

   void typeChanged() {
      if (type == null)
         operatorName = null;
      else if (ModelUtil.isEnumType(type))
         operatorName = "enum";
      else if (ModelUtil.isEnum(type))
         operatorName = "enum constant";
      else if (ModelUtil.isInterface(type))
         operatorName = "interface";
      else if (ModelUtil.isObjectType(type))
         operatorName = "object";
      else if (ModelUtil.isLayerType(type))
         operatorName = "layer";
      else
         operatorName = "class";

      if (type != null) {
         extTypeName = type.extendsTypeName;
         title = operatorName + " " + ModelUtil.getClassName(type) + (extTypeName == null ? "" : " extends " + CTypeUtil.getClassName(extTypeName));
         properties = editorModel.getPropertiesForType(type);
      }
      else {
         extTypeName = null;
         properties = null;
      }
   }

   void removeListeners() {
      for (IElementEditor view:childViews)
         view.removeListeners();
      childViews.clear();
   }

   void updateListeners() {
      for (IElementEditor view:childViews)
         view.updateListeners();
   }

   void stop() {
       removeListeners();
   }

   // Called when the parent is a FormView representing a specific instance.  If we are storing a child instance
   // we can update our instance to the correct child.
   void parentInstanceChanged(Object parentInst) {
   }

}
