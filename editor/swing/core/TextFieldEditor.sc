import sc.type.RTypeUtil;

// view types: text field view {int, string}, enum, sub-object, list/array
TextFieldEditor {
   formComponent = textField;

   object textField extends CompletionTextField {
      location := SwingUtil.point(elementLabel.location.x + elementLabel.size.width + xpad, TextFieldEditor.this.y);
      size := SwingUtil.dimension(TextFieldEditor.this.width - elementLabel.size.width - elementLabel.location.x - 2*xpad-formEditor.borderSize, preferredSize.height);

      completionProvider {
         ctx := editorModel.ctx;
      }

      // If we have a fromString converter registered, we can edit this guy in value mode
      boolean settable := formEditor.instance == null || RTypeUtil.canConvertTypeFromString(ModelUtil.getPropertyType(propC));
      foreground := settable ? ComponentStyle.defaultForeground : SwingUtil.averageColors(ComponentStyle.defaultForeground, ComponentStyle.defaultBackground);

      // instance could be retrieved through type hierarchy but we need to update the binding when the instance changes
      enteredText := propertyValueString(formEditor.instance, propC, changeCt);

      editable := TextFieldEditor.this.editable;

      // Only trigger the change to the model when the user enters the text.  Not when we set enteredText because the value changed
      userEnteredCount =: settable ? setElementValue(formEditor.type, formEditor.instance, propC, enteredText) : errorLabel.text = "This view shows the toString output of property: " + propertyName + " No string conversion for type: " ;

      focus =: focusChanged(this, focus);
   }

   void setElementValue(Object type, Object inst, Object val, String text) {
      if (type == null || val == null)
         return;
      try {
         if (type instanceof ClientTypeDeclaration)
            type = ((ClientTypeDeclaration) type).getOriginal();

         super.setElementValue(type, inst, val, text);
         textField.enteredText = propertyValueString(formEditor.instance, propC, changeCt);
      }
      catch (RuntimeException exc) {
         displayFormError(exc.getMessage());
      }
      finally {
      }
   }

   String getElementStringValue() {
      return textField.text;
   }

   String propertyValueString(Object instance, Object val, int changeCt) {
      if (formEditor == null)
         System.err.println("*** Error - no formEditor for propValue");
      if (editorModel == null)
         System.err.println("*** Error - no editor model for propvalue");
      else if (editorModel.ctx == null)
              System.err.println("*** Error - No context for prop value");
      return editorModel.ctx.propertyValueString(formEditor.type, instance, val);
   }

}
