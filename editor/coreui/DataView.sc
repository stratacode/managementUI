

class DataView extends FormView {
   instanceMode := editorModel.selectedInstances != null && editorModel.selectedInstances.size() > 0;

   List<Object> results := editorModel.searchResults;
   results =: refreshResults();
   ListGridEditor listGridEditor;

   void refreshResults() {
      List<Object> res = results;
      if (res == null)
         visible = false;
      else {
         if (listGridEditor != null) {
            if (listGridEditor.type != editorModel.currentType) {
               listGridEditor.updateListEditor(editorModel.currentType, res);
            }
            else {
               listGridEditor.updateListInstance(res);
            }
         }
         else {
            listGridEditor = new ListGridEditor(DataView.this, null, editorModel.currentType, res);
         }
         visible = true;
      }
   }

   void updateFormProperties() {
      super.updateFormProperties();
      if (listGridEditor != null)
         listGridEditor.updateProperties();
   }
}
