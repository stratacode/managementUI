CreateInstance {
   propertyTypeName :=: propertyTypeField.text;
   lastComponent = propertyTypeField;
   row2y = 5; // determines where confirmButtons go since this widget has no row2

   JComponent startComponent := editorModel.pendingCreate ? createPanel.createLabel : followComponent;

   // Property mode only-----------------------
   object propertyOfTypeLabel extends JLabel {
      text = "of type";
      location := SwingUtil.point(startComponent.location.x + startComponent.size.width + xpad, ypad + baseline);
      size := preferredSize;
   }

   int propertyStart := (int) (propertyOfTypeLabel.location.x + propertyOfTypeLabel.size.width + xpad);

   double propertyFieldRatio = 0.3;

   object propertyTypeField extends CompletionTextField {
      location := SwingUtil.point(propertyStart, ypad);
      size := SwingUtil.dimension(propertyFieldRatio * (createPanel.size.width - propertyStart - xpad), preferredSize.height);

      text =: validateType();

      userEnteredCount =: createInstance();

      completionProvider {
         ctx := createPanel.editorModel.ctx;
         completionType := CompletionTypes.CreateInstanceType;
      }

      void validateType() {
         String err = createPanel.editorModel.validateTypeText(text, true);
         if (err == null)
            displayNameError("");
         else
            displayNameError(err);
      }
   }

   void displayNameError(String error) {
      createPanel.displayCreateError(error);
   }

   void requestFocus() {
   }
}