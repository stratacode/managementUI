import sc.lang.java.ModelUtil;
import sc.lang.JavaLanguage;
import sc.util.StringUtil;
import sc.layer.Layer;

class CreatePanel extends JPanel implements EditorPanelStyle {
   EditorModel editorModel;

   enum ViewMode {
      Property, Type, Layer
   }

   ViewMode viewMode = ViewMode.Property;

   // Incremented each time an operation finishes
   int opComplete = 0;

   object createLabel extends JLabel {
      text = "Add";
      location := SwingUtil.point(xpad, ypad + baseline);
      size := preferredSize;
   }
   object createTypeChoice extends JComboBox {
      items := java.util.Arrays.asList(editorModel == null || editorModel.currentType == null ? new String[] {"Object", "Class"/*, "Enum"*/, "Layer"} : new String[] {"Property", "Object", "Class" /*, "Array", "Enum" */, "Layer"});
      location := SwingUtil.point(createLabel.location.x + createLabel.size.width + xpad, ypad);
      size := preferredSize;
      selectedItem =: (selectedItem.equals("Property") ? viewMode = ViewMode.Property : (selectedItem.equals("Layer") ? viewMode = ViewMode.Layer : viewMode = ViewMode.Type));
      selectedItem =: clearErrors();
   }

   int beforeOfTypeWidth := (int) (createTypeChoice.location.x + createTypeChoice.size.width + xpad);
   int afterOfTypeWidth := (int) (inLabel.size.width + beforeAfter.size.width + beforeAfterLabel.size.width);

   // Property mode only-----------------------
   object propertyOfTypeLabel extends JLabel {
      text = "of type";
      location := SwingUtil.point(createTypeChoice.location.x + createTypeChoice.size.width + xpad, ypad + baseline);
      size := preferredSize;
      visible := viewMode == ViewMode.Property;
   }

   int propertyStart := (int) (propertyOfTypeLabel.location.x + propertyOfTypeLabel.size.width + xpad);

   double propertyFieldRatio = 0.3;
   object propertyTypeField extends CompletionTextField {
      location := SwingUtil.point(propertyStart, ypad);
      size := SwingUtil.dimension(propertyFieldRatio * (CreatePanel.this.size.width - propertyStart - xpad), preferredSize.height);
      visible := viewMode == ViewMode.Property;

      text =: validateType();

      completionProvider {
         ctx := editorModel.ctx;
      }

      void validateType() {
         if (text.trim().length() == 0) {
            clearError();
            return;
         }
         String err = ModelUtil.validateElement(JavaLanguage.getJavaLanguage().type, text, false);
         if (err != null) {
            row1DialogError.errorField = this;
            row1DialogError.errorText = err;
         }
         else
            clearError();
      }

      void clearError() {
         row1DialogError.errorText = "";
      }
   }
   // End property mode only-----------------------

   // Obj mode only-----------------------
   object objInnerChoice extends JComboBox {
      items := java.util.Arrays.asList(editorModel == null || editorModel.currentType == null || ModelUtil.isLayerType(editorModel.currentType) ? new String[] {"Top level"} : new String[] {"Inside", "Top level"});
      visible := viewMode == ViewMode.Type;
      location := SwingUtil.point(createTypeChoice.location.x + createTypeChoice.size.width + xpad, ypad);
      size := preferredSize;
   }
   // End obj mode only-----------------------

   // Layer mode only-----
   object layerModeChoice extends JComboBox {
      items := {"Include", "Create"};
      visible := viewMode == ViewMode.Layer;
      location := SwingUtil.point(createTypeChoice.location.x + createTypeChoice.size.width + xpad, ypad);
      size := preferredSize;
   }

   object dynamicCheck extends JCheckBox {
      text = "Dynamic";
      location := SwingUtil.point(layerModeChoice.location.x + layerModeChoice.size.width + xpad, ypad);
      size := preferredSize;
      visible := viewMode == ViewMode.Layer;
      selected = true;
   }

   object publicCheck extends JCheckBox {
      text = "Public";
      location := SwingUtil.point(dynamicCheck.location.x + dynamicCheck.size.width + xpad, ypad);
      size := preferredSize;
      visible := viewMode == ViewMode.Layer && StringUtil.equalStrings((String) layerModeChoice.selectedItem, "Create");
      selected = true;
   }

   object transparentCheck extends JCheckBox {
      text = "Transparent";
      location := SwingUtil.point(publicCheck.location.x + publicCheck.size.width + xpad, ypad);
      size := preferredSize;
      visible := viewMode == ViewMode.Layer && StringUtil.equalStrings((String) layerModeChoice.selectedItem, "Create");
      selected = false;
   }

   object packageLabel extends JLabel {
      text = "Package";
      location := SwingUtil.point(transparentCheck.location.x + transparentCheck.size.width + xpad, ypad + baseline);
      size := preferredSize;
      visible := publicCheck.visible;
   }

   object packageTextField extends JTextField {
      location := SwingUtil.point(packageLabel.location.x + packageLabel.size.width + xpad, ypad);
      size := SwingUtil.dimension(250, preferredSize.height);
      visible := publicCheck.visible;
   }

   // end layer mode only-----

   JComponent preInComponent := viewMode == ViewMode.Property ? propertyTypeField : objInnerChoice;

   boolean innerType := !StringUtil.equalStrings((String)objInnerChoice.selectedItem, "Top level") && viewMode != ViewMode.Layer;

   object inLabel extends JLabel {
      location := SwingUtil.point(preInComponent.location.x + preInComponent.size.width + xpad, ypad + baseline);
      size := preferredSize;
      text := String.valueOf(editorModel.currentPropertyType);
      visible := innerType;
   }

   object beforeAfter extends JComboBox {
      items = {"after", "before"};
      location := SwingUtil.point(inLabel.location.x + inLabel.size.width + xpad, ypad);
      size := preferredSize;
      visible := innerType;
   }

   object beforeAfterLabel extends JLabel {
      location := SwingUtil.point(beforeAfter.location.x + beforeAfter.size.width + xpad, ypad + baseline);
      size := preferredSize;
      text := editorModel.currentProperty == null ? "the last property" : ModelUtil.getPropertyName(editorModel.currentProperty);
      visible := innerType;
   }

   object inPackageLabel extends JLabel {
      location := SwingUtil.point(preInComponent.location.x + preInComponent.size.width + xpad, ypad + baseline);
      size := preferredSize;
      text := editorModel.currentPackage == null ? "Select a layer or type to choose where to store the type" : "Package: " + editorModel.currentPackage + (editorModel.currentLayer == null ? "" : ", Layer: " + editorModel.currentLayer.layerName);
      visible := !innerType && viewMode != ViewMode.Layer;
   }

   int row2y := (int)(2*ypad + beforeAfter.size.height + (row1DialogError.visible ? row1DialogError.size.height + ypad : 0));

   double nameFieldRatio = 0.3;

   boolean addLayerMode := visible && viewMode == ViewMode.Layer && StringUtil.equalStrings((String) layerModeChoice.selectedItem, "Include");
   boolean createLayerMode := visible && viewMode == ViewMode.Layer && StringUtil.equalStrings((String) layerModeChoice.selectedItem, "Create");
   boolean layerMode := addLayerMode || createLayerMode;

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

   // Second row
   object nameLabel extends JLabel {
      text := layerMode ? "Layer Name" : "Name";
      location := SwingUtil.point(xpad, row2y + baseline);
      size := preferredSize;
   }
   object nameField extends JTextField {
      location := SwingUtil.point(nameLabel.location.x + nameLabel.size.width + xpad, row2y);
      size := SwingUtil.dimension(nameFieldRatio * CreatePanel.this.size.width, preferredSize.height);

      text =: validateName();

      userEnteredCount =: viewMode == ViewMode.Property ? createProperty() : createLayerOrObject();

      visible := !layerMode;

      void validateName() {
         if (text.trim().length() == 0) {
            clearError();
            return;
         }
         JavaLanguage lang = JavaLanguage.getJavaLanguage();
         String err = ModelUtil.validateElement(lang.identifier, text, false);
         if (err != null) {
             row2DialogError.errorField = this;
             row2DialogError.errorText = err;
         }
         else
            clearError();
      }

      void clearError() {
         row2DialogError.errorText = "";
      }
   }

   void clearErrors() {
      row1DialogError.errorText = "";
      row1DialogError.errorField = null;
      row2DialogError.errorText = "";
      row2DialogError.errorField = null;
   }

   int layerNameWidth := addLayerMode ? 0 : (int) (objExtendsTypeField.size.width + 3*xpad + objExtendsLabel.size.width);

   object addLayerField extends CompletionTextField {
      location := SwingUtil.point(nameLabel.location.x + nameLabel.size.width + xpad, row2y);
      visible := layerMode;
      size := SwingUtil.dimension(CreatePanel.this.size.width - nameLabel.location.x - nameLabel.size.width - layerNameWidth - xpad - confirmButtons.width, preferredSize.height);

      text =: validateName();

      userEnteredCount =: createLayerOrObject();

      completionProvider {
         ctx := editorModel.ctx;
         completionType = CompletionTypes.ExistingLayer;
      }

      void validateName() {
         if (text.trim().length() == 0) {
            row2DialogError.errorText = "";
            return;
         }
         JavaLanguage lang = JavaLanguage.getJavaLanguage();
         String err = ModelUtil.validateElement(lang.qualifiedIdentifier, text, false);
         if (err != null) {
             row2DialogError.errorField = this;
             row2DialogError.errorText = err;
         }
         else
            row2DialogError.errorText = "";
      }
   }

   // Property mode only-----------------------
   object propertyFieldValueEditor extends FieldValueEditor {
      visible := viewMode == ViewMode.Property;
      location := SwingUtil.point((int)(nameField.location.x + nameField.size.width + xpad),  row2y);
      size := SwingUtil.dimension((int)(CreatePanel.this.size.width - (nameField.location.x + nameField.size.width + 2*xpad)), valueField.preferredSize.height);

      valueField {
         userEnteredCount =: confirmButtons.okButton.enabled ? createProperty() : null;
      }

      confirmButtons {
         enabled := !StringUtil.isEmpty(nameField.text) && StringUtil.isEmpty(valueFieldError.text);

         cancelButton {
            clickCount =: clearForm();
         }
         okButton {
            clickCount =: createProperty();
         }
      }
   }
   // End property mode only-----------------------


   // Obj mode only-----------------------
   object objExtendsLabel extends JLabel {
      text = "extends";
      visible := viewMode == ViewMode.Type || createLayerMode;
      location := SwingUtil.point(nameField.location.x + nameField.size.width + xpad, row2y + baseline);
      size := preferredSize;
   }
  
   object objExtendsTypeField extends CompletionTextField {
      visible := viewMode == ViewMode.Type || createLayerMode;
      location := SwingUtil.point(objExtendsLabel.location.x + objExtendsLabel.size.width + xpad, row2y);
      size := SwingUtil.dimension(CreatePanel.this.size.width - (objExtendsLabel.location.x + objExtendsLabel.size.width + 2*xpad + confirmButtons.width), preferredSize.height);

      text =: validateType();

      userEnteredCount =: createLayerOrObject();

      completionProvider {
         ctx := editorModel.ctx;
      }

      void validateType() {
         if (text.trim().length() == 0) {
            clearError();
            return;
         }
         String err = ModelUtil.validateElement(JavaLanguage.getJavaLanguage().type, text, false);
         if (err != null) {
             row2DialogError.errorField = this;
             row2DialogError.errorText = err;
         }
         else
            clearError();
      }

      void clearError() {
         row2DialogError.errorText = "";
      }
   }

   void clearForm() {
      objExtendsTypeField.text = "";
      propertyFieldValueEditor.valueField.text = "";
      nameField.text = "";
      packageTextField.text = "";
      addLayerField.text = "";

      opComplete++;
   }

   void createProperty() {
      String mode = (String) createTypeChoice.selectedItem;
      String name = nameField.text.trim();
      if (mode.equals("Property")) {
         if (editorModel.currentPropertyType == null) {
            row2DialogError.text = "First select a type for create property";
            return;
         }
         String propTypeField = propertyTypeField.text;
         String err = editorModel.ctx.addProperty(editorModel.currentPropertyType, propTypeField, name, (String) propertyFieldValueEditor.opSelector.selectedItem, propertyFieldValueEditor.valueField.text);
         if (err != null) {
            row2DialogError.errorField = nameField;
            row2DialogError.errorText = err;
         }
         else
            clearForm();
      }
      else
         System.err.println("*** wrong mode");

      // After create, go back to the name since we may create multiples of the same thing.
      nameField.requestFocus();
   }

   void createLayerOrObject() {
      if (viewMode == ViewMode.Layer)
         createLayer();
      else
         createObject();
   }

   void createObject() {
      String mode = (String) createTypeChoice.selectedItem;
      String name = nameField.text.trim();
      Object currentType = editorModel.currentType;
      if (mode.equals("Array")) {
         System.out.println("*** array mode not implemented");
      }
      else if (mode.equals("Class") || mode.equals("Object")) {
         String err;
         if (name.length() == 0)
            err = "No name specified for new object";
         else if (innerType)
            err = editorModel.ctx.addInnerType(mode, currentType, name, objExtendsTypeField.text);
         else
            err = editorModel.addTopLevelType(mode, editorModel.currentPackage, editorModel.currentLayer, name, objExtendsTypeField.text);

         if (err instanceof String) {
            row2DialogError.errorField = nameField;
            row2DialogError.errorText = err;
         }
         else
            clearForm();
      }
      else {
         System.out.println("*** mode not implemented");
      }
   }

   void addLayer() {
      try {
         String[] addNames = editorModel.ctx.validateExtends(addLayerField.text);
         boolean isDynamic = dynamicCheck.selected;

         Layer origLastLayer = editorModel.system.lastLayer;

         editorModel.ctx.addLayers(addNames, dynamicCheck.selected, true);

         Layer newLastLayer = editorModel.system.lastLayer;
         if (newLastLayer != origLastLayer) {
            if (newLastLayer != null) {
               editorModel.changeCurrentTypeName(newLastLayer.layerModelTypeName);
               editorModel.createMode = false;
            }
         }

         clearForm();
      }
      catch (IllegalArgumentException exc) {
         row2DialogError.errorField = nameField;
         row2DialogError.errorText = exc.toString();
      }
   }

   void createLayer() {
      if (layerModeChoice.selectedItem.equals("Include"))
         addLayer();
      else {
         try {
            String layerName = editorModel.ctx.validateNewLayerPath(addLayerField.text);
            String pkgIdentifier = packageTextField.text;
            String layerPackage = pkgIdentifier == null || pkgIdentifier.trim().length() == 0 ? "" : editorModel.ctx.validateIdentifier(pkgIdentifier);
            String[] extendsNames = editorModel.ctx.validateExtends(objExtendsTypeField.text);
            boolean isDynamic = dynamicCheck.selected;
            boolean isPublic = publicCheck.selected;
            boolean isTransparent = transparentCheck.selected;

            Layer layer = editorModel.ctx.createLayer(layerName, layerPackage, extendsNames, isDynamic, isPublic, isTransparent, true);

            if (layer != null) {
               editorModel.changeCurrentTypeName(layer.layerModelTypeName);
               editorModel.createMode = false;
            }

            clearForm();
         }
         catch (IllegalArgumentException exc) {
            row2DialogError.errorField = addLayerField;
            row2DialogError.errorText = exc.toString();
         }
      }
   }

   boolean objEnabled := editorModel.currentPackage != null && !StringUtil.isEmpty(nameField.text);
   boolean layerEnabled := !StringUtil.isEmpty(addLayerField.text);

   object confirmButtons extends ConfirmButtons {
      visible := viewMode == ViewMode.Type || viewMode == ViewMode.Layer;
      enabled := ((viewMode == ViewMode.Type && objEnabled) || (viewMode == ViewMode.Layer && layerEnabled)) && StringUtil.isEmpty(row2DialogError.errorText) && StringUtil.isEmpty(row1DialogError.errorText);
      x := (int)(objExtendsTypeField.location.x + objExtendsTypeField.size.width + xpad);
      y := row2y - 3;
      cancelButton {
         clickCount =: clearForm();
      }
      okButton {
         clickCount =: createLayerOrObject();
      }
   }
   // End obj mode only-----------------------

}
