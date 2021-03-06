import sc.type.RTypeUtil;

// view types: text field view {int, string}, enum, sub-object, list/array
TextFieldEditor {
   formComponent = textField;

   object elementLabel extends ElementLabel {}

   object textField extends CompletionTextField {
      location := SwingUtil.point(elementLabel.location.x + elementLabel.size.width + xpad, TextFieldEditor.this.y);
      size := SwingUtil.dimension(TextFieldEditor.this.width - elementLabel.size.width - elementLabel.location.x - 2*xpad-formEditor.borderSize, preferredSize.height);

      commitOnFocusLoss = true;

      completionProvider {
         ctx := editorModel.ctx;
      }

      // If we have a fromString converter registered, we can edit this guy in value mode
      boolean settable := formEditor.instance == null || EditorModel.isSettableFromString(propC, propType);
      foreground := settable ? ComponentStyle.defaultForeground : SwingUtil.averageColors(ComponentStyle.defaultForeground, ComponentStyle.defaultBackground);

      // instance could be retrieved through type hierarchy but we need to update the binding when the instance changes
      enteredText := propertyValueString(formEditor.instance, propC, changeCt);

      editable := TextFieldEditor.this.editable;

      // Only trigger the change to the model when the user enters the text.  Not when we set enteredText because the value changed
      userEnteredCount =: settable ? setElementValue(formEditor.type, formEditor.instance, propC, enteredText) : errorLabel.text = "This view shows the toString output of property: " + propertyName + " No string conversion for type: " ;

      focus =: focusChanged(this, focus);
   }

   // TODO: prop is the same as propC here - can we remove that arg.  It's not an event trigger for sure.
   String setElementValue(Object type, Object inst, Object prop, String text) {
      if (type == null || prop == null)
         return "No type or prop";
      String error = null;
      try {
         if (type instanceof ClientTypeDeclaration)
            type = ((ClientTypeDeclaration) type).getOriginal();

         error = super.setElementValue(type, inst, prop, text);
         // Let the user correct the error text in this case
         if (errorText == null || errorText.length() == 0)
            textField.enteredText = propertyValueString(formEditor.instance, prop, changeCt);
      }
      catch (RuntimeException exc) {
         error = exc.getMessage();
         displayFormError(error);
      }
      finally {
      }
      return error;
   }

   String getElementStringValue() {
      return textField.text;
   }
}
