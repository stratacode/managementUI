CreateLayer {
   layerMode :=: (String)layerModeChoice.selectedItem;
   newLayerName :=: addLayerField.text;
   newLayerPackage :=: packageTextField.text;
   newLayerExtends :=: objExtendsTypeField.text;

   isPublic :=: publicCheck.selected;
   isDynamic :=: dynamicCheck.selected;
   isTransparent :=: transparentCheck.selected;

   row2y := (int)(2*ypad + layerModeChoice.size.height + createPanel.row1ErrorHeight);

   lastComponent := addLayerMode ? addLayerField : objExtendsTypeField;

   object layerModeChoice extends JComboBox {
      items := {"Include", "Create"};
      location := SwingUtil.point(followComponent.location.x + followComponent.size.width + xpad, ypad);
      size := preferredSize;
   }

   object dynamicCheck extends JCheckBox {
      text = "Dynamic";
      location := SwingUtil.point(layerModeChoice.location.x + layerModeChoice.size.width + xpad, ypad);
      size := preferredSize;
      selected = true;
   }

   object publicCheck extends JCheckBox {
      text = "Public";
      location := SwingUtil.point(dynamicCheck.location.x + dynamicCheck.size.width + xpad, ypad);
      size := preferredSize;
      visible := !addLayerMode;
      selected = true;
   }

   object transparentCheck extends JCheckBox {
      text = "Transparent";
      location := SwingUtil.point(publicCheck.location.x + publicCheck.size.width + xpad, ypad);
      size := preferredSize;
      visible := !addLayerMode;
      selected = false;
   }

   object packageLabel extends JLabel {
      text = "Package";
      location := SwingUtil.point(transparentCheck.location.x + transparentCheck.size.width + xpad, ypad + baseline);
      size := preferredSize;
      visible := !addLayerMode;
   }

   object packageTextField extends JTextField {
      location := SwingUtil.point(packageLabel.location.x + packageLabel.size.width + xpad, ypad);
      size := SwingUtil.dimension(250, preferredSize.height);
      visible := !addLayerMode;
   }

   object nameLabel extends JLabel {
      text = "Layer Name";
      location := SwingUtil.point(xpad, row2y + baseline);
      size := preferredSize;
   }

   int layerNameWidth := addLayerMode ? 0 : (int) (objExtendsTypeField.size.width + 3*xpad + objExtendsLabel.size.width);

   object addLayerField extends CompletionTextField {
      location := SwingUtil.point(nameLabel.location.x + nameLabel.size.width + xpad, row2y);
      size := SwingUtil.dimension(createPanel.size.width * nameFieldRatio, preferredSize.height);

      text =: validateName(text);

      userEnteredCount =: doSubmit();

      completionProvider {
         ctx := editorModel.ctx;
         completionType = CompletionTypes.ExistingLayer;
      }
   }

   object objExtendsLabel extends JLabel {
      text = "extends";
      visible := !addLayerMode;
      location := SwingUtil.point(addLayerField.location.x + addLayerField.size.width + xpad, row2y + baseline);
      size := preferredSize;
   }

   object objExtendsTypeField extends CompletionTextField {
      visible := !addLayerMode;
      location := SwingUtil.point(objExtendsLabel.location.x + objExtendsLabel.size.width + xpad, row2y);
      size := SwingUtil.dimension(createPanel.size.width * nameFieldRatio, preferredSize.height);

      text =: editorModel.validateTypeText(text, false);

      userEnteredCount =: doSubmit();

      completionProvider {
         ctx := editorModel.ctx;
      }
   }

   void displayNameError(String error) {
      createPanel.displayNameError(error, addLayerField);
   }

   void requestFocus() {
      addLayerField.requestFocus();
   }
}