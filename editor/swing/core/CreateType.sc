CreateType {
   innerChoice :=: (String) objInnerChoice.selectedItem;
   newTypeName :=: nameField.text;
   extendsTypeName :=: objExtendsTypeField.text;

   outerTypeName :=: (String) inTypeCombo.selectedItem;

   row2y := (int)(2*ypad + beforeAfter.size.height + createPanel.row1ErrorHeight);

   lastComponent = objExtendsTypeField;

   object objInnerChoice extends JComboBox {
      items := innerChoiceItems;
      location := SwingUtil.point(followComponent.location.x + followComponent.size.width + xpad, ypad);
      size := preferredSize;
   }

   object inTypeCombo extends JComboBox {
   // TODO: should we add another type group for this - extendable types?
      items := editorModel.ctx.getCreateInstTypeNames();
      location := SwingUtil.point(objInnerChoice.location.x + objInnerChoice.size.width + xpad, ypad);
      size := preferredSize;
      visible := innerType;
   }

   object beforeAfter extends JComboBox {
      items = {"after", "before"};
      location := SwingUtil.point(inTypeCombo.location.x + inTypeCombo.size.width + xpad, ypad);
      size := preferredSize;
      visible := innerType;
   }

   object beforeAfterLabel extends JLabel {
      location := SwingUtil.point(beforeAfter.location.x + beforeAfter.size.width + xpad, ypad + baseline);
      size := preferredSize;
      text := beforeAfterText;
      visible := innerType;
   }

   JComponent beforeInLabel := innerType ? beforeAfterLabel : objInnerChoice;

   object inLabel extends JLabel {
      location := SwingUtil.point(beforeInLabel.location.x + beforeInLabel.size.width + xpad, ypad + baseline);
      size := preferredSize;
      text = "in layer";
   }

   object inLayerCombo extends JComboBox {
      location := SwingUtil.point(inLabel.location.x + inLabel.size.width + xpad, ypad);
      size := preferredSize;
      items := matchingLayerNames;
      selectedIndex := currentLayerIndex;
   }

   object nameLabel extends JLabel {
      text = "Name";
      location := SwingUtil.point(xpad, row2y + baseline);
      size := preferredSize;
   }
   object nameField extends JTextField {
      location := SwingUtil.point(nameLabel.location.x + nameLabel.size.width + xpad, row2y);
      size := SwingUtil.dimension(nameFieldRatio * createPanel.size.width, preferredSize.height);

      userEnteredCount =: doSubmit();
   }

   object objExtendsLabel extends JLabel {
      text = "extends";
      location := SwingUtil.point(nameField.location.x + nameField.size.width + xpad, row2y + baseline);
      size := preferredSize;
   }

   object objExtendsTypeField extends CompletionTextField {
      location := SwingUtil.point(objExtendsLabel.location.x + objExtendsLabel.size.width + xpad, row2y);
      size := SwingUtil.dimension(createPanel.size.width * nameFieldRatio, preferredSize.height);

      userEnteredCount =: doSubmit();

      completionProvider {
         ctx := editorModel.ctx;
      }
   }

   void displayNameError(String error) {
      createPanel.displayNameError(error, nameField);
   }

   void requestFocus() {
      nameField.requestFocus();
   }
}