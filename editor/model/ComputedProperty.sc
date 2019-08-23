// These are created as part of the UI on the client and server so don't sync
//@sc.obj.Sync(syncMode=SyncMode.Disabled)
class ComputedProperty extends CustomProperty implements Cloneable {
   ComputedProperty(String name, Object propertyType, String editorType, Object value, int defaultWidth, UIIcon icon) {
      super(name, propertyType, editorType, value, defaultWidth, icon);
   }

   ComputedProperty clone() {
      try {
         return (ComputedProperty) super.clone();
      }
      catch (CloneNotSupportedException exc) {}
      return null;
   }

}