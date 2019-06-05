import sc.lang.InstanceWrapper;
import sc.type.RTypeUtil;
import sc.type.Type;

class ConstructorProperty extends CustomProperty {
   InstanceWrapper wrapper;
   ConstructorProperty(String name, Object propertyType, String editorType, InstanceWrapper wrapper) {
      super(name, propertyType, editorType, wrapper, 100, null);
      this.wrapper = wrapper;
   }

   boolean isConstant() {
      return false;
   }

   boolean isSettableFromString(Object propType) {
      return RTypeUtil.canConvertTypeFromString(propType);
   }

   String updateInstance(Object inst, Object elemValue) {
      wrapper.pendingValues.put(name, elemValue);
      return null;
   }

   String getValueString() {
      String res = (String) wrapper.pendingValues.get(name);
      if (res == null)
         res = "";
      return res;
   }
}