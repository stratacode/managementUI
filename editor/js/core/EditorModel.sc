import java.util.Arrays;

EditorModel {
   // There are lots of features in the dynamic runtime dependent version of this method (e.g. transparent layers) - this is a replacement that works with what's available
   Object[] getPropertiesForType(Object type) {
      List<Object> res;

      if (type instanceof TypeDeclaration) {
         List<Object> props = ((TypeDeclaration) type).getDeclaredProperties();
         if (props == null)
            props = new ArrayList<Object>();
         if (inherit) {
            Object extType = DynUtil.getExtendsType(type); // change#9
            if (extType != null) {
               Object[] extProps = getPropertiesForType(extType);
               props = ModelUtil.mergeProperties(props, extProps == null ? null : Arrays.asList(extProps), false, true);
            }
         }
         // if (mergeLayers) - Need to add a way to resolve the types for all layers.  Right now, the type tree loads each version of the type declaration
         // but there's no data structure to tie them together.  We'd like a getModifiedType() method in js.layer/BodyTypeDeclaration.  When mergeLayers is true
         // we can get the properties from that type and merge them in when mergeLayers is set to true.
         // Design: each type meta-data when loaded registers with the Layer it's in.  We ship over the modified layer name in the type declaration.  When
         // necessary, we fetch the layer, and from the layer we fetch the layer's version of that type.
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
         if (res.size() != props.size())
            System.out.println("***");
         return res.toArray();
      }
      return null;
   }

}
