enum CreateMode {
   Instance,
   Property,
   Class,
   Object,
   Layer;

   static String[] getAllNames() {
      return new String[] {"Instance", "Property", "Class", "Object", "Layer"};
   }

   static String[] getNoCurrentTypeNames() {
      return new String[] {"Instance", "Class", "Object", "Layer"};
   }
}
