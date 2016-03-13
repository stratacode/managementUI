import sc.util.StringUtil;
import sc.lang.TemplateLanguage;
import sc.lang.java.ModelUtil;

class FieldValueEditor extends JPanel implements EditorPanelStyle {

   object opSelector extends JComboBox {
      items = {"=", ":=", "=:", ":=:"};
      location := SwingUtil.point(0, 1);
      size := preferredSize;
   }

   int valueFieldStart := (int) (opSelector.location.x + opSelector.size.width + xpad);

   object valueFieldError extends ErrorLabel {
      errorField = valueField;
      visible := !StringUtil.isEmpty(text);
   }

   object valueField extends CompletionTextField {
      location := SwingUtil.point(valueFieldStart, 0);
      size := SwingUtil.dimension(FieldValueEditor.this.size.width - 3*xpad - opSelector.size.width - confirmButtons.width, preferredSize.height);

      text =: validateField();

      void validateField() {
         if (text.trim().length() == 0) {
            valueFieldError.text = "";
            return;
         }
         String err = ModelUtil.validateElement(TemplateLanguage.getTemplateLanguage().expression, text, false);
         if (err != null) {
             valueFieldError.text = err;
         }
         else
             valueFieldError.text = "";
      }
   }

   object confirmButtons extends ConfirmButtons {
      x := (int) (valueField.location.x + valueField.size.width + xpad);
      y := 0;
   }
}
