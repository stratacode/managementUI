import sc.lang.java.ModelUtil;
import sc.util.StringUtil;

CodeView extends JScrollPane implements EditorPanelStyle {
   viewportView = contentPanel;
   visible :=: viewVisible;

   int codeViewHeight := (int) (contentPanel.size.height - 2*ypad);
   int maxChildWidth := (int) size.width-4*xpad, maxChildHeight;

   boolean removed = false;

   class CodeSplitPane extends JSplitPane {
      orientation = VERTICAL_SPLIT;
      border = null;
   }

   class TopSplitPane extends CodeSplitPane {
      int topSplitHeight := Math.max(maxChildHeight, codeViewHeight);
      size := SwingUtil.dimension(maxChildWidth, topSplitHeight);
   }

   CodeEditor extends JPanel {
      CodeSplitPane parentSplit;

      errorText := editorModel.ctx.getErrors(file.model, editorModel.ctx.errorsChanged);

      size := SwingUtil.dimension(maxChildWidth,
                                  parentSplit.topComponent == this ? parentSplit.dividerPosition :
                                                                     parentSplit.size.height - parentSplit.dividerPosition);

      fileLabel extends JLabel {
         location := SwingUtil.point(xpad, ypad);
         size := preferredSize;
         text := fileText;
      }

      int modelX := xpad;
      int modelY := (int) (fileLabel.location.y + fileLabel.size.height + 2*ypad);
      int modelW := (int) (CodeEditor.this.size.width - 2*ypad);
      int modelH := (int) (CodeEditor.this.size.height - modelY - errorsHeight);

      editPanel {
         location := SwingUtil.point(xpad, modelY);
         size := SwingUtil.dimension(modelW, modelH);
      }

      object errorsTextArea extends JTextArea {
         text := errorText;
         foreground := GlobalResources.errorTextColor;
         location := SwingUtil.point(xpad, editPanel.location.y + editPanel.size.height + ypad);
         size := SwingUtil.dimension(CodeEditor.this.size.width - 2*ypad, errorsHeight);
      }

      /** Retrieve the line numbers for any type we can find in the file */
      int[] getLineNumbers() {
         ArrayList<Integer> arr = new ArrayList<Integer>();

         for (int i = 0; i < file.types.size(); i++)
            arr.add(ModelUtil.getLineNumber(file.types.get(i)));

         int[] res = new int[arr.size()];
         for (int i = 0; i < res.length; i++)
            res[i] = arr.get(i);

         return res;
      }

      int[] getTypeOffsets() {
         ArrayList<Integer> arr = new ArrayList<Integer>();

         for (int i = 0; i < file.types.size(); i++)
            arr.add(ModelUtil.getTypeOffset(file.types.get(i)));

         int[] res = new int[arr.size()];
         for (int i = 0; i < res.length; i++)
            res[i] = arr.get(i);

         return res;
      }

      int getFileHeight() {
         // Get the lines in the file multiplied by the fontHeight plus sep space
         int numLines = sc.util.FileUtil.countLinesInFile(new java.io.File(file.file.absFileName));
         return numLines * (editPanel.fontSize+2);
      }

      int getDefaultHeight() {
         return Math.max(minEditorHeight, Math.min(getFileHeight() + (int)fileLabel.preferredSize.height + ypad, maxEditorHeight));
      }

      void removeEditor() {
         editPanel.removeEditor();
         DynUtil.dispose(this);
      }
   }


   /** A code editor that does not live in the SplitPane.  It has bindings for it's size */
   class SoloCodeEditor extends CodeEditor {
      size := SwingUtil.dimension(CodeView.this.size.width - 2*ypad, CodeView.this.size.height - 2*ypad); // for VIMPanel y was codeViewHeight

      SoloCodeEditor(EditorModel.SelectedFile f) {
         super(f);
      }
   }

   object contentPanel extends JPanel {
      preferredSize := SwingUtil.dimension(maxChildWidth, maxChildHeight);

      // Children added dynamically in rebuildEditors
   }

   private final static int CODE_NUM_STATIC_COMPONENTS = 0;

   void scheduleRebuild() {
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            rebuildEditors();
         }});
   }

   void shutdown() {
      // First stop the editors.  Do this before messing with the UI so they can shutdown cleanly
      int sz = editors.size();
      for (int i = sz-1; i >= 0; i--) {
         editors.get(i).removeEditor();
      }
      editors.clear();

      for (int i = contentPanel.componentCount-1; i >= CODE_NUM_STATIC_COMPONENTS; i--) {
         contentPanel.remove(i);
      }
   }

   void stop() {
      shutdown();
      removed = true;
   }

   void rebuildEditors() {
      if (editorsValid || removed)
         return;

      editorsValid = true;

      shutdown();

      if (!visible)
         return;

      Collection<EditorModel.SelectedFile> col;
      int sz = (col = editorModel.selectedFileList).size();
      numEditors = sz;
      int maxHeight = 2*ypad;
      if (sz == 1) {
         SoloCodeEditor editor = new SoloCodeEditor(col.iterator().next());
         editors.add(editor);
         maxHeight = codeViewHeight;
         contentPanel.add(editor);
      }
      else if (sz != 0) {
         CodeSplitPane lastSplit = null;
         int i = 0;
         for (EditorModel.SelectedFile selFile:col) {
            CodeEditor editor = new CodeEditor(selFile);
            boolean last = i == sz - 1;
            maxHeight += editor.defaultHeight;
            if (last) {
               editor.parentSplit = lastSplit;
               lastSplit.bottomComponent = editor;
            }
            else {
               // The top level split pane is created on the second to last loop iteration.  It has a binding for
               // the size
               CodeSplitPane split = i == sz - 2 ? new TopSplitPane() : new CodeSplitPane();
               if (lastSplit == null) {
                  split.topComponent = editor;
                  editor.parentSplit = split;
               }
               else {
                  lastSplit.bottomComponent = editor;
                  editor.parentSplit = lastSplit;
                  split.topComponent = lastSplit;
               }
               split.dividerPosition = maxHeight;
               lastSplit = split;
            }
            editors.add(editor);
            i++;
         }
         contentPanel.add(lastSplit);
      }
      maxChildHeight = maxHeight;

      contentPanel.invalidate();
      contentPanel.doLayout();
      invalidate();
      doLayout();
      super.validate();
      repaint();

      // Start the editors after everything is laid out
      for (CodeEditor editor:editors) {
         editor.editPanel.startEditor();
      }

      // First window should get the focus by default
      if (editors.size() > 0)
         editors.get(0).editPanel.requestFocus();

      updateCaretPositions();
   }

   void updateCaretPositions() {
      for (CodeEditor editor:editors) {
         // Set the cursor position to the start of the first selected type in the file
         int[] typeOffsets = editor.getTypeOffsets();
         if (typeOffsets != null && typeOffsets.length > 0 && typeOffsets[0] != -1) {
            editor.editPanel.caretPosition = typeOffsets[0];
            editor.editPanel.centerLineInView();
         }

         editor.editPanel.showCursor(editor.editPanel.hasFocus());
      }
   }

}
