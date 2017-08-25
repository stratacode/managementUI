HeaderCellEditor {
   int headerHeight := ((RowEditor)formEditor).headerHeight;
   object headerLabel extends JLabel {
      location := SwingUtil.point(HeaderCellEditor.this.x, HeaderCellEditor.this.y - headerHeight);
      size := SwingUtil.dimension(HeaderCellEditor.this.width, headerHeight);

      foreground := GlobalResources.normalTextColor;
      background := GlobalResources.headerBkgColor;

      text := propertyName;

      icon := GlobalResources.lookupIcon(propC);

      toolTipText := propC instanceof CustomProperty ? ((CustomProperty) propC).name : "Property of type: " + propertyTypeName + (propC instanceof PropertyAssignment ? " set " : " defined ") + "in: " + ModelUtil.getEnclosingType(propC);
   }
}