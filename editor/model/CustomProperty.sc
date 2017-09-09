
@sc.obj.Sync(syncMode=sc.obj.SyncMode.Disabled)
class CustomProperty {
   String name;
   Object propertyType;
   String editorType;
   Object value;
   int defaultWidth;
   UIIcon icon;

   CustomProperty(String name, Object propertyType, String editorType, Object value, int defaultWidth, UIIcon icon) {
      this.name = name;
      this.propertyType = propertyType;
      this.editorType = editorType;
      this.value = value;
      this.defaultWidth = defaultWidth;
      this.icon = icon;
   }

   void updateInstance(Object inst, Object elemValue) {
      throw new IllegalArgumentException("Custom property: " + name + " does not support updates");
   }

   boolean isConstant() {
      return true;
   }
}