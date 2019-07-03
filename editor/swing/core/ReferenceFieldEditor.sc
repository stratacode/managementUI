ReferenceFieldEditor {
   object refLabel extends JLabel {
      int prefW := ((int) preferredSize.width);
      int labelExtra := prefW >= tabSize ? 0 : (tabSize - prefW % tabSize);
      int labelWidth := prefW + labelExtra;
      location := SwingUtil.point(xpad+labelExtra, baseline);
      size := preferredSize;

      foreground := propertyInherited ? GlobalResources.transparentTextColor : GlobalResources.normalTextColor;

      text := displayName;

      icon := GlobalResources.lookupIcon(parentProperty);

      toolTipText := parentProperty instanceof CustomProperty ? ((CustomProperty) parentProperty).name : "Reference of type: " + parentPropertyType + (parentProperty instanceof PropertyAssignment ? " set " : " defined ") + "in: " + ModelUtil.getEnclosingType(parentProperty);
   }

   object nameButton extends LinkButton {
      location := SwingUtil.point(refLabel.location.x + refLabel.size.width + xpad, baseline);
      size := preferredSize;
      text := referenceId;
      clickCount =: gotoReference();
      foreground := transparentType ? GlobalResources.transparentTextColor : GlobalResources.normalTextColor;
      enabled := referenceable;
   }

   height := (int) (refLabel.location.y + refLabel.size.height) + 2 * ypad;

   object instanceList extends JComboBox {
      location := SwingUtil.point(nameButton.location.x + nameButton.size.width + xpad, 0);
      size := preferredSize;
      //items := !visible ? java.util.Collections.emptyList() : instancesOfType;
      items := instancesOfType;
      selectedIndex := getInstSelectedIndex(instance, instancesOfType);
      //userSelectedItem =: userSelectedInstance((InstanceWrapper) selectedItem);
      userSelectedItem =: instance = ((InstanceWrapper) selectedItem).instance;
      userSelectedItem =: editorModel.updateInstanceProperty(parentProperty, propertyName, formEditor.instance, formEditor.wrapper, instance);
   }

}