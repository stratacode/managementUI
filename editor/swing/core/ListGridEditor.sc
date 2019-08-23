ListGridEditor {
   int headerHeight = 25;
   numRows := (DynUtil.getArrayLength(visList) + numCols-1) / numCols;

   // The components in the list in the top row of the regular grid will follow the listStart component vertically
   listStart := headerList.lastEditor;

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

   borderTop := (int) (componentLabel.location.y + componentLabel.size.height) + ypad;

   object headerList extends ChildList {
      headerMode = true;

      displayMode = "header";
      repeat := properties;
   }

   void validateEditorTree() {
      boolean needsRefresh = headerList.refreshList();
      super.validateEditorTree();
   }

   void refreshChildren() {
      headerList.refreshList();
      super.refreshChildren();
   }

   void validateChildLists() {
      validateChildList(0, headerList.repeatComponents, false);
      validateChildList(headerList.repeatComponents.size(), childList.repeatComponents, true);
      validateSize();
   }

   List<Object> getHeaderCellList() {
      return new ArrayList<Object>(headerList.repeatComponents);
   }

}