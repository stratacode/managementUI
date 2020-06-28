

class DataView extends FormView {
   instanceMode := editorModel.selectedInstances != null && editorModel.selectedInstances.size() > 0;

   List<Object> results := editorModel.searchResults;
   results =: refreshResults();
   SearchResultsEditor searchResultsEditor;

   boolean resultsVisible = false;

   void refreshResults() {
      List<Object> res = results;
      if (res == null)
         resultsVisible = false;
      else {
         if (searchResultsEditor != null) {
            searchResultsEditor.countStartIx = editorModel.searchStartIx;
            if (searchResultsEditor.type != editorModel.currentType) {
               searchResultsEditor.updateListEditor(editorModel.currentType, res);
            }
            else {
               searchResultsEditor.updateListInstance(res);
            }
         }
         else {
            searchResultsEditor = new SearchResultsEditor(DataView.this, null, editorModel.currentType, res, true);
            searchResultsEditor.countStartIx = editorModel.searchStartIx;
         }
         resultsVisible = true;
      }
   }

   void updateFormProperties() {
      super.updateFormProperties();
      if (searchResultsEditor != null)
         searchResultsEditor.updateProperties();
   }
}
