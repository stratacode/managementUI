EditorModel {
   Object[] getPropertiesForType(Object type) {
      // TODO: implement merge and inherit here
      if (type instanceof TypeDeclaration) {
         List<Object> props = ((TypeDeclaration) type).getDeclaredProperties();
         if (props == null)
            return null;
         return props.toArray();
      }
      return null;
   }

/*
 * A remote method
   String setElementValue(Object type, Object inst, Object prop, String expr, boolean updateInstances, boolean valueIsExpr) {
   }
*/
}
