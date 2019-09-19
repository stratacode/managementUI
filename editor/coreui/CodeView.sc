import sc.dyn.DynUtil;
import java.util.Collection;

@sc.obj.Sync(syncMode = sc.obj.SyncMode.Automatic)
class CodeView extends BaseView {
   int maxEditorHeight = 251, minEditorHeight = 150;
   int numEditors;

   abstract List<CodeEditor> getEditors();

   boolean editorsValid = true; // start out true so the first invalidate triggers the refresh

   boolean editorHasChanges := editorModel.ctx.hasAnyMemoryEditSession(editorModel.ctx.memorySessionChanged);

   // Using the non-bindable java version of LinkedHashMap here because EditorContext can't depend on the util layer. It's also ok to serialize the whole thing, and not try to listen for add/remove events on the map
   java.util.LinkedHashMap<SrcEntry,List<ModelError>> errorModels := editorModel.ctx.errorModels;
   errorModels =: updateCodeErrors();

   void invalidateModel() {
      if (editorsValid) {
         if (editorModel == null || editorModel.selectedFileIndex == null)
            return;

         List<CodeEditor> editors = getEditors();

         Collection<EditorModel.SelectedFile> col = editorModel.selectedFileList;
         int sz = col.size();
         int edSz = editors == null ? 0 : editors.size();
         if (sz == edSz) {
            int ct = 0;
            // Go through the list of selected types - if only inner types have changed, do not rebuild the editors - just
            // update the caret positions... and update the file property of each editor to point to the new SelectedFile.
            // We do need the new types to compute the new caret position
            for (EditorModel.SelectedFile selFile:col) {
               CodeEditor ed = editors.get(ct);
               if (!selFile.file.absFileName.equals(ed.file.file.absFileName))
                  break;
               ct++;
            }
            if (ct == sz) {
               ct = 0;
               for (EditorModel.SelectedFile selFile:col) {
                  CodeEditor ed = editors.get(ct);
                  ct++;
                  ed.file = selFile;
               }
               updateCaretPositions();
               return;
            }
         }

         editorsValid = false;

         scheduleRebuild();
      }
   }

   void updateCodeErrors() {
      List<CodeEditor> editors = getEditors();
      if (editors != null) {
         for (CodeEditor editor:editors) {
            editor.modelErrors = errorModels.get(editor.file.file);
         }
      }
   }

   abstract void scheduleRebuild();

   abstract void updateCaretPositions();

   @sc.obj.Sync(syncMode = sc.obj.SyncMode.Automatic)
   class CodeEditor {
      //@sc.obj.Sync(syncMode=sc.obj.SyncMode.Disabled)
      EditorModel.SelectedFile file;

      // TODO: change to errorText.size() now that it works in a binding
      int errorsHeight := TextUtil.length(errorText) == 0 ? 0 : 50;
      String errorText;

      List<sc.lang.ModelError> modelErrors;

      file =: file == null ? null : modelErrors = (errorModels == null ? null : errorModels.get(file.file));

      // TODO: with transparent layers, we need to have a way to indicate it in the editor and copy the file based on a user decision to customize it in this layer.
      boolean transparentType := file.layer != file.modelLayer;

      CodeEditor() {}

      CodeEditor(EditorModel.SelectedFile f) {
         if (f == null)
            System.err.println("*** No file in code editor");
         file = f;
      }

      object fileLabel {
         String fileText := file.file + "";
      }

      object editPanel extends ModelEditor {
         ctx := editorModel.ctx;
         file := CodeEditor.this.file;
      }

      /*
      int[] lineNumbers;
      */

      int[] typeOffsets := editorModel.getTypeOffsets(file);

      int fileHeight;

      int defaultHeight;

      void removeEditor() {
         DynUtil.dispose(this);
      }

   }
}
