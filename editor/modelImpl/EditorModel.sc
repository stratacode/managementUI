import java.lang.reflect.Field;

import sc.type.IBeanMapper;
import java.util.TreeSet;
import java.util.TreeMap;

import sc.layer.LayeredSystem;

import sc.lang.java.TypeDeclaration;
import sc.lang.java.JavaSemanticNode;
import sc.lang.java.BodyTypeDeclaration;
import sc.lang.java.DeclarationType;
import sc.lang.java.InterfaceDeclaration;
import sc.lang.java.VariableDefinition;
import sc.lang.java.ConstructorDefinition;
import sc.lang.java.ModelUtil;
import sc.lang.JavaLanguage;
import sc.lang.sc.PropertyAssignment;

import sc.lang.IUndoOp;
import sc.type.Type;

import sc.parser.ParseUtil;

EditorModel {
   /** Among the typeNames, set to the "currentCtxType" - i.e. the type which has focus. */
   @Bindable(crossScope=true)
   BodyTypeDeclaration currentCtxType := ctx.currentType;

   override @Bindable(crossScope=true)
   currentCtxType =: !pendingCreate ? changeCurrentType(currentCtxType, ctx.currentObject, null) : null;

   currentProperty =: validateCurrentProperty();

   system = LayeredSystem.getCurrent();

   typeNames =: invalidateModel();
   currentLayer =: invalidateModel();
   mergeLayers =: invalidateModel();
   inherit =: invalidateModel();

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
         return; // in rebuildModel, we change currentLayer which may call this when we are in the midst of updating the model so just ignore it
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

   // TODO: rename to refreshModel?
   void rebuildModel() {
      if (modelsValid)
         return;

      modelValidating = true;

      //System.out.println("*** in refreshModel: " + StringUtil.arrayToString(oldTypeNames) + " -> " + StringUtil.arrayToString(typeNames) + " on: " + sc.type.PTypeUtil.getThreadName());

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
         if (currentLayer != null && !currentLayer.matchesFilter(codeTypes))
            currentLayer = null;

         // Pick the first visible layer in the type... if none are visible, then skip this type
         if (currentLayer == null) {
            if (type instanceof BodyTypeDeclaration) {
               BodyTypeDeclaration btd = (BodyTypeDeclaration) type;
               Layer typeLayer = btd.layer;
               while (typeLayer != null && !typeLayer.matchesFilter(codeTypes)) {
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

         // Going to walk the type hierarchy twice - the first time to gather the set of layers in the type
         // the second time to compute the visible set of types once we've decided whether or not to reset
         // the current layer.
         addNewTypeLayer(typeLayer, newTypeLayers);
         if (typeLayer != null) {
            List<Layer> transLayers = typeLayer.getTransparentLayers();
            if (transLayers != null) {
               for (int i = 0; i < transLayers.size(); i++) {
                  addNewTypeLayer(transLayers.get(i), newTypeLayers);
               }
            }
         }

         if (type instanceof TypeDeclaration) {
            TypeDeclaration rootTD = (TypeDeclaration) type;
            BodyTypeDeclaration modType = rootTD.getModifiedType();
            while (modType != null) {
               addNewTypeLayer(modType.getLayer(), newTypeLayers);
               modType = modType.getModifiedType();
            }
            if (inherit) {
               Object extType = rootTD.getExtendsTypeDeclaration();
               while (extType != null && extType instanceof BodyTypeDeclaration) {
                  BodyTypeDeclaration eTD = (BodyTypeDeclaration) extType;
                  // Use this method to just add the layer to the layer indexes.  When type is null, no type is added.
                  addNewTypeLayer(eTD.getLayer(), newTypeLayers);

                  BodyTypeDeclaration eTDRoot = eTD.getModifiedByRoot();

                  BodyTypeDeclaration extModType;
                  BodyTypeDeclaration mtype = eTD;
                  while ((extModType = mtype.getModifiedType()) != null) {
                     addNewTypeLayer(extModType.getLayer(), newTypeLayers);
                     mtype = extModType;
                  }
                  extType = eTD.getExtendsTypeDeclaration();
               }

               addInterfaceNewTypeLayers(rootTD, newTypeLayers);
            }
         }

         // Now that we have the newTypeLayers, we need to determine if we are going to reset the
         // currentLayer or not. If this type does not overlap with the current layer, we'll do the
         // reset. Once we do that, we are ready to call addLayerType which computes the filteredTypes
         // used to set the currentType.
         boolean resetCurrentLayer = true;
         if (newTypeLayers != null) {
            if (currentLayer != null) {
               if (newTypeLayers.contains(currentLayer))
                  resetCurrentLayer = false;
            }
            if (resetCurrentLayer && newTypeLayers.size() > 0)
               currentLayer = newTypeLayers.get(newTypeLayers.size()-1);
         }

         /** --- */

         addLayerType(type, typeLayer, newFilteredLayers);

         if (typeLayer != null) {
            List<Layer> transLayers = typeLayer.getTransparentLayers();
            if (transLayers != null) {
               for (int i = 0; i < transLayers.size(); i++) {
                  addLayerType(type, transLayers.get(i), newFilteredLayers);
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
               addLayerType(modType, modType.getLayer(), newFilteredLayers);
               modType = modType.getModifiedType();
            }
            if (inherit) {
               Object extType = rootTD.getExtendsTypeDeclaration();
               while (extType != null && extType instanceof BodyTypeDeclaration) {
                  BodyTypeDeclaration eTD = (BodyTypeDeclaration) extType;
                  // Use this method to just add the layer to the layer indexes.  When type is null, no type is added.
                  addLayerType(eTD, eTD.getLayer(), newFilteredLayers);

                  BodyTypeDeclaration eTDRoot = eTD.getModifiedByRoot();

                  if (!inheritedTypes.contains(eTDRoot))
                     inheritedTypes.add(eTDRoot);

                  BodyTypeDeclaration extModType;
                  BodyTypeDeclaration mtype = eTD;
                  while ((extModType = mtype.getModifiedType()) != null) {
                     addLayerType(extModType, extModType.getLayer(), newFilteredLayers);
                     mtype = extModType;
                  }
                  extType = eTD.getExtendsTypeDeclaration();
               }

               addInterfaceLayerTypes(rootTD, newFilteredLayers, inheritedTypes);
            }
         }
      }

      filteredTypeLayers = newFilteredLayers;
      // Make sure this does not change when just currentLayer changes...
      if (typeLayers == null || !typeLayers.equals(newTypeLayers))
         typeLayers = newTypeLayers;

      Object filteredType;

      if (filteredTypes.size() > 0)
         filteredType = filteredTypes.get(0);
      else
         filteredType = null;

      if (filteredType instanceof BodyTypeDeclaration) {
         Object ctxCurrentType = ctx.currentType;
         Layer layer = ctxCurrentType == null ? null : ModelUtil.getLayerForType(system, ctxCurrentType);
         if (ctxCurrentType == null || layer == null || !newFilteredLayers.contains(layer)) {
            if (currentInstance != null)
               ctx.setDefaultCurrentObj((BodyTypeDeclaration) filteredType, currentInstance);
            else if (ctxCurrentType == null || !ModelUtil.isAssignableFrom(filteredType, ctxCurrentType))
               ctx.currentType = (BodyTypeDeclaration) filteredType;
            else {
               // there was a more specific type in the context so we'll use that here
            }
         }
      }
      // else - we are not updating ctx.currentType here - so these two are not in sync when it's a compiled class or the matched type is not in a visible layer.  TODO: not sure this is right.

      currentType = filteredType;

// Clear out any selected property.
      currentProperty = null;
      currentPropertyType = currentType;
      savedPropertyValue = currentPropertyValue = null;
      savedPropertyOperator = currentPropertyOperator = null;

      if (currentType != null) {
         currentTypeIsLayer = ModelUtil.isLayerType(currentType);
         if (currentTypeIsLayer)
            currentPackage = currentLayer.packagePrefix;
         else
            currentPackage = ModelUtil.getPackageName(currentType);
         if (currentInstance != null && !ModelUtil.isInstance(currentType, currentInstance))
            currentInstance = null;
      }
      else {
         currentPackage = "";
         currentTypeIsLayer = false;
         currentInstance = null;
      }

      ArrayList newVisibleTypes = new ArrayList();
      if (types != null) {
         for (Object type:types) {
            type = processVisibleType(type);
            if (type != null) {
               newVisibleTypes.add(type);
            }
         }
      }
      setVisibleTypesNoEvent(newVisibleTypes);

      // Do this at the end in case any of our changes trigger the model
      modelsValid = true;

      modelValidating = false;

      updateCurrentJavaModel();

      //System.out.println("*** finished in - refreshModel: " + StringUtil.arrayToString(oldTypeNames) + " -> " + StringUtil.arrayToString(typeNames) + " currentType: " + currentType + " thread: " + sc.type.PTypeUtil.getThreadName());

      // Send an event so people can listen on this value and update dependent data structures, but wait until we've
      // updated the model
      Bind.sendEvent(sc.bind.IListener.VALUE_CHANGED, this, "visibleTypes");
      Bind.sendEvent(sc.bind.IListener.VALUE_CHANGED, this, null);

      //System.out.println("*** finished sending events in - refreshModel: " + StringUtil.arrayToString(oldTypeNames) + " -> " + StringUtil.arrayToString(typeNames) + " currentType: " + currentType + " thread: " + sc.type.PTypeUtil.getThreadName());
   }

   @sc.obj.ManualGetSet
   private void setVisibleTypesNoEvent(ArrayList<Object> visTypes) {
      visibleTypes = visTypes;
   }

   private void addInterfaceLayerTypes(BodyTypeDeclaration rootTD, ArrayList<Layer> newFilteredLayers, List<Object> inheritedTypes) {
      Object[] implTypes = rootTD.getImplementsTypeDeclarations();
      if (implTypes != null) {
         for (Object implTypeObj:implTypes) {
            if (implTypeObj instanceof TypeDeclaration) {
               TypeDeclaration implType = (TypeDeclaration) implTypeObj;

               addLayerType(implType, implType.getLayer(), newFilteredLayers);

               BodyTypeDeclaration iTDRoot = implType.getModifiedByRoot();

               if (!inheritedTypes.contains(iTDRoot))
                  inheritedTypes.add(iTDRoot);

               BodyTypeDeclaration extModType;
               BodyTypeDeclaration mtype = implType;
               while ((extModType = mtype.getModifiedType()) != null) {
                  addLayerType(extModType, extModType.getLayer(), newFilteredLayers);
                  mtype = extModType;
               }

               addInterfaceLayerTypes(implType, newFilteredLayers, inheritedTypes);
            }
         }
      }
   }

   private void addLayerType(Object type, Layer layer, ArrayList<Layer> newFilteredLayers) {
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
      if (layer != null && !layer.matchesFilter(codeTypes))
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

               // We want to record instances for this type by default since we are manipulating it from the management UI
               btd.liveDynType = true;

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
   }

   private void addInterfaceNewTypeLayers(BodyTypeDeclaration rootTD, ArrayList<Layer> newTypeLayers) {
      Object[] implTypes = rootTD.getImplementsTypeDeclarations();
      if (implTypes != null) {
         for (Object implTypeObj:implTypes) {
            if (implTypeObj instanceof TypeDeclaration) {
               TypeDeclaration implType = (TypeDeclaration) implTypeObj;

               addNewTypeLayer(implType.getLayer(), newTypeLayers);

               BodyTypeDeclaration iTDRoot = implType.getModifiedByRoot();

               BodyTypeDeclaration extModType;
               BodyTypeDeclaration mtype = implType;
               while ((extModType = mtype.getModifiedType()) != null) {
                  addNewTypeLayer(extModType.getLayer(), newTypeLayers);
                  mtype = extModType;
               }

               addInterfaceNewTypeLayers(implType, newTypeLayers);
            }
         }
      }
   }

   private static void addNewTypeLayer(Layer layer, List<Layer> newTypeLayers) {
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
      filteredProps.add("serialVersionUID");
   }

   public boolean filteredProperty(Object type, Object p, boolean perLayer, boolean instanceMode) {
      if (super.filteredProperty(type, p, perLayer, instanceMode))
         return true;

      if (!instanceMode) {
         // For now, only StrataCode members
         if (p instanceof java.lang.reflect.Member)
            return true;

         if (p instanceof IBeanMapper && ((IBeanMapper) p).getPropertyMember() instanceof java.lang.reflect.Member)
            return true;
      }

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

   public Object[] getPropertiesForType(Object type) {
      if (type instanceof ClientTypeDeclaration)
         type = ((ClientTypeDeclaration) type).getOriginal();
      Object[] props;
      if (!mergeLayers) {
         // Transparent layers need to grab all of the properties so we can filter them in the code
         if (!inherit && (currentLayer == null || !currentLayer.transparent))
            props = ModelUtil.getDeclaredPropertiesAndTypes(type, "public", system);
          else
            props = ModelUtil.getPropertiesAndTypes(type, "public");
      }
      else {
         if (!inherit && (currentLayer == null || !currentLayer.transparent))
            props = ModelUtil.getDeclaredMergedPropertiesAndTypes(type, "public", true);
         else
            props = ModelUtil.getMergedPropertiesAndTypes(type, "public", system);
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

   String setElementValue(Object type, Object inst, Object prop, String expr, boolean updateType, boolean updateInstances, boolean valueIsExpr) {
      if (type instanceof ClientTypeDeclaration)
         type = ((ClientTypeDeclaration) type).getOriginal();

      Object elemValue = expr;
      if (pendingCreate || !updateType) {
         if (prop != null) {
            Object propertyType = ModelUtil.getPropertyType(prop);
            if (propertyType instanceof Class) {
               Type t = Type.get((Class) propertyType);
               if (t != null) {
                  try {
                     elemValue = t.stringToValue(expr);
                  }
                  catch (RuntimeException exc) {
                     return "Invalid value for: " + prop + ": " + expr + ": " + exc.toString();
                  }
               }
            }
         }
      }

      if (prop instanceof CustomProperty) {
         String error = updateCustomProperty((CustomProperty) prop, inst, elemValue);
         return error;
      }
      else if (pendingCreate) {
         return updatePendingProperty(ModelUtil.getPropertyName(prop), elemValue);
      }

      // The first time they are changing the object in a transparent layer.  We need to create it in this case.
      if (currentLayer != null && currentLayer.transparent && ModelUtil.getLayerForType(null, type) != currentLayer) {
         String typeName = ModelUtil.getTypeName(type);
         type = ctx.addTopLevelType(null, CTypeUtil.getPackageName(typeName), currentLayer, CTypeUtil.getClassName(typeName), null);
         invalidateModel();
      }
      return ctx.setElementValue(type, inst, prop, expr, updateType, updateInstances, valueIsExpr);
   }

   String validatePropertyTypeName(String typeName) {
      typeName = typeName == null ? "" : typeName.trim();
      if (typeName.length() == 0) {
         return null;
      }
      String err = ModelUtil.validateElement(JavaLanguage.getJavaLanguage().type, typeName, false);
      if (err != null) {
         return err;
      }
      else {
         if (ctx.isCreateInstType(typeName))
            return null;
         String fullTypeName = ctx.getCreateInstFullTypeName(typeName);
         if (fullTypeName != null)
            return null;

         if (findType(typeName) != null) {
            return null;
         }

         if (Type.getPrimitiveType(typeName) != null)
            return null;

         if (system.getTypeDeclaration(typeName) != null)
            return null;

         if (system.getTypeDeclaration(CTypeUtil.prefixPath("java.lang", typeName)) != null)
            return null;

         return "No property type named: " + typeName;
      }
   }

   String validateTypeText(String text, boolean instType) {
      String typeName = text == null ? "" : text.trim();
      if (typeName.length() == 0) {
         return null;
      }
      String err = ModelUtil.validateElement(JavaLanguage.getJavaLanguage().type, text, false);
      if (err != null) {
         return err;
      }
      else {
         if (ctx.isCreateInstType(typeName))
            return null;
         String fullTypeName = ctx.getCreateInstFullTypeName(typeName);
         if (fullTypeName != null)
            return null;

         if (!instType) {
            if (findType(typeName) != null) {
               return null;
            }
         }
         return "No type named: " + typeName;
      }
   }

   String validateNameText(String text) {
      if (text.trim().length() == 0) {
         return null;
      }
      JavaLanguage lang = JavaLanguage.getJavaLanguage();
      String err = ModelUtil.validateElement(lang.identifier, text, false);
      if (err != null) {
         return err;
      }
      return null;
   }


   String updateCurrentProperty(Object operator, String value, boolean instanceMode) {
      return setElementValue(currentPropertyType, null, currentProperty, operator + value, !instanceMode, true, true);
   }

   void validateCurrentProperty() {
      Object prop = currentProperty;
      if (prop == null) {
         currentPropertyType = null;
         currentPropertyName = null;
      }
      else {
         currentPropertyType = ModelUtil.getEnclosingType(prop);
         savedPropertyValue = currentPropertyValue = ctx.propertyValueString(currentType, null, prop);
         savedPropertyOperator = currentPropertyOperator = ModelUtil.getOperator(currentProperty);
         if (savedPropertyOperator == null)
            savedPropertyOperator = currentPropertyOperator = "=";
         currentPropertyName = ModelUtil.getPropertyName(prop);
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

   void changeCurrentType(Object type, Object inst, InstanceWrapper wrapper) {
      super.changeCurrentType(type, inst, wrapper);

      BodyTypeDeclaration ctxType = ctx.getCurrentType(false);
      if (ctxType != type) {
         if (ctxType != null)
            ctx.popCurrentType();
         if (type instanceof BodyTypeDeclaration)
            ctx.pushCurrentType((BodyTypeDeclaration) type, inst);
      }

      if (inst != ctx.currentObject)
         ctx.setDefaultCurrentObj(type, inst);

      // Push this back if the change is coming from the editor model side
      if (currentCtxType != type && type instanceof BodyTypeDeclaration) {
         currentCtxType = (BodyTypeDeclaration) type;
      }

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

   public Object findType(String typeName) {
      return system.getSrcTypeDeclaration(typeName, null, true);
   }

   public void updateCurrentType(String typeName) {
      if (currentType == null || !StringUtil.equalStrings(typeNames[0], typeName))
         findCurrentType(typeName);
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
      changeCurrentType(theType, null, null);
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

      changeCurrentType(res, null, null);

      return null;
   }

   String createProperty(String ownerTypeName, String propertyTypeName, String propertyName, String operator, String propertyValue, boolean addBefore, String relPropertyName) {
      String name = propertyName.trim();
      if (StringUtil.isEmpty(ownerTypeName)) {
         return "Select a type to hold the new property";
      }
      if (StringUtil.isEmpty(propertyTypeName)) {
         return "Select a data type for the new property";
      }
      Object ownerType = findType(ownerTypeName);
      if (ownerType == null) {
         return "No type: " + ownerTypeName;
      }
      String err = ctx.addProperty(ownerType, propertyTypeName, name, operator, propertyValue, addBefore, relPropertyName);
      return err;
   }

   String startOrCompleteCreate(String typeName) {
      if (!pendingCreate) {
         String err = createInstance(typeName);
         if (err != null) {
            return err;
         }
         else {
            // Need to potentially refresh the editor - and if the current type is already selected, it's a mode changed requiring a full refresh of the form
            //newInstSelected++;
            instanceModeChanged++;

            return pendingCreateError;
         }
      }
      else {
         String err = completeCreateInstance(true);
         if (err != null) {
            return err;
         }
         else {
            createMode = false; // Set this back to non-create state
            invalidateModel(); // Rebuild it so it says instance instead of new
            return null;
         }
      }
   }

   public String createInstance(String typeName) {
      Object typeObj = DynUtil.findType(typeName);
      String fullTypeName = null;
      if (typeObj == null) {
         // Implements the rule where you can just type in the class name
         fullTypeName = ctx.getCreateInstFullTypeName(typeName);
         if (fullTypeName != null) {
            typeName = fullTypeName;
            typeObj = DynUtil.findType(fullTypeName);
         }
         if (typeObj == null) {
            if (currentPackage != null) {
               fullTypeName = CTypeUtil.prefixPath(currentPackage, typeName);
               typeObj = DynUtil.findType(fullTypeName);
               if (typeObj != null)
                  typeName = fullTypeName;
            }
            if (typeObj == null) {
               return "No type: " + typeName + (currentPackage != null ? " with current package: " + currentPackage : "");
            }
         }

         if (!ctx.isCreateInstType(typeName)) {
            return "Type: " + typeName + " not able to create types missing @EditorCreate";
         }
      }
      if (!(typeObj instanceof BodyTypeDeclaration)) {
         typeObj = ModelUtil.resolveSrcTypeDeclaration(system, typeObj);
      }
      if (typeObj instanceof TypeDeclaration) {
         TypeDeclaration typeDecl = (TypeDeclaration) typeObj;
         InstanceWrapper instWrap = new InstanceWrapper(ctx, null, typeName, "<unset>", false);
         instWrap.pendingValues = new TreeMap<String,Object>();
         instWrap.pendingCreate = true;
         constructorProps = getConstructorProperties(instWrap, typeDecl);
         currentLayer = null; // If this type is not in the current layer, it gets removed from the list of visible types
         changeCurrentType(typeDecl, null, instWrap);

         pendingCreateError = completeCreateInstance(false);
         if (fullTypeName != null)
            createModeTypeName = fullTypeName;
      }
      else
         return "Unable to create instance of type: " + typeName + " - no type metadata found";
      return null;
   }

   public String createType(CreateMode mode, String name, String outerTypeName, String extendsTypeName, String pkg, Layer destLayer) {
      String err;

      Object outerType;
      if (outerTypeName == null) {
         outerType = null;
      }
      else {
         outerType = findType(outerTypeName);
         if (outerType == null) {
            return "No outer type: " + outerTypeName;
         }
      }

      if (name.length() == 0)
         err = "No name specified for new " + mode.toString().toLowerCase();
      else if (outerType != null)
         err = ctx.addInnerType(mode == CreateMode.Object ? "Object" : "Class", outerType, name, extendsTypeName, false, null);
      else {
         if (destLayer == null)
            err = "No layer specified for new " + mode.toString().toLowerCase();
         err = addTopLevelType(mode == CreateMode.Object ? "Object" : "Class", pkg, destLayer, name, extendsTypeName);
      }
      return err;
   }

   public String createLayer(String layerName, String pkgIdentifier, String extendsTypeList, boolean isDynamic, boolean isPublic, boolean isTransparent) {
      try {
         String layerPackage = pkgIdentifier == null || pkgIdentifier.trim().length() == 0 ? "" : ctx.validateIdentifier(pkgIdentifier);
         String[] extendsNames = ctx.validateExtends(extendsTypeList);

         Layer layer = ctx.createLayer(layerName, layerPackage, extendsNames, isDynamic, isPublic, isTransparent, true);

         if (layer != null) {
            changeCurrentTypeName(layer.layerModelTypeName);
            createMode = false;
         }

         return null;
      }
      catch (IllegalArgumentException exc) {
         return exc.toString();
      }
   }

   public String addLayer(String layerNameList, boolean isDynamic) {
      try {
         String[] addNames = ctx.validateExtends(layerNameList);

         Layer origLastLayer = system.lastLayer;

         ctx.addLayers(addNames, isDynamic, true);

         Layer newLastLayer = system.lastLayer;
         if (newLastLayer != origLastLayer) {
            if (newLastLayer != null) {
               changeCurrentTypeName(newLastLayer.layerModelTypeName);
               createMode = false;
            }
         }
         return null;
      }
      catch (IllegalArgumentException exc) {
         return exc.toString();
      }
   }

   public void cancelCreate() {
      changeCurrentType(null, null, null);
      pendingCreate = false;
      pendingCreateError = null;
   }

   public String updateCustomProperty(CustomProperty custProp, Object inst, Object propVal) {
      String err = custProp.updateInstance(inst, propVal);
      if (pendingCreate) // In this case this was the last value we needed, update the error
         pendingCreateError = completeCreateInstance(false);
      return err;
   }

   public String updatePendingProperty(String propName, Object value) {
      if (pendingCreate) {
         if (currentWrapper != null) {
            currentWrapper.pendingValues.put(propName, value);
         }
         pendingCreateError = completeCreateInstance(false);
      }
      return null;
   }

   public String completeCreateInstance(boolean doCreate) {
      if (pendingCreate) {
         if (currentType instanceof TypeDeclaration) {
            TypeDeclaration typeDecl = (TypeDeclaration) currentType;
            AbstractMethodDefinition createMeth = typeDecl.getEditorCreateMethod();
            ArrayList<Object> args = new ArrayList<Object>();
            if (constructorProps != null) {
               ArrayList<String> missingPropNames = null;
               for (ConstructorProperty prop:constructorProps) {
                  String paramName = prop.name;

                  Object val = currentWrapper.pendingValues == null ? null : currentWrapper.pendingValues.get(paramName);
                  if (!isValid(val)) {
                     if (missingPropNames == null)
                        missingPropNames = new ArrayList<String>();
                     missingPropNames.add(paramName);
                  }
                  args.add(val);
               }
            
               // Ready to create the new instance
               if (doCreate && missingPropNames == null) {
                  Object[] argsArr = args.toArray();
                  Object inst;
                  if (createMeth instanceof ConstructorDefinition) {
                     inst = DynUtil.newInnerInstance(typeDecl, null, createMeth.getTypeSignature(), argsArr);
                  }
                  else {
                     inst = DynUtil.invokeMethod(null, createMeth, argsArr);
                  }
                  if (inst != null) {
                     pendingCreate = false;

                     currentInstance = inst;
                     ArrayList<InstanceWrapper> selInsts = new ArrayList<InstanceWrapper>();
                     Map<String,Object> pendingValues = currentWrapper.pendingValues;
                     // Currently we don't serialize changes made to the InstanceWrapper... they get created on either side so maybe it's simpler just to create a new one
                     currentWrapper = currentWrapper.copyWithInstance(inst);
                     selInsts.add(currentWrapper);
                     selectedInstances = selInsts; // NOTE: make sure the value is defined before setting because it's bound to another property

                     currentWrapper.pendingCreate = false;

                     // Register the instance with the dynamic type system so it is returned by getInstancesOfType
                     DynUtil.addDynInstance(currentWrapper.typeName, inst);

                     // Now we've created the instance. For any pendingValues the user entered that were not
                     // constructor properties, set the property to the value in the instance.
                     for (Map.Entry<String,Object> pendingEnt:pendingValues.entrySet()) {
                        String propName = pendingEnt.getKey();
                        boolean skipProp = false;
                        for (int pix = 0; pix < constructorProps.size(); pix++) {
                           ConstructorProperty cprop = constructorProps.get(pix);
                           if (cprop.name.equals(propName)) {
                              skipProp = true;
                              break;
                           }
                        }
                        if (!skipProp) {
                           Object value = pendingEnt.getValue();
                           try {
                              DynUtil.setPropertyValue(currentWrapper.theInstance, propName, value);
                           }
                           catch (IllegalArgumentException exc) {
                              return "Failed to set property after create: " + propName + " to: " + value + " error: " + exc.toString();
                           }
                        }
                     }

                     // Now add it to the type tree
                     instanceAdded(inst);
                     constructorProps = null;

                     // Signal to the form view to refresh it's current instance
                     newInstSelected++;
                  }
                  else
                     return "Failed to create instance";
               }
               else if (missingPropNames != null) {
                   return "Missing values for properties: " + missingPropNames;
               }
            }
         }
      }
      return null;
   }

   public String updateInstanceProperty(Object propC, String propName, Object instance, InstanceWrapper wrapper, Object elementValue) {
      if (propC instanceof CustomProperty) {
         return updateCustomProperty((CustomProperty) propC, instance, elementValue);
      }
      if (instance != null) {
         try {
            DynUtil.setPropertyValue(instance, propName, elementValue);
         }
         catch (IllegalArgumentException exc) {
            return exc.toString();
         }
         catch (UnsupportedOperationException exc1) {
            return exc1.toString();
         }
      }
      else if (pendingCreate && wrapper == currentWrapper) {
         currentWrapper.pendingValues.put(propName, elementValue);
      }
      else
         return "No instance to update";
      return null;
   }

   private boolean isValid(Object obj) {
      if (obj instanceof String)
         return ((String) obj).trim().length() > 0;
      return obj != null;
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
