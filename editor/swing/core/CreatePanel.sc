import sc.lang.java.ModelUtil;
import sc.lang.JavaLanguage;
import sc.util.StringUtil;
import sc.layer.Layer;
import java.util.Arrays;

@CompilerSettings(constructorProperties="editorModel")
CreatePanel extends JPanel implements EditorPanelStyle {
   int row1ErrorHeight := row1DialogError.visible ? (int) (row1DialogError.size.height + ypad) : 0;
   createModeName :=: (String)createTypeChoice.selectedItem;

   object createLabel extends JLabel {
      text := createLabelText;
      location := SwingUtil.point(xpad, ypad + baseline);
      size := preferredSize;
   }
   object createTypeChoice extends JComboBox {
      items := createModeNames;
      location := SwingUtil.point(createLabel.location.x + createLabel.size.width + xpad, ypad);
      size := preferredSize;
      visible := !editorModel.pendingCreate;
   }

   void displayComponentError(String errorText, Object component) {
      super.displayComponentError(errorText, component);
      row1DialogError.errorComponent = (JComponent) component;
   }

   void displayNameError(String errorText, Object component) {
      super.displayNameError(errorText, component);
      row2DialogError.errorComponent = (JComponent) component;
   }

   object row1DialogError extends ErrorLabel {
      text := row1ErrorText;
      visible := !StringUtil.isEmpty(row1ErrorText);
   }

   object row2DialogError extends ErrorLabel {
      text := row2ErrorText;
      visible := !StringUtil.isEmpty(row2ErrorText);
   }

   void displayCreateError(String err) {
      if (err == null)
         err = "";
      super.displayCreateError(err);
      row1DialogError.errorComponent = createLabel;
   }

   object confirmButtons extends ConfirmButtons {
      visible := createSubPanel.needsConfirmButtons;
      enabled := confirmButtonsEnabled;
      x := (int)(createSubPanel.lastComponent.location.x + createSubPanel.lastComponent.size.width + xpad);
      y := createSubPanel.row2y - 3;
      cancelButton {
         clickCount =: clearForm();
      }
      okButton {
         clickCount =: createSubPanel.doSubmit();
      }
   }

   void removeSubPanel(CreateSubPanel panel) {
      SwingUtil.removeChild(this, panel);
      super.removeSubPanel(panel);
   }

   void addSubPanel(CreateSubPanel panel) {
      super.addSubPanel(panel);
      int ix = SwingUtil.indexOf(this, createTypeChoice);
      if (ix == -1)
         System.err.println("*** Error - createTypeChoice not found!");
      SwingUtil.addChild(this, panel, ix);
   }

   void modeChangeComplete() {
      revalidate();
      repaint();
   }
}
