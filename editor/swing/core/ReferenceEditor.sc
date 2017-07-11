ReferenceEditor {
   boolean propertyInherited := parentProperty != null && ModelUtil.getLayerForMember(null, parentProperty) != editorModel.currentLayer;

   object refLabel extends JLabel {
      int prefW := ((int) preferredSize.width);
      int labelExtra := prefW >= tabSize ? 0 : (tabSize - prefW % tabSize);
      int labelWidth := prefW + labelExtra;
      location := SwingUtil.point(xpad+labelExtra, baseline);
      size := preferredSize;

      foreground := propertyInherited ? GlobalResources.transparentTextColor : GlobalResources.normalTextColor;

      text := displayName;

      icon := GlobalResources.lookupIcon(parentProperty);

      toolTipText := "Reference of type: " + ModelUtil.getPropertyType(parentProperty) + (parentProperty instanceof PropertyAssignment ? " set " : " defined ") + "in: " + ModelUtil.getEnclosingType(parentProperty);
   }

   object nameButton extends LinkButton {
      location := SwingUtil.point(refLabel.location.x + refLabel.size.width + xpad, baseline);
      size := preferredSize;
      text := referenceId;
      clickCount =: editorModel.changeCurrentType(type, instance);
      foreground := transparentType ? GlobalResources.transparentTextColor : GlobalResources.normalTextColor;
   }

   height := (int) (refLabel.location.y + refLabel.size.height) + 2 * ypad;

}