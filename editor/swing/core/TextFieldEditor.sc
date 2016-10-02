// view types: text field view {int, string}, enum, sub-object, list/array
class TextFieldEditor extends LabeledEditor {
   // Want value set in the constructor both so it is not null and does not send an extra set of change events during initialization
   TextFieldEditor(Object prop) {
      propC = prop;
   }

   void displayFormError(String msg) {
      int ix = msg.indexOf(" - ");
      if (ix != -1)
         msg = msg.substring(ix + 3);
         errorLabel.text = msg;
      }

      //Color saveHighlightColor;
      void setElementValue(Object type, Object inst, Object val, String text) {
         // We misfire during init because this method is on an object which sends change events
         if (type == null || val == null)
            return;
         try {
            //if (saveHighlightColor != null)
            //   textField.highlight = saveHighlightColor;
            if (type instanceof ClientTypeDeclaration)
               type = ((ClientTypeDeclaration) type).getOriginal();

            errorLabel.text = "";
            disableFormRebuild = true;
            String error = editorModel.setElementValue(type, inst, val, text, updateInstances, inst == null);
            // Refetch the member since we may have just defined one for this type
            propC = ModelUtil.definesMember(type, ModelUtil.getPropertyName(val), JavaSemanticNode.MemberType.PropertyAnySet, null, null);
            // Update this manually since we change it when moving the operator into the label
            propertyName = getPropertyNameString(propC);
            propertyOperator = propertyOperator(instance, propC);
            textField.enteredText = propertyValueString(instance, propC, changeCt);
            if (error != null)
               displayFormError(error);
         }
         catch (RuntimeException exc) {
            //saveHighlightColor = textField.highlight;
            displayFormError(exc.getMessage());
         }
         finally {
            disableFormRebuild = false;
         }
      }

      String propertyValueString(Object instance, Object val, int changeCt) {
         return editorModel.ctx.propertyValueString(type, instance, val);
      }

      propertyName =: updateListeners();
      Object oldListenerInstance = null;
      JavaModel oldListenerModel = null;
      String oldPropName = null;

      void removeListeners() {
          removed = true;
          instance = null;
          updateListeners();

          DynUtil.dispose(this);
       }

      void updateListeners() {
         String propName = propertyName;
         String simpleProp;
         int ix = propName.indexOf("[");
         if (ix == -1)
            simpleProp = propName;
         else
            simpleProp = propName.substring(0, ix);
         JavaModel javaModel = instance == null ? ModelUtil.getJavaModel(ClassView.this.type) : null;
         if (oldListenerInstance == instance && propName == oldPropName && oldListenerModel == javaModel)
            return;

         if (oldPropName != null && !oldPropName.equals("<null>")) {
            if (oldListenerInstance != null) {
               Bind.removeDynamicListener(oldListenerInstance, type, simpleProp, valueEventListener, IListener.VALUE_CHANGED);
               oldListenerInstance = null;
            }
            else if (oldListenerModel != null) {
               Bind.removeListener(oldListenerModel, null, valueEventListener, IListener.VALUE_CHANGED);
               oldListenerModel = null;
            }
         }

         if (propName != null && !propName.equals("<null>")) {
            if (instance != null) {
               Bind.addDynamicListener(instance, type, simpleProp, valueEventListener, IListener.VALUE_CHANGED);
               oldListenerInstance = instance;
            }
            else if (javaModel != null) {
               Bind.addListener(javaModel, null, valueEventListener, IListener.VALUE_CHANGED);
               oldListenerModel = javaModel;
            }
         }
         oldPropName = propName;
      }


      object textField extends CompletionTextField {
         location := SwingUtil.point(label.location.x + label.size.width + xpad, TextFieldView.this.y);
         size := SwingUtil.dimension(TextFieldView.this.width - label.size.width - label.location.x - 2*xpad-borderSize, preferredSize.height);

         completionProvider {
            ctx := editorModel.ctx;
         }

         // If we have a fromString converter registered, we can edit this guy in value mode
         boolean settable := instance == null || RTypeUtil.canConvertTypeFromString(ModelUtil.getPropertyType(propC));
         foreground := settable ? ComponentStyle.defaultForeground : SwingUtil.averageColors(ComponentStyle.defaultForeground, ComponentStyle.defaultBackground);

         // instance could be retrieved through type hierarchy but we need to update the binding when the instance changes
         enteredText := propertyValueString(instance, propC, changeCt);

         // Only trigger the change to the model when the user enters the text.  Not when we set enteredText because the value changed
         userEnteredCount =: settable ? setElementValue(type, ClassView.this.instance, propC, enteredText) : errorLabel.text = "This view shows the toString output of property: " + propertyName + " No string conversion for type: " ;

         focus =: focusChanged(this, propC, ClassView.this.instance, focus);
      }
      object errorLabel extends JLabel {
         location := SwingUtil.point(textField.location.x, textField.location.y + textField.size.height + ypad);
         size := preferredSize;
         foreground := GlobalResources.errorTextColor;
      }
      height := (int) (errorLabel.size.height + textField.size.height + ypad);
   }
