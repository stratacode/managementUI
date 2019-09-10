import java.util.Arrays;
import sc.type.IResponseListener;

EditorModel {
   Object[] getPropertiesForType(Object type, IResponseListener updateListener) {
      List<Object> res;

      if (type instanceof TypeDeclaration) {
         TypeDeclaration td = ((TypeDeclaration) type);
         boolean isHidden = td.getLayer() != null && td.getLayer().hidden;
         List<Object> props = isHidden ? null : td.getDeclaredProperties();
         if (props == null)
            props = new ArrayList<Object>();
         else
            props = new ArrayList<Object>(props);
         if (td.getInheritProperties()) {
            String baseTypeName = td.getExtendsTypeName();
            if (baseTypeName != null) {
               BodyTypeDeclaration extType = getOrFetchTypeByName(baseTypeName, updateListener);
               if (extType != null && extType.getExportProperties()) {
                  Layer l = extType.getLayer();
                  Object[] extProps = getPropertiesForType(extType, updateListener);
                  props = ModelUtil.mergeProperties(props, extProps == null ? null : Arrays.asList(extProps), false, true);
               }
            }
         }
         BodyTypeDeclaration modTD = td.getModifiedType();
         if (modTD != null && ctx.currentLayers.contains(modTD.getLayer())) {
            props = addModifiedProperties(modTD, props);
         }
         res = new ArrayList<Object>(props.size());
         // Reorder so that properties are always on top.  When they become interleaved with objects the display is messy
         for (Object prop:props) {
            if (prop instanceof VariableDefinition || prop instanceof PropertyAssignment || prop instanceof sc.lang.java.EnumConstant) {
               if (isVisible(prop))
                  res.add(prop);
            }
            // TODO: if prop is String, we could use DynUtil.getPropertyAnnotation to find the annotation on the runtime types?
         }
         for (Object prop:props) {
            if (prop instanceof BodyTypeDeclaration) {
               if (isVisible(prop))
                  res.add(prop);
            }
         }
         return res.toArray();
      }
      else if (type != null) {
         BodyTypeDeclaration btd = getOrFetchTypeByName(DynUtil.getTypeName(type, false), updateListener);
         if (btd != null) {
            return getPropertiesForType(btd, updateListener);
         }
      }
      return null;
   }


   List<Object> addModifiedProperties(BodyTypeDeclaration td, List<Object> props) {
      BodyTypeDeclaration modTD = td.getModifiedType();
      if (modTD != null) {
         props = addModifiedProperties(modTD, props);
      }
      List<Object> tdProps = td.getDeclaredProperties();
      props = ModelUtil.mergeProperties(props, tdProps, false, true);
      return props;
   }

}
