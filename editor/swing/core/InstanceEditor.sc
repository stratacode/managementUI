InstanceEditor {
   void validateChildLists() {}

   public void validateChildList(int startIx, List<IElementEditor> repeatComponents, boolean removeEnd) {
      int curIx = startIx;
      for (int i = 0; i < repeatComponents.size(); i++) {
         IElementEditor ed = (IElementEditor) repeatComponents.get(i);

         if (childViews.size() <= curIx) {
            childViews.add(ed);
            SwingUtil.addChild(this, ed);
         }
         else if (childViews.get(curIx) != ed) {
            remove(curIx);
            childViews.set(curIx, ed);
            SwingUtil.addChild(this, ed);
         }
         ed.updateListeners(true);
         if (ed instanceof TypeEditor)
            ((TypeEditor) ed).validateEditorTree();
         curIx++;
      }
      if (removeEnd) {
         while (curIx < childViews.size()) {
            remove(curIx);
            childViews.remove(curIx);
         }
      }
   }

   class ChildList extends RepeatComponent<IElementEditor> {
      manageChildren = false; // We call SwingUtil.addChild explicitly when synchronizing the childViews array

      parentComponent = InstanceEditor.this;

      int oldNumRows, oldNumCols;

      IElementEditor[][] viewsGrid;

      String displayMode;

      public IElementEditor createRepeatElement(Object prop, int ix, Object oldComp) {
         IElementEditor res = null;

         res = createElementEditor(prop, ix, (IElementEditor)oldComp, displayMode);

         updateCell(res, ix);

         return res;
      }

      boolean refreshList() {
         int size = DynUtil.getArrayLength(repeat);

         if ((numRows == 0 || numCols == 0) && size > 0)
            System.out.println("***");

         boolean gridChanged = false;
         if (oldNumRows != numRows || oldNumCols != numCols) {
            gridChanged = true;
            viewsGrid = new IElementEditor[numRows][numCols];
            oldNumRows = numRows;
            oldNumCols = numCols;
         }

         boolean anyChanges = super.refreshList();

         if (gridChanged || anyChanges) {
            int ix = 0;
            for (Object elem:repeatComponents) {
               IElementEditor fed = (IElementEditor) elem;
               updateCell(fed, ix);
               ix++;
            }
         }

         lastView = repeatComponents.size() == 0 ? null : repeatComponents.get(repeatComponents.size()-1);

         Bind.sendChangedEvent(InstanceEditor.this, "cellHeight");
         return anyChanges;
      }

      void listChanged() {
         validateChildLists();
      }

      private void updateCell(IElementEditor ed, int ix) {
         if (numCols == 0)
            System.out.println("***");
         ed.row = ix / numCols;
         ed.col = ix % numCols;
         if (ed.row != 0)
            ed.prev = viewsGrid[ed.row - 1][ed.col];
         else
            ed.prev = null;
         if (ed.col != 0)
            ed.prevCell = viewsGrid[ed.row][ed.col - 1];
         else
            ed.prevCell = null;

         viewsGrid[ed.row][ed.col] = ed;
      }
   }
}