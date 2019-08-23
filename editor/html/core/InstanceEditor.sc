InstanceEditor {
   ElementRepeatWrapper repeatWrapper;

   class ElementRepeatWrapper extends sc.lang.html.Div implements sc.lang.html.IRepeatWrapper {
      String displayMode;
      Element createElement(Object elem, int ix, Element oldTag) {
         return (Element) InstanceEditor.this.createElementEditor(elem, ix, (IElementEditor) oldTag, displayMode);
      }

      void repeatTagsChanged() {
         childViewsChanged(true);
      }

      // Called when an element above us in the list has been removed, so we can renumber the elements in the list.
      void updateElementIndexes(int fromIx) {
         Object[] children = DynUtil.getObjChildren(repeatWrapper, null, false);
         if (children != null) {
            for (int i = fromIx; i < children.length; i++) {
               IElementEditor child = (IElementEditor) children[i];
               if (child != null && child.getListIndex() != i)
                  child.setListIndex(i);
            }
         }
      }
   }

   class HeaderRepeatWrapper extends ElementRepeatWrapper {
   // Will create an instance of HeaderCellEditor for each child
      displayMode = "header";
   }

   void refreshChildren() {
      repeatWrapper.refreshRepeat(true);
   }

   void updateListeners(boolean add) {
      scheduleValidateTree();
      if (repeatWrapper != null) {
         Object[] children = DynUtil.getObjChildren(repeatWrapper, null, true);
         if (children == null) {
            childViews = null;
            return;
         }
         else {
            childViews = new ArrayList<IElementEditor>(children.length);
         }

         int ix = 0;
         for (Object child:children) {
            if (child instanceof IElementEditor) {
               IElementEditor childEditor = (IElementEditor) child;
               if (childEditor.getListIndex() != ix)
                  childEditor.setListIndex(ix);
               childViews.add(childEditor);
            }
            ix++;
         }
      }
      super.updateListeners(add);
   }

}