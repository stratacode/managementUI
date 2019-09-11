import sc.bind.BindingContext;

InstanceEditor {
   IElementEditor listStart;

   void validateChildLists() {}

   void clearChildren() {
      if (childViews == null)
         return;
      for (int i = childViews.size() - 1; i >= 0; i--) {
         SwingUtil.removeChild(this, childViews.get(i));
      }
      super.clearChildren();
   }

   public void validateChildList(int startIx, List<IElementEditor> repeatComponents, boolean removeEnd) {
      int curIx = startIx;
      for (int i = 0; i < repeatComponents.size(); i++) {
         IElementEditor ed = (IElementEditor) repeatComponents.get(i);

         IElementEditor cur;

         if (childViews.size() <= curIx) {
            childViews.add(ed);
            SwingUtil.addChild(this, ed);
         }
         else if ((cur = childViews.get(curIx)) != ed) {
            if (cur != null)
               SwingUtil.removeChild(this, cur);
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
            IElementEditor remEd = childViews.remove(curIx);
            SwingUtil.removeChild(this, remEd);
         }
      }
   }

   class ChildList extends RepeatComponent<IElementEditor> {
      manageChildren = false; // We call SwingUtil.addChild explicitly when synchronizing the childViews array

      // If it's the header list, don't put them into the grid
      boolean headerMode = false;

      parentComponent = InstanceEditor.this;

      int oldNumRows, oldNumCols;

      IElementEditor[][] viewsGrid;

      IElementEditor listStart := InstanceEditor.this.listStart;

      String displayMode;

      public IElementEditor createRepeatElement(Object prop, int ix, Object oldComp) {
         IElementEditor res = null;

         res = createElementEditor(prop, ix, (IElementEditor)oldComp, displayMode);

         updateCell(res, ix);

         return res;
      }

      public Object getRepeatVar(IElementEditor component) {
         return component.getElemToEdit();
      }

      public void setRepeatIndex(IElementEditor component, int ix) {
      }

      boolean refreshList() {
         int size = DynUtil.getArrayLength(repeat);

         boolean gridChanged = false;
         if (!headerMode) {
            if (oldNumRows != numRows || oldNumCols != numCols) {
               gridChanged = true;
               viewsGrid = new IElementEditor[numRows][numCols];
               oldNumRows = numRows;
               oldNumCols = numCols;
            }
         }

         boolean anyChanges = super.refreshList();

         // TODO: is this necessary? It seems like it might be more efficient if the entire chain is set up before
         // we start delivering the change events.
         BindingContext oldCtx = BindingContext.queueEvents();
         try {
            if ((gridChanged || anyChanges)) {
               int ix = 0;
               for (Object elem:repeatComponents) {
                  IElementEditor fed = (IElementEditor) elem;
                  updateCell(fed, ix);
                  ix++;
               }
            }

            if (!headerMode)
               lastView = repeatComponents.size() == 0 ? null : repeatComponents.get(repeatComponents.size()-1);

            Bind.sendChangedEvent(InstanceEditor.this, "cellHeight");
            Bind.sendChangedEvent(this, "lastEditor");
         }
         finally {
            BindingContext.flushQueue(oldCtx);
         }
         return anyChanges;
      }

      void listChanged() {
         validateChildLists();
      }

      private void updateCell(IElementEditor ed, int ix) {
         if (headerMode) {
            ed.prev = null;
            ed.prevCell = ix == 0 ? null : repeatComponents.get(ix-1);
            return;
         }

         ed.row = ix / numCols;
         ed.col = ix % numCols;
         if (ed.row != 0)
            ed.prev = viewsGrid[ed.row - 1][ed.col];
         else
            ed.prev = listStart;
         if (ed.col != 0)
            ed.prevCell = viewsGrid[ed.row][ed.col - 1];
         else
            ed.prevCell = null;

         if (ed.col >= numCols)
            System.out.println("*** Invalid column");
         if (ed.row >= numRows)
            System.out.println("*** Invalid row");

         viewsGrid[ed.row][ed.col] = ed;
      }

      @Bindable(manual=true)
      IElementEditor getLastEditor() {
         int sz = repeatComponents.size();
         if (sz == 0)
            return null;
         return repeatComponents.get(sz-1);
      }
   }
}
