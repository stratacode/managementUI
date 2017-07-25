class ListEditor extends TypeEditor {
   List<Object> instList;
   List<Object> oldInstList;
   ListEditor(FormView view, TypeEditor parentEditor, Object parentProperty, BodyTypeDeclaration type, List<Object> insts) {
      super(view, parentEditor, parentProperty, type, insts);
      instList = insts;
   }

   @sc.obj.ManualGetSet
   void updateEditor(Object elem, Object prop, Object propType, Object inst) {
      setTypeNoChange(prop, (BodyTypeDeclaration) elem);
      this.instList = (List<Object>)inst;
      // Notify any bindings on 'instance' that the value is changed but don't validate those bindings before we've refreshed the children.
      //Bind.sendInvalidate(this, "instList", inst);
      // This both invalidates and validates for type
      Bind.sendChange(this, "type", elem);
      // refreshChildren();

      // Now we're ready to validate the bindings on the instance
      //Bind.sendValidate(this, "instList", inst);
   }
}
