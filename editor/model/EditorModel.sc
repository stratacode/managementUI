import sc.bind.Bind;
import sc.sync.SyncManager;
import sc.obj.Sync;
import sc.obj.SyncMode;
import sc.lang.html.Element;
import sc.lang.java.BodyTypeDeclaration;
import sc.lang.InstanceWrapper;
import sc.lang.java.ModelUtil;

/** 
   The main view model object for viewing and editing of the program model.  It exposes
   the current selection and provides access to the currently selected property, types and layers. 
   */
class EditorModel implements sc.bind.IChangeable {
   /** Specifies the list of types for the model */
   String[] typeNames = new String[0];

   List<InstanceWrapper> selectedInstances = null;

   String[] oldTypeNames; // Last time model was rebuilt

   String createModeTypeName; // Set when in createMode and a type is selected

   LayeredSystem system;

   int windowState = 0; // 0 = iconfied, 1 = open, 2 = maximized

   /** Set to the current layer */
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

   /** The currentPropertyType filtered based on the imported type name */
   String importedPropertyType;

   /** Set this to search for a current type - if found, it is set and currentType is changed */
   String currentTypeSearch;

   /** If there's a selected instance, the instance */
   Object currentInstance;

   /** When the add/minus button is pressed, this gets toggled */
   boolean createMode = false;

   String currentPropertyOperator;

   String savedPropertyOperator;

   /** Property value bound to the current text field - updated live */
   String currentPropertyValue;

   /** Set to the currently selected package, or if a type is selected, the package of that type */
   String currentPackage;

   /** Last known value from the model for the property */
   String savedPropertyValue;

   /** Set to false if we only use types from the current layer.  Otherwise, merge all layers behind the current layer */
   boolean mergeLayers = false;

   /** Set to true when the current type is a layer */
   boolean currentTypeIsLayer;

   /** Set to true for an object or class to show properties from its extends class */
   boolean inherit = false;

   /** Incremented each time the selection changes so we can update any one who depends on the selection from a central event. */
   int selectionChanged = 0;

   /** Incremented each time a new instance is selected with the same type */
   int newInstSelected = 0;

   /** Set to true when changes have been made to source files in the current runtime which require a process restart. */
   boolean staleCompiledModel;

   /** True when we the current property is an editable one. */
   boolean editSelectionEnabled = false;

   /** Generated values, kept in sync when you change typeNames and currentLayer */
   ArrayList<Object> types;                 // The global list of just the selected types
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
   ArrayList<Object> visibleTypes = new ArrayList<Object>();     // Used in form view - filters the set of types by the merge and inherited

   class SelectedFile {
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

   ArrayList<CodeFunction> codeFunctions = new ArrayList(EnumSet.allOf(CodeFunction.class));

   EditorContext ctx;

   boolean triggeredByUndo; // When a type change occurs because of an undo operation we do not want to record that op in the redo list again.

   void changeCodeFunctions(EnumSet<CodeFunction> cfs) {
      codeFunctions = new ArrayList(cfs);
   }

   void updateCodeFunction(CodeFunction cf, boolean add) {
      if (add) {
         if (!codeFunctions.contains(cf))
            codeFunctions.add(cf);
      }
      else
         codeFunctions.remove(cf);
   }

   boolean isTypeNameSelected(String typeName) {
      if (typeName == null)
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

   void changeCurrentType(Object type, Object inst) {
      if (type == null || type == currentType)
         return;

      String[] newTypeNames = new String[1];
      String newTypeName = DynUtil.getTypeName(type, true);
      newTypeNames[0] = newTypeName;
      typeNames = newTypeNames;

      currentType = type;
      currentInstance = inst;
      List<InstanceWrapper> selInstances = new ArrayList<InstanceWrapper>(1);
      selInstances.add(new InstanceWrapper(ctx, inst, newTypeName));
      selectedInstances = selInstances;

      selectionChanged++;
   }

   void clearCurrentType() {
      typeNames = new String[0];
      currentType = null;
      currentInstance = null;
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

   void removeLayers(ArrayList<Layer> layers) {
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

   abstract Object[] getPropertiesForType(Object type);

   boolean enableUpdateProperty := !DynUtil.equalObjects(currentPropertyValue, savedPropertyValue) ||
                                   !DynUtil.equalObjects(currentPropertyOperator, savedPropertyOperator); 

   //abstract String setElementValue(Object type, Object inst, Object prop, String expr, boolean updateInstances, boolean valueIsExpr);


   public boolean filteredProperty(Object type, Object p, boolean perLayer) {
      return false;
   }

   /** When merging layers we use extendsLayer so that we do not pick up independent layers which which just happen to sit lower in the stack, below the selected layer */
   public boolean currentLayerMatches(Layer layer) {
      if (currentLayer == null)
         return true;
      if (currentLayer.transparentToLayer(layer))
         return true;
      return ((!mergeLayers && currentLayer == layer) || (mergeLayers && (layer == currentLayer || currentLayer.extendsLayer(layer))));
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
      Boolean vis = (Boolean) ModelUtil.getAnnotationValue(prop, "sc.obj.EditorSettings", "visible");
      if (vis != null && !vis)
         return false;
      return true;
   }
}
