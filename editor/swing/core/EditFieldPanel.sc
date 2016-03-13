import sc.lang.java.ModelUtil;
import sc.util.StringUtil;

class EditFieldPanel extends JPanel implements EditorPanelStyle {
   EditorModel editorModel;

   Object currentProperty := editorModel.currentProperty;
   currentProperty =: fieldValueEditor.opSelector.selectedItem = currentProperty == null ? "=" : ModelUtil.getOperator(currentProperty);

   int icxpad = 2;
   int confirmButtonsWidth = (24 + icxpad) * 2;
   int treeAlignedWidth = 268;

   object currentTypeTextField extends CompletionTextField {
      location := SwingUtil.point(0, ypad);
      size := SwingUtil.dimension(treeAlignedWidth, preferredSize.height);
      completionProvider {
         ctx := editorModel.ctx;
         completionType = CompletionTypes.ApplicationType;
      }

      text := editorModel.currentPropertyType == null ? "" : ModelUtil.getInnerTypeName(editorModel.currentPropertyType);
      userEnteredCount =: changeCurrentType();

      void changeCurrentType() {
         editorModel.findCurrentType(text);
         // TODO: handle errors
      }
   }

   object nameLabel extends JLabel {
      location := SwingUtil.point(xpad + currentTypeTextField.location.x + treeAlignedWidth, ypad + baseline);
      icon := GlobalResources.lookupIcon(currentProperty);
      size := preferredSize;
      text := currentProperty == null ? "<no property selected>" : editorModel.ctx.getImportedPropertyType(currentProperty) + " " + ModelUtil.getPropertyName(currentProperty);
      visible := editorModel.currentProperty != null;
   }

   int nameLabelWidth := (int) nameLabel.size.width;

   object fieldValueEditor extends FieldValueEditor {
      location := SwingUtil.point(nameLabel.location.x + nameLabelWidth + xpad, ypad);
      size := SwingUtil.dimension((EditFieldPanel.this.size.width - nameLabelWidth - treeAlignedWidth - 3*xpad), valueField.preferredSize.height);
      visible := editorModel.currentProperty != null;
      opSelector {
         selectedItem :=: editorModel.currentPropertyOperator;
      }
      valueField {
         text :=: editorModel.currentPropertyValue;
         userEnteredCount =: commitValue();

         // Auto completion support
         completionProvider {
            ctx := editorModel.ctx;
         }
      }
      confirmButtons {
         enabled := editorModel.enableUpdateProperty && StringUtil.isEmpty(valueFieldError.text);

         cancelButton {
            clickCount =: cancelValue();
         }
         okButton {
            clickCount =: commitValue();
         }
      }

      void cancelValue() {
         editorModel.currentPropertyValue = editorModel.savedPropertyValue;
         editorModel.currentPropertyOperator = editorModel.savedPropertyOperator;
         valueFieldError.text = "";
      }

      void commitValue() {
         String error = editorModel.updateCurrentProperty(opSelector.selectedItem, valueField.text);
         if (error != null) {
            int ix = error.indexOf(" - "); // Strips off the File and other crap from a normal error
            if (ix != -1)
               error = error.substring(ix + 3);
            valueFieldError.text = error;
         }
         else
            valueFieldError.text = "";
      }
   }
}
