// These are created as part of the UI on the client and server so don't sync
//@sc.obj.Sync(syncMode=SyncMode.Disabled)
class ComputedProperty extends CustomProperty {
   ComputedProperty(String name, Object propertyType, String editorType, Object value, int defaultWidth, UIIcon icon) {
      super(name, propertyType, editorType, value, defaultWidth, icon);
   }
}