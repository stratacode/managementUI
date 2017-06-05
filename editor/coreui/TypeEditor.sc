
@Component
class TypeEditor extends CompositeEditor {
   BaseView parentView;
   TypeEditor parentEditor;

   int numCols = 1;

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

   // TODO: workaround for the fact that the type property is set in the constructor, before the binding is registered.  We could possibly detect this case - a property set in the constructor - and fire the binding when the binding is defined.
   void init() {
      typeChanged();
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
      else if (ModelUtil.isLayerType(type))
         operatorName = "layer";
      else if (ModelUtil.isObjectType(type))
         operatorName = "object";
      else
         operatorName = "class";

      if (type != null) {
         extTypeName = type.extendsTypeName;
         title = operatorName + " " + ModelUtil.getClassName(type) + (extTypeName == null ? "" : " extends " + CTypeUtil.getClassName(extTypeName));
         Object[] newProps = editorModel.getPropertiesForType(type);
         ArrayList<Object> visProps = new ArrayList<Object>();
         if (newProps != null) {
            for (Object prop:newProps) {
                // TODO: this method is defined in modelImpl which is not accessible to coreui.  Maybe add default implementations to the model class?
                if (editorModel.filteredProperty(type, prop, false))
                    continue;

                if (prop instanceof BodyTypeDeclaration) {
                   prop = editorModel.processVisibleType(prop);
                   if (prop == null)
                        continue;
                }
                else if (ModelUtil.isProperty(prop)) {
                   // If there is a current layer set and this object is not defined in that layer (or merged), skip it.
                   // If we get a def for a prop which is after the specified layer, see if there's a previous definition
                   // we should be using first.
                   if (editorModel.currentLayer != null) {
                      Layer propLayer = ModelUtil.getPropertyLayer(prop);

                      // If this property is not defined or modified in this layer or one which should be merged continue.
                      while (propLayer != null && !editorModel.currentLayerMatches(propLayer)) {
                         Object prevProp = ModelUtil.getPreviousDefinition(prop);
                         if (prevProp == null) {
                            prop = null;
                            break;
                         }
                         prop = prevProp;
                         propLayer = ModelUtil.getPropertyLayer(prop);
                         if (propLayer == null)
                            prop = null;
                      }
                      if (prop == null)
                         continue;
                   }
                }
                else // class or a compiled type we can't handle
                   continue;
                visProps.add(prop);
            }
         }
         properties = visProps.toArray();
      }
      else {
         extTypeName = null;
         properties = null;
      }
   }

   // Sets the field without firing a change event
   @sc.obj.ManualGetSet
   void setTypeNoChange(BodyTypeDeclaration newType) {
      type = newType;
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
      setVisible(false);
   }

   // Called when the parent is a FormView representing a specific instance.  If we are storing a child instance
   // we can update our instance to the correct child.
   void parentInstanceChanged(Object parentInst) {
   }
}
