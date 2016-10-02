abstract class LabeledEditor extends ElementEditor {
  object label extends JLabel {
     int prefW := ((int) preferredSize.width);
     int labelExtra := prefW >= tabSize ? 0 : (tabSize - prefW % tabSize);
     int labelWidth := prefW + labelExtra;
     location := SwingUtil.point(LabeledEditor.this.x+xpad+labelExtra, LabeledEditor.this.y + baseline);
     size := preferredSize;

     foreground := propertyInherited ? GlobalResources.transparentTextColor : GlobalResources.normalTextColor;

     text := propertyName + propertyOperator;

     icon := GlobalResources.lookupIcon(propC);

     toolTipText := "Property of type: " + ModelUtil.getPropertyType(propC) + (propC instanceof PropertyAssignment ? " set " : " defined ") + "in: " + ModelUtil.getEnclosingType(propC);
  }
  height := (int) label.size.height;
}

