CreateProperty {
   needsConfirmButtons = false;

   propertyName :=: nameField.text;
   propertyTypeName :=: propertyTypeField.text;
   ownerTypeName :=: (String) inTypeCombo.selectedItem;
   operator :=: (String) propertyFieldValueEditor.opSelector.selectedItem;
   propertyValue :=: propertyFieldValueEditor.valueField.text;

   addBefore := StringUtil.equalStrings((String)beforeAfter.selectedItem, "before");
   // Do we need this direction?
   //addBefore =: addBefore ? (beforeAfter.selectedItem = "before"): (beforeAfter.selectedItem = "after");

   relPropertyName := editorModel.currentPropertyName;

   object propertyOfTypeLabel extends JLabel {
      text = "of type";
      location := SwingUtil.point(followComponent.location.x + followComponent.size.width + xpad, ypad + baseline);
      size := preferredSize;
   }

   int propertyStart := (int) (propertyOfTypeLabel.location.x + propertyOfTypeLabel.size.width + xpad);

   double propertyFieldRatio = 0.3;

   row2y := (int)(2*ypad + createPanel.createTypeChoice.size.height + createPanel.row1ErrorHeight);

   lastComponent = propertyFieldValueEditor.valueField;

   object propertyTypeField extends CompletionTextField {
      location := SwingUtil.point(propertyStart, ypad);
      size := SwingUtil.dimension(propertyFieldRatio * (createPanel.size.width - propertyStart - xpad), preferredSize.height);

      text =: validateType();

      completionProvider {
         ctx := editorModel.ctx;
         completionType = CompletionTypes.ApplicationType;
      }

      void validateType() {
         String err = editorModel.validateTypeText(text, false);
         if (err == null)
            createPanel.displayComponentError("", null);
         else
            createPanel.displayComponentError(err, this);
      }
   }

   object inLabel extends JLabel {
      override @sc.bind.NoBindWarn
      location := SwingUtil.point(propertyTypeField.location.x + propertyTypeField.size.width + xpad, ypad + baseline);
      size := preferredSize;
      text = "to ";
   }

   object inTypeCombo extends JComboBox {
   // TODO: should we add another type group for this - extendable types?
      items := editorModel.ctx.getCreateInstTypeNames();
      location := SwingUtil.point(inLabel.location.x + inLabel.size.width + xpad, ypad);
      size := preferredSize;
      selectedItem =: updateCurrentType((String)selectedItem);
   }

   object beforeAfter extends JComboBox {
      items = {"after", "before"};
      location := SwingUtil.point(inTypeCombo.location.x + inTypeCombo.size.width + xpad, ypad);
      size := preferredSize;
   }

   object beforeAfterLabel extends JLabel {
      location := SwingUtil.point(beforeAfter.location.x + beforeAfter.size.width + xpad, ypad + baseline);
      size := preferredSize;
      text := relPropertyName == null ? "the " + (addBefore ? "first" : "last")  + " property" : relPropertyName;
   }

   object nameLabel extends JLabel {
      text := "Name";
      location := SwingUtil.point(xpad, row2y + baseline);
      size := preferredSize;

      visible := editorModel.currentCreateMode != CreateMode.Instance;
   }

   object nameField extends JTextField {
      location := SwingUtil.point(nameLabel.location.x + nameLabel.size.width + xpad, row2y);
      size := SwingUtil.dimension(nameFieldRatio * createPanel.size.width, preferredSize.height);

      text =: validateName(text);

      userEnteredCount =: createProperty();

      void clearError() {
         displayNameError("");
      }
   }

   object propertyFieldValueEditor extends FieldValueEditor {
      location := SwingUtil.point((int)(nameField.location.x + nameField.size.width + xpad),  row2y);
      size := SwingUtil.dimension((int)(createPanel.size.width - (nameField.location.x + nameField.size.width + 2*xpad)), valueField.preferredSize.height);

      valueField {
         userEnteredCount =: confirmButtons.okButton.enabled ? createProperty() : null;
      }

      confirmButtons {
         enabled := !StringUtil.isEmpty(nameField.text) && StringUtil.isEmpty(valueFieldError.text);

         cancelButton {
            clickCount =: createPanel.clearForm();
         }
         okButton {
            clickCount =: createProperty();
         }
      }
   }

   void clearFields() {
      propertyName = "";
      propertyValue = "";
   }

   void doSubmit() {
      createProperty();
   }

   void createProperty() {
      super.createProperty();

      // After create, go back to the name since we may create multiples of the same thing.
      nameField.requestFocus();
   }

   void displayNameError(String error) {
      createPanel.displayNameError(error, nameField);
   }

   void requestFocus() {
      nameField.requestFocus();
   }

   void updateCurrentType(String typeName) {
      if (editorModel.currentType == null || !StringUtil.equalStrings(editorModel.typeNames[0], typeName))
         editorModel.findCurrentType(typeName);
   }
}