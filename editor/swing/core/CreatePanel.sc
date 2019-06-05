import sc.lang.java.ModelUtil;
import sc.lang.JavaLanguage;
import sc.util.StringUtil;
import sc.layer.Layer;
import java.util.Arrays;

class CreatePanel extends JPanel implements EditorPanelStyle {
   EditorModel editorModel;

   // Incremented each time an operation finishes
   int opComplete = 0;

   CreateMode currentCreateMode :=: editorModel.currentCreateMode;
   boolean createMode := editorModel.createMode;

   createMode =: onModeChange();
   currentCreateMode =: onModeChange();

   object createLabel extends JLabel {
      text := editorModel.pendingCreate ? (instanceEnabled ? "Create instance" : "Provide required fields for instance") :  "Add";
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

   JComponent preOfTypeComponent := editorModel.pendingCreate ? createLabel : createTypeChoice;

   // Property mode only-----------------------
   object propertyOfTypeLabel extends JLabel {
      text = "of type";
      location := SwingUtil.point(preOfTypeComponent.location.x + preOfTypeComponent.size.width + xpad, ypad + baseline);
      size := preferredSize;
      visible := currentCreateMode == CreateMode.Property || instMode;
   }

   int propertyStart := (int) (propertyOfTypeLabel.location.x + propertyOfTypeLabel.size.width + xpad);

   double propertyFieldRatio = 0.3;
   object propertyTypeField extends CompletionTextField {
      location := SwingUtil.point(propertyStart, ypad);
      size := SwingUtil.dimension(propertyFieldRatio * (CreatePanel.this.size.width - propertyStart - xpad), preferredSize.height);
      visible := currentCreateMode == CreateMode.Property || instMode;

      text =: validateType();

      userEnteredCount =: doCreate(); // Maybe only for instMode?

      completionProvider {
         ctx := editorModel.ctx;
         completionType := instMode ? CompletionTypes.CreateInstanceType : CompletionTypes.ApplicationType;
      }

      void validateType() {
         String typeName = text.trim();
         if (typeName.length() == 0) {
            clearError();
            return;
         }
         String err = ModelUtil.validateElement(JavaLanguage.getJavaLanguage().type, text, false);
         if (err != null) {
            row1DialogError.errorComponent = this;
            row1DialogError.errorText = err;
         }
         else {
            if (instMode) {
               if (!editorModel.ctx.isCreateInstType(typeName)) {
                  row1DialogError.errorText = "No type named: " + typeName;
                  row1DialogError.errorComponent = this;
               }
            }


            clearError();
         }
      }

      void clearError() {
         row1DialogError.errorText = "";
      }
   }
   // End property mode only-----------------------

   // Obj mode only-----------------------
   object objInnerChoice extends JComboBox {
      items := java.util.Arrays.asList(editorModel == null || editorModel.currentType == null || ModelUtil.isLayerType(editorModel.currentType) ? new String[] {"Top level"} : new String[] {"Inside", "Top level"});
      visible := typeMode;
      location := SwingUtil.point(createTypeChoice.location.x + createTypeChoice.size.width + xpad, ypad);
      size := preferredSize;
   }
   // End obj mode only-----------------------

   // Layer mode only-----
   object layerModeChoice extends JComboBox {
      items := {"Include", "Create"};
      visible := currentCreateMode == CreateMode.Layer;
      location := SwingUtil.point(createTypeChoice.location.x + createTypeChoice.size.width + xpad, ypad);
      size := preferredSize;
   }

   object dynamicCheck extends JCheckBox {
      text = "Dynamic";
      location := SwingUtil.point(layerModeChoice.location.x + layerModeChoice.size.width + xpad, ypad);
      size := preferredSize;
      visible := currentCreateMode == CreateMode.Layer;
      selected = true;
   }

   object publicCheck extends JCheckBox {
      text = "Public";
      location := SwingUtil.point(dynamicCheck.location.x + dynamicCheck.size.width + xpad, ypad);
      size := preferredSize;
      visible := currentCreateMode == CreateMode.Layer && StringUtil.equalStrings((String) layerModeChoice.selectedItem, "Create");
      selected = true;
   }

   object transparentCheck extends JCheckBox {
      text = "Transparent";
      location := SwingUtil.point(publicCheck.location.x + publicCheck.size.width + xpad, ypad);
      size := preferredSize;
      visible := currentCreateMode == CreateMode.Layer && StringUtil.equalStrings((String) layerModeChoice.selectedItem, "Create");
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

   JComponent preInComponent := currentCreateMode == CreateMode.Property ? propertyTypeField : objInnerChoice;

   boolean innerType := !StringUtil.equalStrings((String)objInnerChoice.selectedItem, "Top level") && currentCreateMode != CreateMode.Layer;

   object inLabel extends JLabel {
      override @sc.bind.NoBindWarn
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
      override @sc.bind.NoBindWarn
      location := SwingUtil.point(preInComponent.location.x + preInComponent.size.width + xpad, ypad + baseline);
      size := preferredSize;
      text := editorModel.currentPackage == null ? "Select a layer or type to choose where to store the new type" : "Package: " + editorModel.currentPackage + (editorModel.currentLayer == null ? "" : ", Layer: " + editorModel.currentLayer.layerName);
      visible := !innerType && currentCreateMode != CreateMode.Layer && currentCreateMode != CreateMode.Instance;
   }

   int row2y := (int)(2*ypad + beforeAfter.size.height + (row1DialogError.visible ? row1DialogError.size.height + ypad : 0));

   double nameFieldRatio = 0.3;

   boolean addLayerMode := visible && currentCreateMode == CreateMode.Layer && StringUtil.equalStrings((String) layerModeChoice.selectedItem, "Include");
   boolean createLayerMode := visible && currentCreateMode == CreateMode.Layer && StringUtil.equalStrings((String) layerModeChoice.selectedItem, "Create");
   boolean layerMode := addLayerMode || createLayerMode;
   boolean instMode := currentCreateMode == CreateMode.Instance;

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

      visible := editorModel.currentCreateMode != CreateMode.Instance;
   }
   object nameField extends JTextField {
      location := SwingUtil.point(nameLabel.location.x + nameLabel.size.width + xpad, row2y);
      size := SwingUtil.dimension(nameFieldRatio * CreatePanel.this.size.width, preferredSize.height);

      text =: validateName();

      userEnteredCount =: doCreate();

      visible := !layerMode && !instMode;

      void validateName() {
         if (text.trim().length() == 0) {
            clearError();
            return;
         }
         JavaLanguage lang = JavaLanguage.getJavaLanguage();
         String err = ModelUtil.validateElement(lang.identifier, text, false);
         if (err != null) {
             row2DialogError.errorComponent = this;
             row2DialogError.errorText = err;
         }
         else
            clearError();
      }

      void clearError() {
         row2DialogError.errorText = "";
      }
   }

   /*
   object instTypeChoice extends JComboBox {
      visible := instMode && !editorModel.pendingCreate;
      location := nameField.location;
      size := preferredSize;
      items := editorModel == null ? Arrays.asList(new String[] {"<no types>"}) : editorModel.ctx.createInstTypeNames;
   }
   */

   void clearErrors() {
      row1DialogError.errorText = "";
      row1DialogError.errorComponent = null;
      row2DialogError.errorText = "";
      row2DialogError.errorComponent = null;
   }

   void onModeChange() {
      switch (currentCreateMode) {
         case Property:
         case Instance:
            if (editorModel.currentType == null) {
               displayCreateError("Select a type for the new " + currentCreateMode.toString().toLowerCase());
            }
            break;
      }
   }

   int layerNameWidth := addLayerMode ? 0 : (int) (objExtendsTypeField.size.width + 3*xpad + objExtendsLabel.size.width);

   object addLayerField extends CompletionTextField {
      location := SwingUtil.point(nameLabel.location.x + nameLabel.size.width + xpad, row2y);
      visible := layerMode;
      size := SwingUtil.dimension(CreatePanel.this.size.width - nameLabel.location.x - nameLabel.size.width - layerNameWidth - xpad - confirmButtons.width, preferredSize.height);

      text =: validateName();

      userEnteredCount =: doCreate();

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
             row2DialogError.errorComponent = this;
             row2DialogError.errorText = err;
         }
         else
            row2DialogError.errorText = "";
      }
   }

   // Property mode only-----------------------
   object propertyFieldValueEditor extends FieldValueEditor {
      visible := currentCreateMode == CreateMode.Property;
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


   // Obj/create layer modes -----------------------
   object objExtendsLabel extends JLabel {
      text = "extends";
      visible := typeMode || createLayerMode;
      location := SwingUtil.point(nameField.location.x + nameField.size.width + xpad, row2y + baseline);
      size := preferredSize;
   }
  
   object objExtendsTypeField extends CompletionTextField {
      visible := typeMode || createLayerMode;
      location := SwingUtil.point(objExtendsLabel.location.x + objExtendsLabel.size.width + xpad, row2y);
      size := SwingUtil.dimension(CreatePanel.this.size.width - (objExtendsLabel.location.x + objExtendsLabel.size.width + 2*xpad + confirmButtons.width), preferredSize.height);

      text =: validateType();

      userEnteredCount =: doCreate();

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
             row2DialogError.errorComponent = this;
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

      if (editorModel.pendingCreate) {
         editorModel.cancelCreate();
      }

      row1DialogError.errorText = "";
      row2DialogError.errorText = "";

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
            row2DialogError.errorComponent = nameField;
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

   void doCreate() {
      switch (currentCreateMode) {
         case Layer:
            createLayer();
            break;
         case Class:
         case Object:
            createObject();
            break;
         case Instance:
            createInstance();
            break;
         case Property:
            createProperty();
            break;
      }
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
            row2DialogError.errorComponent = nameField;
            row2DialogError.errorText = err;
         }
         else
            clearForm();
      }
      else {
         System.out.println("*** mode not implemented");
      }
   }

   void displayCreateError(String err) {
      row1DialogError.errorComponent = createLabel;
      row1DialogError.errorText = err;
   }

   void createInstance() {
      if (!editorModel.pendingCreate) {
         String err = editorModel.createInstance(propertyTypeField.text);
         if (err instanceof String) {
            displayCreateError(err);
         }
         else {
            displayCreateError(pendingCreateError);
            if (pendingCreateError == null)
               clearForm();
         }
      }
      else {
         String err = editorModel.completeCreateInstance(true);
         if (err != null) {
            displayCreateError(err);
         }
         else {
            clearForm();
            editorModel.createMode = false; // Set this back to non-create state
            editorModel.invalidateModel(); // Rebuild it so it says instance instead of new
         }
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
         row2DialogError.errorComponent = nameField;
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
            row2DialogError.errorComponent = addLayerField;
            row2DialogError.errorText = exc.toString();
         }
      }
   }

   boolean objEnabled := editorModel.currentPackage != null && !StringUtil.isEmpty(nameField.text);
   boolean layerEnabled := !StringUtil.isEmpty(addLayerField.text);
   boolean typeMode := currentCreateMode == CreateMode.Class || currentCreateMode == CreateMode.Object;

   String pendingCreateError := editorModel.pendingCreateError;
   pendingCreateError =: displayCreateError(pendingCreateError);
   boolean instanceEnabled := pendingCreateError == null;

   object confirmButtons extends ConfirmButtons {
      visible := typeMode || currentCreateMode == CreateMode.Layer || currentCreateMode == CreateMode.Instance;
      enabled := ((typeMode && objEnabled) ||
                  (currentCreateMode == CreateMode.Layer && layerEnabled) ||
                  (currentCreateMode == CreateMode.Instance && instanceEnabled)) &&
                  StringUtil.isEmpty(row2DialogError.errorText) && StringUtil.isEmpty(row1DialogError.errorText);
      x := (int)(objExtendsTypeField.location.x + objExtendsTypeField.size.width + xpad);
      y := row2y - 3;
      cancelButton {
         clickCount =: clearForm();
      }
      okButton {
         clickCount =: doCreate();
      }
   }
   // End obj mode only-----------------------

}
