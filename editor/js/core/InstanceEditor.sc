InstanceEditor {
   ElementRepeatWrapper repeatWrapper;

   class ElementRepeatWrapper extends sc.lang.html.Div implements sc.lang.html.IRepeatWrapper {
      String displayMode;
      Element createElement(Object elem, int ix, Element oldTag) {
         return (Element) InstanceEditor.this.createElementEditor(elem, ix, (IElementEditor) oldTag, displayMode);
      }

      void repeatTagsChanged() {
         childViewsChanged();
      }
   }

   void refreshChildren() {
      repeatWrapper.refreshRepeat();
   }

   void updateListeners(boolean add) {
      Object[] children = DynUtil.getObjChildren(repeatWrapper, null, true);
      if (children == null) {
         childViews = null;
         return;
      }
      else {
         childViews = new ArrayList<IElementEditor>(children.length);
      }

      for (Object child:children) {
         if (child instanceof IElementEditor) {
            childViews.add((IElementEditor) child);
         }
      }
      super.updateListeners(add);
   }

}