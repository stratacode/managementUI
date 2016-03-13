import sc.dyn.DynUtil;
import sc.lang.java.BodyTypeDeclaration;
import sc.lang.java.DeclarationType;
import sc.lang.java.ModelUtil;
import sc.lang.sc.ModifyDeclaration;
import sc.layer.Layer;
import sc.layer.LayeredSystem;

import java.util.HashMap;

/** UI resources for the editor application (the part shared by the js and swing components) */
class GlobalResources {
   @sc.obj.Sync(syncMode=sc.obj.SyncMode.Disabled)
   static HashMap<String,UIIcon> iconCache = new HashMap<String,UIIcon>();

   final static UIIcon classIcon = createIcon("images/class.gif", "Class");
   final static UIIcon objectIcon = createIcon("images/object.gif", "Object");
   final static UIIcon classDynIcon = createIcon("images/classDyn.gif", "Dynamic Class");
   final static UIIcon objectDynIcon = createIcon("images/objectDyn.gif", "Dynamic Object");
   final static UIIcon interfaceIcon = createIcon("images/interface.gif", "Interface");
   final static UIIcon interfaceDynIcon = createIcon("images/interfaceDyn.gif", "Dynamic interface");
   final static UIIcon enumIcon = createIcon("images/enum.gif", "Enum");
   final static UIIcon enumDynIcon = createIcon("images/enumDyn.gif", "Dynamic Enum");
   final static UIIcon layerIcon = createIcon("images/layer.gif", "Compiled Layer");
   final static UIIcon layerDynIcon = createIcon("images/layerDyn.gif", "Dynamic Layer");
   final static UIIcon inactiveLayerIcon = createIcon("images/inactiveLayer.gif", "Inactive Layer");

   final static UIIcon stringIcon = createIcon("images/string.gif", "String");
   final static UIIcon intIcon = createIcon("images/int.gif", "Integer");
   final static UIIcon floatIcon = createIcon("images/float.gif", "Floating point");
   final static UIIcon booleanIcon = createIcon("images/boolean.gif", "Boolean");

   final static UIIcon stringDynIcon = createIcon("images/stringDyn.gif", "Dyn String");
   final static UIIcon intDynIcon = createIcon("images/intDyn.gif", "Dyn Integer");
   final static UIIcon floatDynIcon = createIcon("images/floatDyn.gif", "Dyn Floating point");
   final static UIIcon booleanDynIcon = createIcon("images/booleanDyn.gif", "Dyn Boolean");

   final static UIColor needsSaveTextColor = new UIColor(0x1c, 0x88, 0x23);
   final static UIColor errorTextColor = new UIColor(0x88, 0x22, 0x33);
   final static UIColor normalTextColor = new UIColor(16, 16, 16);
   final static UIColor transparentTextColor = new UIColor(128, 128, 128);

   static UIIcon lookupUIIcon(Object type) {
      if (ModelUtil.isLayerType(type)) {
         LayeredSystem sys = LayeredSystem.getCurrent();
         if (sys != null) { 
            Layer l = sys.getLayerByDirName(((ModifyDeclaration) type).typeName);
            if (l == null) {
               System.err.println("*** Can't find layer for type: " + type);
               return layerIcon;
            }
            return l.dynamic ? layerDynIcon : layerIcon;
         }
         else {
            return layerIcon;
         }
      }
      else if (ModelUtil.isProperty(type)) {
         Object propType = ModelUtil.getPropertyType(type);

         // TODO: do we need this case anymore?  for properties we should always provide the isDyn flag
         return lookupUIIcon(propType, false);
      }
      else {
         boolean isDyn = ModelUtil.isDynamicType(type);
         return lookupUIIcon(type, isDyn);
      }
   }

   static UIIcon lookupUIIcon(Object type, boolean isDyn) {
      if (type == null)
         return null;
      if (type instanceof BodyTypeDeclaration) {
         return ModelUtil.isObjectType(type) ?
             (isDyn ? objectDynIcon : objectIcon) :
             (ModelUtil.isEnumType(type) ? (isDyn ? enumDynIcon : enumIcon) : (isDyn ? classDynIcon : classIcon));
      }
      else if (ModelUtil.isProperty(type)) {
         Object propType = ModelUtil.getPropertyType(type);
         return lookupUIIcon(propType, isDyn);
      }
      else if (type == Integer.class) {
         return isDyn ? intDynIcon : intIcon;
      }
      else if (type == String.class) {
         return isDyn ? stringDynIcon : stringIcon;
      }
      else if (type == Boolean.class) {
         return isDyn ? booleanDynIcon : booleanIcon;
      }
      else if (DynUtil.isType(type)) {
         return DynUtil.isObjectType(type) ? (isDyn ? objectDynIcon : objectIcon) : (DynUtil.isEnumConstant(type) ? (isDyn ? enumDynIcon : enumIcon) :
                (isDyn ? classDynIcon : classIcon));
      }
      else {
         System.err.println("*** Unrecognized type in lookupUIIcon");
      }
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
