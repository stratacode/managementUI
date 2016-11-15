BaseView extends JScrollPane implements EditorPanelStyle {
   int numCols = 1;
   int nestWidth = 10;
   int tabSize = 140;

   /** Current selected widget (if any) */
   JTextField currentTextField;

   visible :=: viewVisible;

   viewportView = contentPanel;

   void invalidateModel() {
      contentPanel.invalidateForm();
   }

   object contentPanel extends JPanel {
      void invalidateForm() {
      }
   }

}
