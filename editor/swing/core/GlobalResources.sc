GlobalResources {
   // Returns the swing icon
   static Icon lookupIcon(Object type) {
      UIIcon icon = lookupUIIcon(type);
      if (icon == null)
         return null;
      return icon.icon;
   }
}
