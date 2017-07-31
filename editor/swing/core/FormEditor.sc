FormEditor {
   // A border around the contents of the group with the group name
   //border := BorderFactory.createCompoundBorder (BorderFactory.createEmptyBorder(borderSize, borderSize, borderSize, borderSize), BorderFactory.createTitledBorder(title));
   //border := BorderFactory.createTitledBorder(title);
   object border extends GapBorder {
      border = BorderFactory.createEtchedBorder();
      gapX := titleBorderX + xpad;
      gapW := (int) (objectLabel.size.width + nameButton.size.width + extendsLabel.size.width + extendsTypeButton.size.width + instanceList.visWidth + 6*xpad);
   }

   int borderTitleY = 0;

   object objectLabel extends JLabel {
      location := SwingUtil.point(2*xpad + borderSize + titleBorderX, borderTitleY + baseline);
      icon := getSwingIcon(FormEditor.this.icon);
      size := preferredSize;
      text := operatorName;
      border = BorderFactory.createEmptyBorder();
   }

   object nameButton extends LinkButton {
      location := SwingUtil.point(objectLabel.location.x + objectLabel.size.width + xpad, borderTitleY + baseline);
      size := preferredSize;
      text := displayName;
      clickCount =: parentProperty == null ? editorModel.changeCurrentType(type, instance) : parentView.focusChanged(null, parentProperty, instance, true);
      defaultColor := parentProperty != null && editorModel.currentProperty == parentProperty ? GlobalResources.highlightColor : (transparentType ? GlobalResources.transparentTextColor : GlobalResources.normalTextColor);
   }

   object extendsLabel extends JLabel {
      location := SwingUtil.point(nameButton.location.x + nameButton.size.width + xpad, borderTitleY + baseline);
      size := preferredSize;
      text := extendsTypeLabel;
      border = BorderFactory.createEmptyBorder();
   }

   // TODO: rename this to 'formType'?   When we are editing a property, it's the type of the property not the base type
   object extendsTypeButton extends LinkButton {
      location := SwingUtil.point(extendsLabel.location.x + extendsLabel.size.width + xpad, borderTitleY + baseline);
      size := preferredSize;
      text := extendsTypeName;
      clickCount =: gotoExtendsType();
   }

   object instanceList extends JComboBox {
      location := SwingUtil.point(extendsTypeButton.location.x + extendsTypeButton.size.width + xpad, borderTitleY);
      size := preferredSize;
      visible := showInstanceSelect;
      double visWidth := !visible ? 0.0 : size.width;
      //items := !visible ? java.util.Collections.emptyList() : instancesOfType;
      items := instancesOfType;
      selectedIndex := getInstSelectedIndex(instance, instancesOfType);
      userSelectedItem =: userSelectedInstance((InstanceWrapper) selectedItem);
   }

   void validateTree() {
      childList.refreshList();
   }

   void refreshChildren() {
      childList.refreshList();
   }

   object childList extends RepeatComponent<IElementEditor> {
      repeat := properties;

      parentComponent = FormEditor.this;

      int numRows := (DynUtil.getArrayLength(repeat) + numCols-1) / numCols;

      int oldNumRows, oldNumCols;

      IElementEditor[][] viewsGrid;

      public IElementEditor createRepeatElement(Object prop, int ix, Object oldComp) {
         IElementEditor res = null;

         res = createElementEditor(prop, ix, (IElementEditor)oldComp);

         updateCell(res, ix);

         return res;
      }

      void refreshList() {
         int size = DynUtil.getArrayLength(repeat);

         boolean gridChanged = false;
         if (oldNumRows != numRows || oldNumCols != numCols) {
            gridChanged = true;
            viewsGrid = new IElementEditor[numRows][numCols];
         }

         super.refreshList();

         if (gridChanged) {
             int ix = 0;
             for (Object elem:repeatComponents) {
                IElementEditor fed = (IElementEditor) elem;
                updateCell(fed, ix);
                ix++;
             }
         }

         validateTree();

         lastView = repeatComponents.size() == 0 ? null : repeatComponents.get(repeatComponents.size()-1);
      }

      public void validateTree() {
         int curIx = 0;
         for (Object elem:repeatComponents) {
            IElementEditor ed = (IElementEditor) elem;

            if (childViews.size() <= curIx) {
               childViews.add(ed);
               SwingUtil.addChild(parentComponent, ed);
            }
            else if (childViews.get(curIx) != ed) {
               remove(curIx);
               childViews.set(curIx, ed);
               SwingUtil.addChild(parentComponent, ed);
            }
            ed.updateListeners(true);
            if (ed instanceof TypeEditor)
               ((TypeEditor) ed).validateTree();
            curIx++;
         }
         while (curIx < childViews.size()) {
             remove(curIx);
             childViews.remove(curIx);
         }
      }

      private void updateCell(IElementEditor ed, int ix) {
         ed.row = ix / numCols;
         ed.col = ix % numCols;
         if (ed.row != 0)
            ed.prev = viewsGrid[ed.row -1][ed.col];
         else
            ed.prev = null;

         viewsGrid[ed.row][ed.col] = ed;
      }
   }

   childViews = new ArrayList<IElementEditor>();

   void userSelectedInstance(InstanceWrapper wrapper) {
      setSelectedInstance(wrapper.instance);
   }

   void setSelectedInstance(Object inst) {
      instance = inst;
      // When the user explicitly chooses <type> we need to mark that they've chosen nothing for this type to keep the current object from coming back
      if (instance == null)
         parentView.setDefaultCurrentObj(type, EditorContext.NO_CURRENT_OBJ_SENTINEL);
      else {
         parentView.setDefaultCurrentObj(type, instance);
      }

      if (childViews != null) {
         for (int i = 0; i < childViews.size(); i++) {
            IElementEditor view = childViews.get(i);
            if (view instanceof FormEditor) {
               FormEditor cview = (FormEditor) view;
               if (cview.type instanceof BodyTypeDeclaration) {
                  BodyTypeDeclaration btd = (BodyTypeDeclaration) cview.type;
                  if (hasInnerTypeInstance(btd)) {
                     Object childInst = instance == null ? null : DynUtil.getProperty(instance, btd.getTypeName());
                     cview.setSelectedInstance(childInst);
                  }
               }
            }
         }
      }
   }
}