BaseView extends JScrollPane implements EditorPanelStyle {
   int numCols = 1;
   int nestWidth = 10;
   int tabSize = 140;

   visible :=: viewVisible;

   viewportView = contentPanel;

   void invalidateModel() {
      contentPanel.invalidateForm();
   }

   object contentPanel extends JPanel {
      void invalidateForm() {
      }
   }

   Object getDefaultCurrentObj(Object type) {
      return editorModel.ctx.getDefaultCurrentObj(type);
   }

   void setDefaultCurrentObj(Object type, Object obj) {
      editorModel.ctx.setDefaultCurrentObj(type, obj);
   }

}
