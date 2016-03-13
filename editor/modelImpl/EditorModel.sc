import java.lang.reflect.Field;

import sc.type.IBeanMapper;
import java.util.TreeSet;

import sc.layer.LayeredSystem;

import sc.lang.java.TypeDeclaration;
import sc.lang.java.JavaSemanticNode;
import sc.lang.java.BodyTypeDeclaration;
import sc.lang.java.DeclarationType;
import sc.lang.java.InterfaceDeclaration;
import sc.lang.java.VariableDefinition;
import sc.lang.java.ModelUtil;
import sc.lang.sc.PropertyAssignment;

import sc.lang.IUndoOp;

import sc.parser.ParseUtil;

@sc.obj.Component
EditorModel {
   /** Among the typeNames, set to the type object which has focus */
   Object currentCtxType :=: ctx.currentType;

   currentCtxType =: changeCurrentType(currentCtxType);

   currentProperty =: validateCurrentProperty();

   system = LayeredSystem.getCurrent();

   typeNames =: invalidateModel();
   currentLayer =: invalidateModel();
   mergeLayers =: invalidateModel();
   inherit =: invalidateModel();

   codeFunctions =: invalidateModel();

   /** Set this to true so the command line interpreter and UI do not share the same current type, etc. */
   boolean separateContext = false;

   private EditorContext theSeparateCtx = null;
   private EditorContext getTheSeparateContext() {
      if (theSeparateCtx == null)
         theSeparateCtx = new EditorContext(system);
      return theSeparateCtx;
   }
   ctx := separateContext ? getTheSeparateContext() : system.getDefaultEditorContext();

   boolean modelsValid = true; // start out true so the first invalidate kicks in.... when nothing is selected, we are valid

   importedPropertyType := ctx.getImportedPropertyType(currentProperty);

   currentProperty =: currentPropertyIcon = GlobalResources.lookupUIIcon(currentProperty);


   // When the currentTypeSearch field is changed, this will look for a type matching that pattern, and if found change the current type.  this gets pushed to the client.
   currentTypeSearch =: findCurrentType(currentTypeSearch);

   void invalidateModel() {  // OVERRIDE in your framework to so rebuildModel is run in a doLater
      if (modelValidating) {
          System.out.println("*** Triggered model invalidate when the model was in the midst of validating!");
       }

      if (modelsValid) {
         modelsValid = false;

         DynUtil.invokeLater(new Runnable() {
            public void run() {
               rebuildModel();
               }}, 0);
      }
   }

   boolean modelValidating = false;

   void rebuildModel() {
      if (modelsValid)
         return;

      modelValidating = true;

      if (!triggeredByUndo) {
         boolean typesChanged = !StringUtil.arraysEqual(oldTypeNames,typeNames);

         if (oldTypeNames != null && typesChanged) {
            ctx.addOp(new IUndoOp() {
               String[] prevTypeNames = oldTypeNames;
               String[] newTypeNames = typeNames;

               void undo() {
                  triggeredByUndo = true;
                  typeNames = prevTypeNames;
                  oldTypeNames = newTypeNames;
               }
               void redo() {
                  triggeredByUndo = true;
                  oldTypeNames = prevTypeNames;
                  typeNames = newTypeNames;
               }

            });
         }
      }
      else
         triggeredByUndo = false;

      oldTypeNames = typeNames;

      ArrayList<Layer> newFilteredLayers = new ArrayList<Layer>();
      ArrayList<Layer> newTypeLayers = new ArrayList<Layer>();
      types = new ArrayList<Object>();
      inheritedTypes = new ArrayList<Object>();
      typesPerLayer = new ArrayList<List<Object>>();
      filteredTypes = new ArrayList<Object>();
      filteredTypesByLayer = new LinkedHashMap<String,List<Object>>();

      selectedFileIndex = new LinkedHashMap<String, SelectedFile>();
      selectedFileList = new ArrayList<SelectedFile>();

      for (String typeName:typeNames) {
         boolean isLayerType = false;

         Object type = system.getTypeDeclaration(typeName);
         if (type == null) {
            Layer layer = system.getLayerByTypeName(typeName);
            if (layer == null) {
               System.err.println("*** Can't find type or layer named: " + typeName);
               continue;
            }
            type = layer.model.getModelTypeDeclaration();
            isLayerType = true;
         }

         // Don't try to preserve the current layer when it goes from visible to invisible
         if (currentLayer != null && !currentLayer.matchesFilter(codeTypes, codeFunctions))
            currentLayer = null;

         // Pick the first visible layer in the type... if none are visible, then skip this type
         if (currentLayer == null) {
            if (type instanceof BodyTypeDeclaration) {
               BodyTypeDeclaration btd = (BodyTypeDeclaration) type;
               Layer typeLayer = btd.layer;
               while (typeLayer != null && !typeLayer.matchesFilter(codeTypes, codeFunctions)) {
                  btd = btd.getModifiedType();
                  if (btd == null)
                     break;
                  typeLayer = btd.layer;
               }
               // There is no version of this type in the selected layer
               if (btd == null)
                  continue;
               type = btd;
            }
         }

         Layer typeLayer = type instanceof BodyTypeDeclaration ? ((BodyTypeDeclaration) type).getLayer() : null;

         addLayerType(type, typeLayer, newFilteredLayers, newTypeLayers);

         if (typeLayer != null) {
            List<Layer> transLayers = typeLayer.getTransparentLayers();
            if (transLayers != null) {
               for (int i = 0; i < transLayers.size(); i++) {
                  addLayerType(type, transLayers.get(i), newFilteredLayers, newTypeLayers);
               }
            }
         }

         // Add this root type to the global list of types
         types.add(type);
         inheritedTypes.add(type);

         if (type instanceof TypeDeclaration) {
            TypeDeclaration rootTD = (TypeDeclaration) type;
            BodyTypeDeclaration modType = rootTD.getModifiedType();
            while (modType != null) {
               addLayerType(modType, modType.getLayer(), newFilteredLayers, newTypeLayers);
               modType = modType.getModifiedType();
            }
            if (inherit) {
               Object extType = rootTD.getExtendsTypeDeclaration();
               while (extType != null && extType instanceof BodyTypeDeclaration) {
                  BodyTypeDeclaration eTD = (BodyTypeDeclaration) extType;
                  // Use this method to just add the layer to the layer indexes.  When type is null, no type is added.
                  addLayerType(eTD, eTD.getLayer(), newFilteredLayers, newTypeLayers);

                  BodyTypeDeclaration eTDRoot = eTD.getModifiedByRoot();

                  if (!inheritedTypes.contains(eTDRoot))
                     inheritedTypes.add(eTDRoot);

                  BodyTypeDeclaration extModType;
                  BodyTypeDeclaration mtype = eTD;
                  while ((extModType = mtype.getModifiedType()) != null) {
                     addLayerType(extModType, extModType.getLayer(), newFilteredLayers, newTypeLayers);
                     mtype = extModType;
                  }
                  extType = eTD.getExtendsTypeDeclaration();
               }

               addInterfaceLayerTypes(rootTD, newFilteredLayers, newTypeLayers, inheritedTypes);
            }
         }
      }

      filteredTypeLayers = newFilteredLayers;
      // Make sure this does not change when just currentLayer changes...
      if (typeLayers == null || !typeLayers.equals(newTypeLayers))
         typeLayers = newTypeLayers;

      boolean resetCurrentLayer = true;
      if (typeLayers != null) {
         if (currentLayer != null) {
            if (typeLayers.contains(currentLayer))
               resetCurrentLayer = false;
         }
         if (resetCurrentLayer && typeLayers.size() > 0)
            currentLayer = typeLayers.get(typeLayers.size()-1);
      }

      if (filteredTypes.size() > 0)
         currentType = filteredTypes.get(0);
      else
         currentType = null;

      // Clear out any selected property.
      currentProperty = null;
      currentPropertyType = currentType;
      savedPropertyValue = currentPropertyValue = null;
      savedPropertyOperator = currentPropertyOperator = null;
      currentInstance = null;

      if (currentType != null) {
         currentTypeIsLayer = ModelUtil.isLayerType(currentType);
         if (currentTypeIsLayer)
            currentPackage = currentLayer.packagePrefix;
         else
            currentPackage = ModelUtil.getPackageName(currentType);
      }
      else {
         currentPackage = "";
         currentTypeIsLayer = false;
      }

      if (currentCtxType != currentType)
         currentCtxType = currentType;

      updateCurrentJavaModel();

      ArrayList newVisibleTypes = new ArrayList();
      if (types != null) {
         for (Object type:types) {
            type = processVisibleType(type);
            if (type != null) {
               newVisibleTypes.add(type);
            }
         }
      }
      visibleTypes = newVisibleTypes;

      // Do this at the end in case any of our changes trigger the model
      modelsValid = true;

      modelValidating = false;


      // Send an event so people can listen on this value and update dependent data structures
      Bind.sendEvent(sc.bind.IListener.VALUE_CHANGED, this, null);
   }

   private void addInterfaceLayerTypes(BodyTypeDeclaration rootTD, ArrayList<Layer> newFilteredLayers, ArrayList<Layer> newTypeLayers, List<Object> inheritedTypes) {
      Object[] implTypes = rootTD.getImplementsTypeDeclarations();
      if (implTypes != null) {
         for (Object implTypeObj:implTypes) {
            if (implTypeObj instanceof TypeDeclaration) {
               TypeDeclaration implType = (TypeDeclaration) implTypeObj;

               addLayerType(implType, implType.getLayer(), newFilteredLayers, newTypeLayers);

               BodyTypeDeclaration iTDRoot = implType.getModifiedByRoot();

               if (!inheritedTypes.contains(iTDRoot))
                  inheritedTypes.add(iTDRoot);

               BodyTypeDeclaration extModType;
               BodyTypeDeclaration mtype = implType;
               while ((extModType = mtype.getModifiedType()) != null) {
                  addLayerType(extModType, extModType.getLayer(), newFilteredLayers, newTypeLayers);
                  mtype = extModType;
               }

               addInterfaceLayerTypes(implType, newFilteredLayers, newTypeLayers, inheritedTypes);
            }
         }
      }
   }

   private void addLayerType(Object type, Layer layer, ArrayList<Layer> newFilteredLayers, ArrayList<Layer> newTypeLayers) {
      Layer prevLayer;
      if (type instanceof TypeDeclaration) {
         TypeDeclaration td = (TypeDeclaration) type;

         ParseUtil.initAndStartComponent(td);

         BodyTypeDeclaration prevType = td.getModifiedByType();
         prevLayer = prevType == null ? null : prevType.getLayer();
      }
      else {
         prevLayer = null;
      }

      boolean isTypeLayer = true;

      // When we filter a layer, we remove it from the allLayers attribute as well as not showing any types from it.
      if (layer != null && !layer.matchesFilter(codeTypes, codeFunctions))
         return;

      // Don't show this layer if we have a currentLayer set and depending on the "mergeLayers" flag we should or not
      if (layer != null && currentLayer != null && ((!mergeLayers && currentLayer != layer) || (mergeLayers && currentLayer.getLayerPosition() < layer.getLayerPosition()))) {
         isTypeLayer = false;
      }

      if (isTypeLayer) {
         int layerIx = newFilteredLayers.indexOf(layer);
         List<Object> layerTypes;
         if (layerIx == -1) {
            layerIx = newFilteredLayers.size();
            // Keep layers sorted with null as the very first layer if it is present
            int pos;

            if (layer == null) {
               pos = 0;
            }
            else {
               for (pos = 0; pos < newFilteredLayers.size(); pos++) {
                  Layer cur = newFilteredLayers.get(pos);
                  if (cur != null && cur.layerPosition > layer.layerPosition)
                     break;
               }
            }
            newFilteredLayers.add(pos, layer);
            typesPerLayer.add(pos, layerTypes = new ArrayList<Object>());
         }
         else
            layerTypes = typesPerLayer.get(layerIx);

         // Also associate ths type with its layer
         if (type != null) {
            layerTypes.add(type);

            String typeName = ModelUtil.getTypeName(type);
            List<Object> typeList = filteredTypesByLayer.get(typeName);
            if (typeList == null) {
               typeList = new ArrayList<Object>();
               filteredTypesByLayer.put(typeName, typeList);
            }
            typeList.add(type);

            // If the previous type is null or the previous type's position is outside the selected region, do not include it
            // Otherwise, this type was already added to the global list in a previous layer's type
            if (prevLayer == null || (currentLayer != null && prevLayer.getLayerPosition() > currentLayer.getLayerPosition()))
               filteredTypes.add(type);

            if (type instanceof BodyTypeDeclaration) {
               BodyTypeDeclaration btd = (BodyTypeDeclaration) type;
               JavaModel javaModel = btd.getJavaModel();
               SrcEntry ent = javaModel.getSrcFile();
               if (ent != null) {
                  SelectedFile f = selectedFileIndex.get(ent.absFileName);
                  if (f == null) {
                     f = new SelectedFile();
                     f.file = ent;
                     f.types = new ArrayList<Object>();
                     f.model = javaModel;
                     f.layer = currentLayer;
                     selectedFileIndex.put(ent.absFileName, f);
                     selectedFileList.add(f);
                  }
                  if (!f.types.contains(type))
                     f.types.add(type);
               }
            }
         }
      } 
      int allIx = newTypeLayers.indexOf(layer);
      if (allIx == -1) {
         int i;
         for (i = 0; i < newTypeLayers.size(); i++) {
            if (newTypeLayers.get(i).layerPosition < layer.layerPosition) {
               newTypeLayers.add(i, layer);
               break;
            }
         }
         if (i == newTypeLayers.size())
            newTypeLayers.add(layer);
      }
   }

   private static HashSet<String> filteredProps = new HashSet<String>();
   static {
      filteredProps.add("class");
      filteredProps.add("initState");
   }

   public boolean filteredProperty(Object type, Object p, boolean perLayer) {
      // For now, only StrataCode members
      if (p instanceof java.lang.reflect.Member)
         return true;

      if (p instanceof IBeanMapper && ((IBeanMapper) p).getPropertyMember() instanceof java.lang.reflect.Member)
         return true;

      Object ownerType = ModelUtil.getEnclosingType(p);

      if (type instanceof ClientTypeDeclaration)
         type = ((ClientTypeDeclaration) type).getOriginal();

      // Normally !inherit mode only uses the declared properties.  But for transparent layers we have to get all of them and filter them here
      if (!inherit && !ModelUtil.sameTypes(ownerType, type))
         return true;

      // In threeD view, we don't want to merge the properties as we go up the layer stack unlike form view.
      if (perLayer && ModelUtil.getLayerForType(null, type) != ModelUtil.getLayerForType(null, ownerType))
         return true;

      String pname;
      return p == null || (pname = ModelUtil.getPropertyName(p)).startsWith("_") || filteredProps.contains(pname);
   }

   /** When merging layers we use extendsLayer so that we do not pick up independent layers which which just happen to sit lower in the stack, below the selected layer */
   public boolean currentLayerMatches(Layer layer) {
      if (currentLayer == null)
         return true;
      if (currentLayer.transparentToLayer(layer))
         return true;
      return ((!mergeLayers && currentLayer == layer) || (mergeLayers && (layer == currentLayer || currentLayer.extendsLayer(layer))));
   }

   public Object[] getPropertiesForType(Object type) {
      if (type instanceof ClientTypeDeclaration)
         type = ((ClientTypeDeclaration) type).getOriginal();
      Object[] props;
      if (!mergeLayers) {
         // Transparent layers need to grab all of the properties so we can filter them in the code
         if (!inherit && (currentLayer == null || !currentLayer.transparent))
            props = ModelUtil.getDeclaredPropertiesAndTypes(type, null);
          else
            props = ModelUtil.getPropertiesAndTypes(type, null);
      }
      else {
         if (!inherit && (currentLayer == null || !currentLayer.transparent))
            props = ModelUtil.getDeclaredMergedPropertiesAndTypes(type, null, true);
         else
            props = ModelUtil.getMergedPropertiesAndTypes(type, null);
      }
      return props;
   }

   public Object[] toClientTypeDeclarations(Object[] types) {
      if (types == null)
         return null;
      int i = 0;
      for (Object type:types) {
         if (type instanceof ClientTypeDeclaration)
            continue;
         if (type instanceof BodyTypeDeclaration)
            types[i] = ((BodyTypeDeclaration) type).getClientTypeDeclaration();
      }
      return types;
   }

   String setElementValue(Object type, Object inst, Object prop, String expr, boolean updateInstances, boolean valueIsExpr) {
      if (type instanceof ClientTypeDeclaration)
         type = ((ClientTypeDeclaration) type).getOriginal();
      // The first time they are changing the object in a transparent layer.  We need to create it in this case.
      if (currentLayer != null && currentLayer.transparent && ModelUtil.getLayerForType(null, type) != currentLayer) {
         String typeName = ModelUtil.getTypeName(type);
         type = ctx.addTopLevelType(null, CTypeUtil.getPackageName(typeName), currentLayer, CTypeUtil.getClassName(typeName), null);
         invalidateModel();
      }
      return ctx.setElementValue(type, inst, prop, expr, updateInstances, valueIsExpr);
   }

   String updateCurrentProperty(Object operator, String value) {
      return setElementValue(currentPropertyType, null, currentProperty, operator + value, true, true);
   }

   void validateCurrentProperty() {
      Object prop = currentProperty;
      if (prop == null) {
         currentPropertyType = null;
      }
      else {
         currentPropertyType = ModelUtil.getEnclosingType(prop);
         savedPropertyValue = currentPropertyValue = ctx.propertyValueString(currentType, null, prop);
         savedPropertyOperator = currentPropertyOperator = ModelUtil.getOperator(currentProperty);
         if (savedPropertyOperator == null)
            savedPropertyOperator = currentPropertyOperator = "=";
      }
      propertySelectionChanged();
   }

   // Called when the current JavaModel changes
   private object modelEventListener extends AbstractListener {
      public boolean valueValidated(Object obj, Object prop, Object eventDetail, boolean apply) {
         if (currentType != null && currentProperty != null) {
            savedPropertyValue = currentPropertyValue = ctx.propertyValueString(currentType, null, currentProperty);
            savedPropertyOperator = currentPropertyOperator = ModelUtil.getOperator(currentProperty);
            if (savedPropertyOperator == null)
               savedPropertyOperator = currentPropertyOperator = "=";
         }

         // If the model has changed, the type itself may have changed
         if (currentType instanceof BodyTypeDeclaration) {
            BodyTypeDeclaration typeDecl = (BodyTypeDeclaration) currentType;

            BodyTypeDeclaration newTypeDecl = typeDecl.resolve(false);
            // When the type has changed, update the current model which will trigger the rebuilding of the form
            if (newTypeDecl != typeDecl) {
               currentType = newTypeDecl;

               invalidateModel();

/*
               int i = 0;
               for (Object type:types) {
                  if (type instanceof BodyTypeDeclaration)
                     types.set(i, ((BodyTypeDeclaration) type).resolve(false));
                  i++;
               }
*/
            }
         }

         return true;
      }
   }

   void removeCurrentListener() {
      if (currentJavaModel != null) {
         Bind.removeListener(currentJavaModel, null, modelEventListener, IListener.VALUE_CHANGED);
      }
   }

   void updateCurrentJavaModel() {
      JavaModel newModel = ModelUtil.getJavaModel(currentType);
      if (newModel != currentJavaModel) {
         removeCurrentListener();
         currentJavaModel = newModel;
         if (newModel != null) {
            Bind.addListener(currentJavaModel, null, modelEventListener, IListener.VALUE_CHANGED);
         }
      }
   }

   void changeCurrentType(Object type) {
      super.changeCurrentType(type);

      // Push this back if the change is coming from the editor model side
      if (currentCtxType != type)
         currentCtxType = type;

      updateCurrentJavaModel();
   }

   void clearCurrentType() {
      super.clearCurrentType();
      updateCurrentJavaModel();
   }

   String getPropertySelectionName() {
      if (currentProperty != null) {
         String dynPrefix = ModelUtil.isDynamicProperty(currentProperty) ? " Dynamic" : " Compiled";
         if (currentProperty instanceof VariableDefinition) {
            return dynPrefix + " Field";
         }
         else if (currentProperty instanceof PropertyAssignment) {
            return dynPrefix + " Property Assignment";
         }
         else if (currentProperty instanceof Field)
            return " Native Field";
         else
            return " ???"; // method?
      }
      else
         return null;
   }

   String getTypeSelectionName() {
      if (currentType != null) {
         DeclarationType declType = ModelUtil.getDeclarationType(currentType);
         String name = declType.name;
         String prefix;
         if (currentType instanceof BodyTypeDeclaration) {
            prefix = ModelUtil.isDynamicType(currentType) ? " Dynamic" : " Compiled";
         }
         else
            prefix = " Native";

         return " " + Character.toUpperCase(name.charAt(0)) + name.substring(1);
      }
      else
         return null;
   }

   public void deleteCurrentProperty() {
      if (currentType != null && currentProperty != null && currentType instanceof BodyTypeDeclaration && currentProperty instanceof JavaSemanticNode) {
         ctx.removeProperty((BodyTypeDeclaration) currentType, (JavaSemanticNode) currentProperty, true);
         clearCurrentProperty();
      }
      else
         System.err.println("*** Can't delete current property");
   }

   public void deleteCurrentLayer() {
      if (currentLayer != null) {
         ctx.removeLayer(currentLayer, true);
         clearCurrentType();
      }
      else
         System.err.println("*** no current layer to delete");
   }

   public void deleteCurrentType() {
      if (currentType != null || !(currentType instanceof BodyTypeDeclaration)) {
         ctx.removeType((BodyTypeDeclaration) currentType, true);
         clearCurrentType();
      }
      else
         System.err.println("*** no current type to delete");
   }

   public void deleteCurrentSelection() {
      if (currentProperty != null) {
         deleteCurrentProperty();
      }
      else if (currentTypeIsLayer) {
         deleteCurrentLayer();
      }
      else if (currentType != null) {
         deleteCurrentType();
      }
   }

   public String findCurrentType(String rootName) {
      if (rootName == null)
         return null;

      // First see if they specified the whole name
      BodyTypeDeclaration theType = system.getSrcTypeDeclaration(rootName, null, true);

      if (theType == null) {
         List<BodyTypeDeclaration> types = system.findTypesByRootName(rootName);
         if (types == null || types.size() == 0) {
            return "No types named: " + rootName;
         }
         theType = types.get(0);
      }
      changeCurrentType(theType);
      return null;
   }

   public void commitMemorySessionChanges() {
      ctx.commitMemorySessionChanges();
      invalidateModel();
   }

   void clearCurrentProperty() {
      currentProperty = null;
      currentPropertyType = null;
      currentPropertyValue = null;
      currentPropertyOperator = null;
      currentInstance = null;

      propertySelectionChanged();
   }

   void propertySelectionChanged() {
      // Need to manually change these properties when the current property changes cause rebuildModel does not send the "model changed" event in this case
      if (editSelectionEnabled != getEditableProperty())
         editSelectionEnabled = !editSelectionEnabled;
      Bind.sendDynamicEvent(IListener.VALUE_CHANGED, this, "currentSelectionName");;
   }

   public boolean getEditableProperty() {
      if (currentProperty != null) {
         if (currentProperty instanceof VariableDefinition) {
            return true;
         }
         else if (currentProperty instanceof PropertyAssignment) {
            return true;
         }
         else if (currentProperty instanceof Field)
            return false;
         else
            return false;
      }
      else if (currentTypeIsLayer) {
         return true;
      }
      else if (currentType != null) {
         return true;
      }
      else {
         return false;
      }
   }

   public String addTopLevelType(String mode, String currentPackage, Layer layer, String name, String extType) {
      Object res = ctx.addTopLevelType(mode, currentPackage, layer, name, extType);
      if (res instanceof String)
         return (String) res;

      changeCurrentType(res);

      return null;
   }

   void removeLayers(ArrayList<Layer> layers) {
      ctx.removeLayers(layers);
   }

   ClientTypeDeclaration toClientType(Object type) {
      if (type instanceof BodyTypeDeclaration) {
         if (type instanceof ClientTypeDeclaration)
            return (ClientTypeDeclaration) type;
         return ((BodyTypeDeclaration) type).getClientTypeDeclaration();
      }
      return null;
   }

   BodyTypeDeclaration processVisibleType(Object typeObj) {
      if (typeObj instanceof BodyTypeDeclaration) {
         return toClientType(((BodyTypeDeclaration) typeObj).getDeclarationForLayer(currentLayer, inherit, mergeLayers));
      }
      return null;
   }


   void stop() {
      removeCurrentListener();
   }
}
