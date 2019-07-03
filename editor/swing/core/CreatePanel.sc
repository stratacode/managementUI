import sc.lang.java.ModelUtil;
import sc.lang.JavaLanguage;
import sc.util.StringUtil;
import sc.layer.Layer;
import java.util.Arrays;

@CompilerSettings(constructorProperties="editorModel")
CreatePanel extends JPanel implements EditorPanelStyle {
   int row1ErrorHeight := row1DialogError.visible ? (int) (row1DialogError.size.height + ypad) : 0;

   boolean addLayerMode := createSubPanel instanceof CreateLayer && ((CreateLayer) createSubPanel).addLayerMode;
   boolean createLayerMode := createSubPanel instanceof CreateLayer && !addLayerMode;

   object createLabel extends JLabel {
      text := editorModel.pendingCreate ? (createSubPanel.enabled ? "Create instance" : "Provide required fields for instance") :  "Add";
      location := SwingUtil.point(xpad, ypad + baseline);
      size := preferredSize;
   }
   object createTypeChoice extends JComboBox {
      items := java.util.Arrays.asList(editorModel == null || editorModel.currentType == null ?
                                       CreateMode.getNoCurrentTypeNames() : CreateMode.getAllNames());
      location := SwingUtil.point(createLabel.location.x + createLabel.size.width + xpad, ypad);
      size := preferredSize;
      selectedItem =: currentCreateMode = CreateMode.valueOf(selectedItem);
      selectedItem =: clearErrors();
      visible := !editorModel.pendingCreate;
   }

   void displayComponentError(String errorText, Object component) {
      row1DialogError.errorText = errorText;
      row1DialogError.errorComponent = (JComponent) component;
   }

   void displayNameError(String errorText, Object component) {
      row2DialogError.errorText = errorText;
      row2DialogError.errorComponent = (JComponent) component;
   }

   object row1DialogError extends ErrorLabel {
      String errorText;
      text := errorText;
      visible := errorText != null && !StringUtil.isEmpty(errorText);
   }

   object row2DialogError extends ErrorLabel {
      String errorText;
      text := errorText;
      visible := errorText != null && !StringUtil.isEmpty(errorText);
   }

   void clearErrors() {
      displayComponentError("", null);
      displayNameError("", null);
   }

   void clearTextFields() {
      createSubPanel.clearFields();
   }

   void clearForm() {
      clearTextFields();

      createSubPanel.clearForm();

      editorModel.createMode = false;

      displayComponentError("", null);
      row2DialogError.errorText = "";

      opComplete++;
   }

   void displayCreateError(String err) {
      row1DialogError.errorComponent = createLabel;
      row1DialogError.errorText = err;
   }

   object confirmButtons extends ConfirmButtons {
      visible := createSubPanel.needsConfirmButtons;
      enabled := createSubPanel.enabled && StringUtil.isEmpty(row2DialogError.errorText) && StringUtil.isEmpty(row1DialogError.errorText);
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
