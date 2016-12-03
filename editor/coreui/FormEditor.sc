class FormEditor extends TypeEditor {
   Object instance;
   Object oldInstance;
   FormView parentFormView;

   instance =: instanceChanged();

   FormEditor(FormView view, TypeEditor parentEditor, BodyTypeDeclaration type, Object instance) {
      super(view, parentEditor, type, instance);
      parentFormView = view;

      this.instance = instance;
   }

   void init() {
      // Bindings not set before we set the instance property so need to do this once up front
      super.init();
      instanceChanged();
   }

   void instanceChanged() {
      if (removed)
          return;
      if (instance != oldInstance) {
         updateListeners();
         updateChildInsts();
         oldInstance = instance;
      }
   }

   void updateChildInsts() {
      for (IElementEditor view:childViews) {
        if (view instanceof TypeEditor) {
           TypeEditor te = ((TypeEditor) view);
           te.parentInstanceChanged(instance);
        }
      }
   }

   void parentInstanceChanged(Object parentInst) {
       if (parentInst == null)
          instance = null;
       else {
          instance = DynUtil.getPropertyPath(parentInst, CTypeUtil.getClassName(ModelUtil.getInnerTypeName(type)));
       }
   }

   Object getInnerTypeInstance(BodyTypeDeclaration subType) {
      Object subInst = null;
      if (FormEditor.this.instance != null && DynUtil.isObjectType(subType)) {
         subInst = DynUtil.getProperty(FormEditor.this.instance, subType.typeName);
      }
      return subInst;
   }

   IElementEditor createChildElementEditor(Object prop, int ix) {
      IElementEditor res = null;
      if (prop instanceof BodyTypeDeclaration) {
         BodyTypeDeclaration subType = (BodyTypeDeclaration) prop;
         Object subInst = getInnerTypeInstance(subType);
         FormEditor editor = new FormEditor(parentFormView, FormEditor.this, subType, subInst);
         res = editor;
      }
      else if (ModelUtil.isProperty(prop)) {
         TextFieldEditor elemView = new TextFieldEditor(FormEditor.this, prop);
         res = elemView;
      }
      return res;
   }

}
