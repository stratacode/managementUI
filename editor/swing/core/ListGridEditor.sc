ListGridEditor {
   numRows := (DynUtil.getArrayLength(visList) + numCols-1) / numCols;

   object objectLabel extends JLabel {
      location := SwingUtil.point(2*xpad + borderSize + titleBorderX, borderTitleY + baseline);
      icon := getSwingIcon(ListGridEditor.this.icon);
      size := preferredSize;
      text := operatorName;
      border = BorderFactory.createEmptyBorder();
   }

   object nameButton extends LinkButton {
      location := SwingUtil.point(objectLabel.location.x + objectLabel.size.width + xpad, borderTitleY + baseline);
      size := preferredSize;
      text := displayName;
      clickCount =: parentProperty == null ? editorModel.changeCurrentType(type, instance, null) : parentView.focusChanged(null, parentProperty, instance, true);
      defaultColor := parentProperty != null && editorModel.currentProperty == parentProperty ? GlobalResources.highlightColor : (transparentType ? GlobalResources.transparentTextColor : GlobalResources.normalTextColor);
   }

   object componentLabel extends LinkButton {
      location := SwingUtil.point(nameButton.location.x + nameButton.size.width + xpad, borderTitleY + baseline);
      size := preferredSize;
      visible := componentTypeName != null;
      clickCount =: gotoComponentType();
      text := componentTypeName;
   }

}