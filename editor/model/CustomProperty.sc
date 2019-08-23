import sc.obj.IObjectId;

@sc.obj.Sync(syncMode=SyncMode.Enabled, onDemand=true)
class CustomProperty {
   String name;
   Object propertyType;
   String editorType;
   Object value;
   int defaultWidth;
   UIIcon icon;
   String operator = null;

   CustomProperty(String name, Object propertyType, String editorType, Object value, int defaultWidth, UIIcon icon) {
      this.name = name;
      this.propertyType = propertyType;
      this.editorType = editorType;
      this.value = value;
      this.defaultWidth = defaultWidth;
      this.icon = icon;
   }

   String updateInstance(Object inst, Object elemValue) {
      return "Custom property: " + name + " does not support updates";
   }

   boolean isSettableFromString(Object propType) {
      return false;
   }

   boolean isConstant() {
      return true;
   }

   String getDescription() {
      return "Property: " + name + " of type: " + DynUtil.getTypeName(propertyType, false);
   }

   String getValueString() {
      return value.toString();
   }

   int compare(Object o1, Object o2) {
      if (!(o1 instanceof Comparable)) {
         if (name.equals("Id"))
            return DynUtil.compare(DynUtil.getInstanceName(o1), DynUtil.getInstanceName(o2));
      }
      return DynUtil.compare(o1,o2);
   }
}