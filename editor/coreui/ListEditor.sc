class ListEditor extends TypeEditor {
   List<Object> listValues;
   ListEditor(BaseView view, TypeEditor parentEditor, BodyTypeDeclaration type, Object parentProperty, List<Object> values) {
      super(view, parentEditor, parentProperty, type);
      listValues = values;
   }
}
