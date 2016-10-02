FormEditor {
   IElementEditor lastView;
   IElementEditor prev;

   @Bindable
   int x := col * (xpad + columnWidth) + (parentView.nestWidth/2) + xpad,
       y := prev == null ? ypad + (parentEditor == null ? 0 : parentEditor.startY) : prev.y + prev.height + ypad,
       width := columnWidth - (nestLevel * (parentView.nestWidth + 2*xpad)),
       height := (lastView == null ? startY : lastView.y + lastView.height) + 2 * ypad + borderSize + borderBottom;

   int row, col;

   startY := ypad + borderTop;

   size := SwingUtil.dimension(width, height);
   location := SwingUtil.point(x, y);

   boolean transparentType := !ModelUtil.isTypeInLayer(type, classViewLayer);

   int titleBorderX = 30;

   // A border around the contents of the group with the group name
   //border := BorderFactory.createCompoundBorder (BorderFactory.createEmptyBorder(borderSize, borderSize, borderSize, borderSize), BorderFactory.createTitledBorder(title));
   //border := BorderFactory.createTitledBorder(title);
   object border extends GapBorder {
      border = BorderFactory.createEtchedBorder();
      gapX := titleBorderX + xpad;
      gapW := (int) (objectLabel.size.width + nameButton.size.width + extendsLabel.size.width + extendsTypeButton.size.width + instanceList.size.width + 6*xpad);
   }

   int borderTitleY = 0;

   object objectLabel extends JLabel {
      location := SwingUtil.point(2*xpad + borderSize + titleBorderX, borderTitleY + baseline);
      icon := GlobalResources.lookupIcon(type);
      size := preferredSize;
      text := operatorName;
      border = BorderFactory.createEmptyBorder();
   }

   object nameButton extends LinkButton {
      location := SwingUtil.point(objectLabel.location.x + objectLabel.size.width + xpad, borderTitleY + baseline);
      size := preferredSize;
      text := type == null ? "<null>" : ModelUtil.getClassName(type);
      clickCount =: editorModel.changeCurrentType(type);
      foreground := transparentType ? GlobalResources.transparentTextColor : GlobalResources.normalTextColor;
   }

   object extendsLabel extends JLabel {
      location := SwingUtil.point(nameButton.location.x + nameButton.size.width + xpad, borderTitleY + baseline);
      size := preferredSize;
      text := extTypeName == null ? "" : "extends";
      border = BorderFactory.createEmptyBorder();
   }

   object extendsTypeButton extends LinkButton {
      location := SwingUtil.point(extendsLabel.location.x + extendsLabel.size.width + xpad, borderTitleY + baseline);
      size := preferredSize;
      text := extTypeName == null ? "" : CTypeUtil.getClassName(extTypeName);
      clickCount =: editorModel.changeCurrentType(DynUtil.findType(extTypeName));
   }

   object instanceList extends JComboBox {
      location := SwingUtil.point(extendsTypeButton.location.x + extendsTypeButton.size.width + xpad, borderTitleY);
      size := preferredSize;
      items := editorModel.ctx.getInstancesOfType(type, 10, true);
      userSelectedItem =: userSelectedInstance((InstanceWrapper) selectedItem);
   }

   object childList extends RepeatComponent<IElementEditor> {
      repeat := properties;

      public Object createRepeatElement(Object prop, int ix, Object oldComp) {
         if (prop instanceof BodyTypeDeclaration) {
            /*
                            prop = editorModel.processVisibleType(prop);
                             if (prop == null)
                                continue;
            */
            FormEditor editor = new FormEditor(parentView, FormEditor.this, (BodyTypeDeclaration) prop, null);
            return editor;
         }
         else if (ModelUtil.isProperty(prop)) {
            ElementEditor elemView = new TextFieldEditor(prop);
            /*
                  // If there is a current layer set and this object is not defined in that layer (or merged), skip it.
                  // If we get a def for a prop which is after the specified layer, see if there's a previous definition
                  // we should be using first.
                  if (editorModel.currentLayer != null) {
                     Layer propLayer = ModelUtil.getPropertyLayer(prop);

                     // If this property is not defined or modified in this layer or one which should be merged continue.
                     while (propLayer != null && !editorModel.currentLayerMatches(propLayer)) {
                        Object prevProp = ModelUtil.getPreviousDefinition(prop);
                        if (prevProp == null) {
                           prop = null;
                           break;
                        }
                        prop = prevProp;
                        propLayer = ModelUtil.getPropertyLayer(prop);
                        if (propLayer == null)
                          prop = null;
                    }
                    if (prop == null)
                       continue;
                 }
           */

           return elemView;
        }
     }
  }

   childViews = childList.repeatComponents;

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

      for (int i = 0; i < childViews.size(); i++) {
         IElementEditor view = childViews.get(i);
         if (view instanceof FormEditor) {
            FormEditor cview = (FormEditor) view;
            if (cview.type instanceof BodyTypeDeclaration) {
               BodyTypeDeclaration btd = (BodyTypeDeclaration) cview.type;
               if (btd.getDefinesCurrentObject()) {
                  Object childInst = instance == null ? null : DynUtil.getProperty(instance, btd.getTypeName());
                  cview.setSelectedInstance(childInst);
               }
            }
         }
      }
   }

   void instanceChanged() {
      if (removed)
          return;
      if (instanceList.selectedItem == null || ((InstanceWrapper)instanceList.selectedItem).instance != instance) {
         instanceList.selectedItem = new InstanceWrapper(editorModel.ctx, instance);
      }
      super.instanceChanged();
   }

}