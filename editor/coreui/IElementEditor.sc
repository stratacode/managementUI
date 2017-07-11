interface IElementEditor {
   void updateListeners();
   void removeListeners();

   boolean isVisible();
   void setVisible(boolean val);

   void updateEditor(Object elem, Object prop, Object type, Object inst);

   void setRepeatVar(Object val);

   void setRepeatIndex(int ix);
}
