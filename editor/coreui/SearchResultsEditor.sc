class SearchResultsEditor extends ListGridEditor {
   EditorModel editorModel;

   SearchResultsEditor(EditorModel model, FormView view, TypeEditor parentEditor, Object compType, Object instList, boolean instanceEditor) {
      super(view, parentEditor, compType, instList, instanceEditor);
      editorModel = model;
   }

   void sortChanged() {
      if (editorModel != null) {
         editorModel.searchOrderByProps = sortProps == null ? new ArrayList<String>() : new ArrayList<String>(sortProps);
      }
   }

}
