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

   Object getDefaultCurrentObj(Object type) {
      return editorModel.ctx.getDefaultCurrentObj(type);
   }

   void setDefaultCurrentObj(Object type, Object obj) {
      editorModel.ctx.setDefaultCurrentObj(type, obj);
   }

   void focusChanged(JComponent component, Object prop, Object inst, boolean focus) {
      if (focus) {
         if (editorModel.currentProperty != prop || editorModel.currentInstance != inst) {
            if (component instanceof JTextField)
               currentTextField = (JTextField) component;
            else
               currentTextField = null;

            editorModel.currentProperty = prop;
            editorModel.currentInstance = inst;
         }
      }
      else if (!focus && editorModel.currentProperty == prop) {
         // Switching focus to the status panel should not alter the current property.
         //currentProperty = null;
         //currentTextField = null;
      }
   }
}
