interface IElementEditor {
   void updateListeners(boolean add);
   void removeListeners();

   // Called to validate the editor after the model is updated
   void invalidateEditor();

   boolean isVisible();
   void setVisible(boolean val);

   void updateEditor(Object elem, Object prop, Object type, Object inst, int ix);

   /** This is the property, type, etc. for this editor to edit */
   void setElemToEdit(Object elem);

   /** It's position in the parent list */
   void setRepeatIndex(int ix);
   int getRepeatIndex();

   /** If we are editing a list, the list index */
   public void setListIndex(int ix);
   public int getListIndex();

   public boolean getCellMode();
   public void setCellMode(boolean b);
}
