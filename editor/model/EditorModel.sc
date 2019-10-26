import sc.bind.Bind;
import sc.sync.SyncManager;
import sc.obj.Sync;
import sc.obj.SyncMode;
import sc.lang.html.Element;
import sc.lang.java.BodyTypeDeclaration;
import sc.lang.InstanceWrapper;
import sc.lang.java.ModelUtil;
import sc.lang.java.AbstractMethodDefinition;
import sc.lang.java.Parameter;
import sc.type.IResponseListener;

import sc.layer.CodeType;

/** 
   The main view model object for viewing and editing of the program model or instances.  It exposes
   the current selection and provides access to the currently selected property, types and layers. 
   */
@sc.obj.Component
class EditorModel implements sc.bind.IChangeable, sc.dyn.IDynListener {
   /** Specifies the current LIST of types for the model */
   String[] typeNames = new String[0];

   List<InstanceWrapper> selectedInstances = null;

   String[] oldTypeNames; // Last time model was rebuilt

   String createModeTypeName; // Set when in createMode and a type is selected

   LayeredSystem system;

   int windowState = 0; // 0 = iconified, 1 = open, 2 = maximized

   /** Set to the current layer */
   @Bindable(crossScope=true, sameValueCheck=true)
   Layer currentLayer :=: ctx.currentLayer;

   /** The current type */
   Object currentType;

   /** The current Java model for the type */
   JavaModel currentJavaModel;

   /** When a property has focus, set to the property */
   Object currentProperty;

   UIIcon currentPropertyIcon;

   /** The enclosing type of the current property */
   Object currentPropertyType;

   /** The name of currentProperty */
   String currentPropertyName;

   /** The currentPropertyType filtered based on the imported type name */
   String importedPropertyType;

   /** Set this to search for a current type - if found, it is set and currentType is changed */
   String currentTypeSearch;

   /** If there's a selected instance, the instance */
   Object currentInstance;

   /** Stores info about the currentInstance - used in particular in the 'pendingCreate' mode - before the instance has been created */
   InstanceWrapper currentWrapper;

   /** When the add/minus button is pressed, this gets toggled */
   boolean createMode = false;

   CreateMode currentCreateMode = CreateMode.Instance;

   String currentPropertyOperator;

   String savedPropertyOperator;

   /** Property value bound to the current text field - updated live */
   String currentPropertyValue;

   /** Set to the currently selected package, or if a type is selected, the package of that type */
   String currentPackage;

   /** Last known value from the model for the property */
   String savedPropertyValue;

   /** Number of layers to merge starting from the current type's layer */
   int mergeLayerCt = 0;

   /** Set to true when the current type is a layer */
   boolean currentTypeIsLayer;

   /** Number of the current type's base type's properties to include in the current view */
   int inheritTypeCt = 0;

   /** Set to true after the user has started a createInstance but not yet completed the create */
   boolean pendingCreate = false;

   /** Set to the list of ConstructorProperties or regular properties when pendingCreate = true if there are required params to build the new instance */
   @Sync(syncMode=SyncMode.Disabled)
   List<ConstructorProperty> constructorProps = null;

   /** Set to an error string to display when pendingCreate = true and missing required values are present */
   String pendingCreateError = null;

   /** Incremented each time the selection changes so we can update any one who depends on the selection from a central event. */
   int selectionChanged = 0;

   /** Incremented each time a new instance is selected with the same type */
   int newInstSelected = 0;

   /** Incremented each time the type remains the same but the instance mode changes (meaning we need to rebuild the editor lists) */
   int instanceModeChanged = 0;

   /** Set to true when changes have been made to source files in the current runtime which require a process restart. */
   boolean staleCompiledModel;

   /** True when we the current property is an editable one. */
   boolean editSelectionEnabled = false;

   /** List of type names that can be created from the 'Add' panel. */
   String[] currentInstTypeNames;

   /** Generated values, kept in sync when you change typeNames and currentLayer */
   ArrayList<Object> types;                 // The current list of just the selected types
   ArrayList<Object> inheritedTypes;        // Like types only includes any inherited types as well when inherit is true
   @Sync(syncMode=SyncMode.Disabled)
   ArrayList<Object> filteredTypes;         // The merged list of the most specific type in the current selected set of types/layers
   ArrayList<Layer> typeLayers;             // The list of layers which define the types
   @Sync(syncMode=SyncMode.Disabled)
   ArrayList<Layer> filteredTypeLayers;     // The list of layers which define the types based on currentLayer/mergeLayers flags - used for 3d view
   @Sync(syncMode=SyncMode.Disabled)
   ArrayList<List<Object>> typesPerLayer;   // For each layer in the current set, the set of types in this layer - used for 3d view
   @Sync(syncMode=SyncMode.Disabled)
   Map<String, List<Object>> filteredTypesByLayer;   // For each selected type, the list of types for each selected layer - used for 3d view
   ArrayList<Object> visibleTypes = new ArrayList<Object>();     // The list of types used to create the form view - removes types filtered by the merge and inherited flags

   @sc.obj.Constant
   static List<String> operatorList = {"=", ":=", "=:", ":=:"};

   @Sync(onDemand=true)
   static class SelectedFile {
      SrcEntry file;
      List<Object> types;
      JavaModel model;
      Layer layer; // Layer where this file was selected.  if the layer is transparent, it may not be the same as the model's layer

      Layer getModelLayer() {
         return model.layer;
      }
   }

   LinkedHashMap<String, SelectedFile> selectedFileIndex; // Groups selected, filtered types by the files they live in for the code view
   ArrayList<SelectedFile> selectedFileList;

   ArrayList<CodeType> codeTypes = new ArrayList(CodeType.allSet);

   @Bindable(sameValueCheck=true)
   EditorContext ctx;

   boolean triggeredByUndo; // When a type change occurs because of an undo operation we do not want to record that op in the redo list again.

   @Sync(syncMode=SyncMode.Disabled)
   int refreshInstancesCt = 0;
   @Sync(syncMode=SyncMode.Disabled)
   boolean refreshInstancesValid = true;

   void init() {
      SyncManager.initStandardTypes();
      DynUtil.addDynListener(this);
   }

   boolean isTypeNameSelected(String typeName) {
      if (typeName == null)
         return false;

      // When an instance is selected, it's type is not
      if (selectedInstances != null && selectedInstances.size() > 0)
         return false;

      for (String tn:typeNames)
         if (tn.equals(typeName))
            return true;

      return false;
   }

   boolean isCreateModeTypeNameSelected(String typeName) {
      if (createModeTypeName == null)
         return false;

      return createModeTypeName.equals(typeName);
   }

   void changeCurrentType(Object type, Object inst, InstanceWrapper wrapper) {
      if (type == currentType && inst == currentInstance && wrapper == currentWrapper)
         return;

      String newTypeName = null;
      if (type != null) {
         newTypeName = ModelUtil.getTypeName(type);
         String[] newTypeNames = new String[1];
         newTypeNames[0] = newTypeName;
         typeNames = newTypeNames;
      }
      else
         typeNames = new String[0];

      currentType = type;
      currentInstance = inst;
      // Previously used to pop/push current type here
      if (type != null) {
         List<InstanceWrapper> selInstances = new ArrayList<InstanceWrapper>(1);
         if (wrapper == null)
            wrapper = new InstanceWrapper(ctx, inst, newTypeName, null, false);
         selInstances.add(wrapper);
         selectedInstances = selInstances;
         currentWrapper = wrapper;
      }
      else {
         selectedInstances = null;
         currentWrapper = null;
         selectedInstances = null;
      }
      selectionChanged++;
      if (currentWrapper == null)
         pendingCreate = false;
      else
         pendingCreate = currentWrapper.pendingCreate;
   }

   void clearCurrentType() {
      typeNames = new String[0];
      currentType = null;
      currentInstance = null;
      currentWrapper = null;
      pendingCreate = false;
      pendingCreateError = null;
   }

   void changeCurrentTypeName(String typeName) {
      String[] newTypeNames = new String[1];
      newTypeNames[0] = typeName;
      typeNames = newTypeNames;
   }

   String getPropertySelectionName() {
      return " Property";
   }

   String getTypeSelectionName() {
      return DynUtil.isObject(currentType) ? " Object" : " Class";
   }

   String getCurrentSelectionName() {
      if (currentProperty != null) {
         return getPropertySelectionName();
      }
      else if (currentTypeIsLayer) {
         if (currentLayer.dynamic)
            return " Dynamic Layer";
         else
            return " Compiled Layer";

      }
      else if (currentType != null)
         return getTypeSelectionName();
      else
         return null;
   }

   // These should all be removed when remote methods are implemented.
   void deleteCurrentProperty() {
      System.err.println("*** ERROR: remote method not implemented");
   }

   void deleteCurrentLayer() {
      System.err.println("*** ERROR: remote method not implemented");
   }

   void deleteCurrentType() {
      System.err.println("*** ERROR: remote method not implemented");
   }

   void removeLayers(ArrayList<String> layers) {
      System.err.println("*** Implement as a remote method");
   }

   boolean getDebugBindingEnabled() {
      return Bind.trace;
   }

   void setDebugBindingEnabled(boolean de) {
      if (system != null && system.options != null)
         system.options.verbose = de;
      Bind.trace = de;
   }

   void toggleDebugBindingEnabled() {
      setDebugBindingEnabled(!getDebugBindingEnabled());
   }

   boolean getDebugHTMLEnabled() {
      return Element.trace;
   }

   void setDebugHTMLEnabled(boolean de) {
      Element.trace = de;
   }

   void toggleDebugHTMLEnabled() {
      setDebugHTMLEnabled(!getDebugHTMLEnabled());
   }

   boolean getDebugSyncEnabled() {
      return SyncManager.trace;
   }

   void setDebugSyncEnabled(boolean de) {
      SyncManager.trace = de;
   }

   void toggleDebugSyncEnabled() {
      setDebugSyncEnabled(!getDebugSyncEnabled());
   }

   abstract Object[] getPropertiesForType(Object type, sc.type.IResponseListener listener);

   boolean enableUpdateProperty := !DynUtil.equalObjects(currentPropertyValue, savedPropertyValue) ||
                                   !DynUtil.equalObjects(currentPropertyOperator, savedPropertyOperator); 

   //abstract String setElementValue(Object type, Object inst, Object prop, String expr, boolean updateInstances, boolean valueIsExpr);

   public boolean filteredProperty(Object type, Object prop, boolean perLayer, boolean instanceMode) {
      if (instanceMode) {
         if (prop instanceof IVariableInitializer) {
            IVariableInitializer varInit = (IVariableInitializer) prop;
            String opStr = varInit.getOperatorStr();
            if (opStr != null && opStr.equals("=:"))
               return true;
         }
      }
      return false;
   }

   public static boolean isConstantProperty(Object prop) {
      if (prop == null)
         return true;
      if (prop instanceof CustomProperty)
         return ((CustomProperty) prop).isConstant();
      return ModelUtil.hasAnnotation(prop, "sc.obj.Constant") || ModelUtil.hasModifier(prop, "final");
   }

   public static boolean isSettableFromString(Object propC, Object propType) {
      if (!isConstantProperty(propC)) {
         if (propC instanceof CustomProperty)
            return ((CustomProperty) propC).isSettableFromString(propType);
         return sc.type.RTypeUtil.canConvertTypeFromString(propType);
      }
      return false;
   }

   /** When merging layers we use extendsLayer so that we do not pick up independent layers which which just happen to sit lower in the stack, below the selected layer */
   public boolean currentLayerMatches(Layer layer) {
      if (currentLayer == null)
         return true;
      if (ctx.currentLayers.contains(layer))
         return true;
      return false;
      //return ((!mergeLayers && currentLayer == layer) || (mergeLayers && (layer == currentLayer || currentLayer.extendsLayer(layer))));
   }

   BodyTypeDeclaration processVisibleType(Object typeObj) {
      if (typeObj instanceof BodyTypeDeclaration) {
         return (BodyTypeDeclaration) typeObj;
      }
      return null;
   }

   static String getDisplayNameAnnotation(Object typeOrProp) {
      String name = (String) ModelUtil.getAnnotationValue(typeOrProp, "sc.obj.EditorSettings", "displayName");
      if (name != null && name.length() > 0)
         return name;
      return null;
   }

   static String getPropertyName(Object prop) {
      if (prop == null)
         return ("*** null property");
      if (prop instanceof CustomProperty)
         return ((CustomProperty) prop).name;
      String name = getDisplayNameAnnotation(prop);
      if (name != null)
         return name;
      return ModelUtil.getPropertyName(prop);
   }

   static String getClassDisplayName(Object type) {
      String name = getDisplayNameAnnotation(type);
      if (name != null)
         return name;
      return ModelUtil.getClassName(type);
   }

   boolean isVisible(Object prop) {
      if (prop instanceof CustomProperty)
         return true;
      Boolean vis = (Boolean) ModelUtil.getAnnotationValue(prop, "sc.obj.EditorSettings", "visible");
      if (vis != null && !vis)
         return false;
      return true;
   }

   boolean isReferenceType(Object type) {
      if (ModelUtil.isObjectType(type))
         return true;
      if (ModelUtil.hasAnnotation(type, "sc.obj.ValueObject"))
         return false;
      return true;
   }

   void changeFocus(Object newProp, Object newInst) {
      this.currentProperty = newProp;
      this.currentInstance = newInst;
   }

   void cancelPropertyEdit() {
      /* to reset back to the original values instead of just clearing the current property like we do now
      currentPropertyValue = savedPropertyValue;
      currentPropertyOperator = savedPropertyOperator;
      */
      currentProperty = null;
   }

   void instanceAdded(Object inst) {
      refreshInstances();
   }
   void instanceRemoved(Object inst) {
      if (inst == currentInstance)
         changeCurrentType(currentType, null, null);
      refreshInstances();
   }
   void refreshInstances() {
      if (refreshInstancesValid) {
         refreshInstancesValid = false;
         DynUtil.invokeLater(new Runnable() {
            void run() {
               refreshInstancesValid = true;
               refreshInstancesCt++;
            }
         }, 300);
      }
   }

   void refreshInstancesCheck(Object obj) {
      if (refreshInstancesValid) // This makes sure we do not refresh the bindings unless the instances were valid when this editor was created
         Bind.refreshBinding(obj, "instancesOfType");
   }

   static Layer getLayerForMember(Object prop) {
      if (prop == null || prop instanceof CustomProperty)
         return null;
      return ModelUtil.getLayerForMember(null, prop);
   }

   boolean getPropertyInherited(Object prop, Layer layer) {
      if (prop == null) // list element
         return false;
      if (prop instanceof CustomProperty)
         return false;
      Layer memberLayer = ModelUtil.getLayerForMember(null, prop);
      return memberLayer != layer;
   }

   static Object getPropertyType(Object prop) {
      if (prop instanceof CustomProperty)
         return ((CustomProperty) prop).propertyType;
      return ModelUtil.getPropertyType(prop);
   }

   Object fetchInstanceType(Object inst) {
      Object instType = DynUtil.getType(inst);
      instType = ModelUtil.resolveSrcTypeDeclaration(system, instType);
      return instType;
   }

   void changeCodeTypes(EnumSet<CodeType> newSet) {
      codeTypes = new ArrayList<CodeType>(newSet);
   }

   List<ConstructorProperty> getConstructorProperties(InstanceWrapper wrapper, TypeDeclaration typeDecl) {
      AbstractMethodDefinition createMeth = typeDecl.getEditorCreateMethod();
      ArrayList<ConstructorProperty> props = new ArrayList<ConstructorProperty>();
      if (createMeth != null) {
         List<Parameter> paramList = createMeth.getParameterList();
         String constrParamStr = typeDecl.getConstructorParamNames();
         String[] constrParamNames = constrParamStr != null ? constrParamStr.split(",") : null;
         if (paramList != null) {
            int nps = paramList.size();
            if (constrParamNames != null && constrParamNames.length != nps) {
               System.err.println("*** Ignoring EditorCreate.constructorParamNames for createMethod: mismatching parameters");
               constrParamNames = null;
            }
            for (int pix = 0; pix < nps; pix++) {
               Parameter param = paramList.get(pix);
               String paramName = constrParamNames != null ? constrParamNames[pix].trim() : param.variableName;
               Object paramType = DynUtil.findType(param.parameterTypeName);
               if (paramType == null) {
                  System.err.println("*** Unable to find parameter type: " + param.parameterTypeName);
               }
               props.add(new ConstructorProperty(paramName, paramType, null, wrapper));
            }
         }
         return props;
      }
      return null;
   }

   public String getLayerPrefixForLayerName(String layerName) {
      Layer layer = system.getLayerByDirName(layerName);
      if (layer != null)
         return "Package: " + layer.packagePrefix +
                (layer.packagePrefix != null && layer.packagePrefix.length() > 0 ? "." : "");
      return "";
   }

   public List<String> getMatchingLayerNamesForType(String typeName) {
      if (system.layers == null)
         return java.util.Arrays.asList(new String[]{"No layers loaded"});
      ArrayList<String> res = new ArrayList<String>();
      for (Layer layer:system.layers) {
         if (typeName == null || typeName.length() == 0 ||
             layer.packagePrefix == null || typeName.startsWith(layer.packagePrefix + "."))
            res.add(layer.getLayerName());
      }
      return res;
   }

   BodyTypeDeclaration getOrFetchTypeByName(String typeName, IResponseListener listener) {
      BodyTypeDeclaration type = system.getSrcTypeDeclaration(typeName, null);
      if (type instanceof BodyTypeDeclaration) {
         return (BodyTypeDeclaration) type;
      }
      else {
         system.fetchRemoteTypeDeclaration(typeName, listener);
      }
      return null;
   }

}
