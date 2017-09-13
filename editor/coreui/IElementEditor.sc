interface IElementEditor {
   void updateListeners(boolean add);
   void removeListeners();

   // Called to validate the editor after the model is updated
   void invalidateEditor();

   boolean isVisible();
   void setVisible(boolean val);

   void updateEditor(Object elem, Object prop, Object type, Object inst, int ix);

   void setRepeatVar(Object val);

   void setRepeatIndex(int ix);

   public void setListIndex(int ix);
   public int getListIndex();

   public boolean getCellMode();
   public void setCellMode(boolean b);
}
