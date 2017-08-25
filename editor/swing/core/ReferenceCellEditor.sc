ReferenceCellEditor {
   object nameButton extends LinkButton {
      location := SwingUtil.point(xpad, ypad + baseline);
      size := SwingUtil.dimension(Math.min(preferredSize.width, cellWidth), preferredSize.height);
      text := referenceId;
      clickCount =: gotoReference();
      foreground := transparentType ? GlobalResources.transparentTextColor : GlobalResources.normalTextColor;
      enabled := referenceable;
   }
   height := cellHeight;
}