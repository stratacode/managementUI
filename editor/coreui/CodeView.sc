import sc.dyn.DynUtil;
import java.util.Collection;

@sc.obj.Sync(syncMode = sc.obj.SyncMode.Automatic)
class CodeView {
   EditorModel editorModel;
   int maxEditorHeight = 251, minEditorHeight = 150;
   int numEditors;

   @sc.obj.Sync
   boolean viewVisible;

   List<CodeEditor> editors = new ArrayList<CodeEditor>();

   editorModel =: invalidateEditors();
   viewVisible =: invalidateEditors();

   boolean editorsValid = true; // start out true so the first invalidate triggers the refresh

   void invalidateEditors() {
      if (editorsValid) {
         if (editorModel == null || editorModel.selectedFileIndex == null)
            return;

         Collection<EditorModel.SelectedFile> col = editorModel.selectedFileList;
         int sz = col.size();
         if (sz == editors.size()) {
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

   abstract void scheduleRebuild();

   abstract void updateCaretPositions();

   @sc.obj.Sync(syncMode = sc.obj.SyncMode.Automatic)
   class CodeEditor {
      @sc.obj.Sync(syncMode=sc.obj.SyncMode.Disabled)
      EditorModel.SelectedFile file;

      // Using TextUtil.length works around a bug in data binding.  It's not listening on the object for the .length() object.
      int errorsHeight := TextUtil.length(errorText) == 0 ? 0 : 50;
      String errorText;

      // TODO: with transparent layers, we need to have a way to indicate it in the editor and copy the file based on a user decision to customize it in this layer.
      boolean transparentType := file.layer != file.modelLayer;

      CodeEditor() {}

      CodeEditor(EditorModel.SelectedFile f) {
         file = f;
      }

      object fileLabel {
         String fileText := file.file + "";
      }

      object editPanel extends ModelEditor {
         ctx := editorModel.ctx;
         file := CodeEditor.this.file;
      }

      int[] lineNumbers;

      int[] typeOffsets;

      int fileHeight;

      int defaultHeight;

      void removeEditor() {
         DynUtil.dispose(this);
      }
   }
}
