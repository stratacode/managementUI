interface IElementEditor {
   void updateListeners(boolean add);
   void removeListeners();

   // Called to validate the editor after the model is updated
   void invalidateEditor();

   // Perform an incremental refresh of the editor and all children (e.g. size of child changed)
   void validateEditorTree();

   // Incremental refresh to detect possible size changes
   void validateSize();

   boolean isVisible();
   void setVisible(boolean val);

   void updateEditor(Object elem, Object prop, Object type, Object inst, int ix, InstanceWrapper wrapper);

   /** This is the property, type, etc. for this editor to edit */
   void setElemToEdit(Object elem);
   Object getElemToEdit();

   /** It's position in the parent list */
   void setRepeatIndex(int ix);
   int getRepeatIndex();

   /** If we are editing a list, the list index */
   public void setListIndex(int ix);
   public int getListIndex();

   public int getCurrentListSize();

   void cellWidthChanged(String propName);

   public boolean getCellMode();
   public void setCellMode(boolean b);

   public boolean getCellChild();
   public void setCellChild(boolean b);

   void updateComputedValues();
}
