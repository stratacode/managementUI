GlobalResources {

   static UIIcon lookupUIIcon(Object type) {
      if (ModelUtil.isLayerType(type)) {
         Layer l = LayeredSystem.getCurrent().getLayerByDirName(((ModifyDeclaration) type).typeName);
         if (l == null) {
            System.err.println("*** Can't find layer for type: " + type);
            return layerIcon;
         }
         return l.dynamic ? layerDynIcon : layerIcon;
      }
      else if (ModelUtil.isProperty(type)) {
         Object propType = ModelUtil.getPropertyType(type);

         // For icon purposes just convert these to Object for now.
         if (ModelUtil.isTypeVariable(propType))
            propType = ModelUtil.getTypeParameterDefault(propType);

         Object enclType = ModelUtil.getEnclosingType(type);
         boolean isDyn;
         // Interfaces are not considered compiled properties in the strict sense (as they can be overridden by dynamic properties)
         if (ModelUtil.getDeclarationType(enclType) == DeclarationType.INTERFACE)
            isDyn = ModelUtil.isDynamicType(enclType);
         else
            isDyn = !ModelUtil.isCompiledProperty(enclType, ModelUtil.getPropertyName(type), false, false);

         return lookupUIIcon(propType, isDyn);
      }
      else {
         boolean isDyn = ModelUtil.isDynamicType(type);
         return lookupUIIcon(type, isDyn);
      }
   }

   static UIIcon lookupUIIcon(Object type, boolean isDyn) {
      if (type == null)
         return null;
      if (DynUtil.isType(type) || type instanceof ClientTypeDeclaration) {
         String typeName = ModelUtil.getTypeName(type);
         if (ModelUtil.isPrimitive(type)) {
            if (typeName.equals("int") || typeName.equals("short") || typeName.equals("byte") || typeName.equals("long"))
               return isDyn ? intDynIcon : intIcon;
            else if (typeName.equals("float") || typeName.equals("double"))
               return isDyn ? floatDynIcon : floatIcon;
            else if (typeName.equals("boolean"))
               return isDyn ? booleanDynIcon : booleanIcon;
         }
         if (typeName.equals("String") || typeName.equals("java.lang.String"))
            return isDyn ? stringDynIcon : stringIcon;
         return ModelUtil.isObjectType(type) ?
             (isDyn ? objectDynIcon : objectIcon) :
             (ModelUtil.isEnumType(type) || ModelUtil.isEnum(type) ? (isDyn ? enumDynIcon : enumIcon) : (isDyn ? classDynIcon : classIcon));
      }
      else if (ModelUtil.isProperty(type) || ModelUtil.isLayerType(type)) {
         return lookupUIIcon(type);
      }
      else
         System.err.println("*** Unrecognized type in lookupUIIcon");
      return null;
   }

   static UIIcon createIcon(String pathName, String textName) {
      // Caching the icons to shrink the state that needs to be sync'd to the client
      UIIcon icon = iconCache.get(pathName);
      if (icon == null) {
         icon = new UIIcon("/sc/editor/", pathName, textName);
         iconCache.put(pathName, icon);
      }
      return icon;
   }
}
