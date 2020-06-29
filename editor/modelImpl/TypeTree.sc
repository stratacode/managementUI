import sc.layer.LayerIndexInfo;

TypeTree {
   boolean isImplProcess() {
      return true;
   }

   TreeEnt {
      needsType =: fetchType();

      void fetchType() {
         if (needsType) {
            cachedTypeDeclaration = getTypeDeclaration();
            if (cachedTypeDeclaration == null)
               needsType = false;
            processEntry();
            if (cachedTypeDeclaration != null) {
               initChildren();
            }
         }
      }

      boolean hasErrors() {
         Object td = getTypeDeclaration();
         if (td instanceof BodyTypeDeclaration) {
            JavaModel model = ((BodyTypeDeclaration) td).getJavaModel();
            return editorModel.ctx.hasErrors(model);
         }
         return false;
      }

      boolean needsSave() {
         Object td = getTypeDeclaration();
         if (td instanceof BodyTypeDeclaration) {
            JavaModel model = ((BodyTypeDeclaration) td).getJavaModel();
            return editorModel.ctx.modelChanged(model);
         }
         return false;
      }

      Object getTypeDeclaration() {
         if (typeName == null)
            return null;
         if (cachedTypeDeclaration != null)
            return cachedTypeDeclaration;
         if (type == EntType.LayerFile) {
            Layer layer = treeModel.system.getLayerByTypeName(typeName);
            if (layer != null) {
               return layer.model.getModelTypeDeclaration();
            }
            return null;
         }

         // First try to get the src version
         if (treeModel.system == null)
            return null;
         Object res = treeModel.system.getSrcTypeDeclaration(typeName, null, prependPackage);
         // Fall back to the class but only for things which are real types.
         if (res == null && prependPackage)
            res = treeModel.system.getClassWithPathName(typeName);

         return res;
      }

/*
      boolean getTypeIsVisible() {
         // TODO: method to find the set of layers for a file name - filter those layers
         if (cachedTypeDeclaration != null || treeModel.loadInnerTypesAtStartup) {
            Object type = getTypeDeclaration();
            if (type != null) {
               if (!ModelUtil.matchesLayerFilter(type, treeModel.codeTypes, treeModel.codeFunctions)) {
                  return false;
               }
               if (!treeModel.createMode && !ModelUtil.isApplicationType(type))
                  return false;
            }
         }
         return true;
      }
   */

      void processTypeInfo() {
         // type is already assigned for these types
         UIIcon newIcon = null;
         switch (type) {
            // NOTE: Package cannot be in this list due the fact that getSrcTypeNames returns inner classes in front of
            // outer classes.  When we do lookupPackage we create the parent node as a package.  We don't then reset it
            // when we add the type with the same name.
            case Root:
               open = true;
               // Fall through to set the icon
            case LayerGroup:
            case InactiveLayer:
            case Primitive:
            case Instance:
               newIcon = findIcon();
               if (newIcon != null && newIcon != icon)
                  icon = newIcon;
               return;
            case LayerDir:
            case LayerFile:
               if (layer != null) {
                  // We already pull these out of the layer so no need to set them here.  They are used in TypeIsVisible which is not used for layers anyway.
                  //entCodeTypes = new ArrayList<CodeType>(Collections.singletonList(layer.codeType));
                  newIcon = findIcon();
                  if (newIcon != null && newIcon != icon)
                     icon = newIcon;
               }
               return;
         }
         if (newIcon != null && newIcon != icon)
            icon = newIcon;

         Object typeDecl = treeModel.loadInnerTypesAtStartup ? getTypeDeclaration() : cachedTypeDeclaration;
         if (typeDecl != null) {
            EntType newType = null;
            switch (ModelUtil.getDeclarationType(typeDecl)) {
               case CLASS:
                  if (DynUtil.isSingletonType(typeDecl))
                     newType = EntType.ParentObject;
                  else
                     newType = EntType.ParentType;
                  break;
               case OBJECT:
                  newType = EntType.ParentObject;
                  break;
               case INTERFACE:
                  newType = EntType.ParentInterface;
                  break;
               case ENUM:
                  newType = EntType.ParentEnum;
                  break;
               case ENUMCONSTANT:
                  newType = EntType.ParentEnumConstant;
                  break;
               default:
                  newType = EntType.ParentType;
                  break;
            }
            // This can get called twice, once again after we set the type declaration (if we did not load types at startup)
            if (newType != null && newType != type)
               type = newType;

            // Only set these for nodes with types.
            if (entCodeTypes == null) {
               entCodeTypes = new ArrayList<CodeType>();
               if (isTypeTree() || layer == null) {
                   ModelUtil.getFiltersForType(typeDecl, entCodeTypes, isTypeTree());
               }
               // For typeDir ents, we don't want the most specific layer of the type only the layer associated with the directory
               else {
                  entCodeTypes.add(layer.codeType);
               }
            }
         }
         else {
            Layer layer = treeModel.system.getLayerByName(srcTypeName);
            if (layer == null) {
               layer = treeModel.system.getLayerByTypeName(srcTypeName);
            }
            if (layer != null) {
               type = EntType.LayerFile;
            }
            // Package
         }
         icon = findIcon();
      }

      boolean isDynamic() {
         Object type = treeModel.loadInnerTypesAtStartup ? getTypeDeclaration() : getCachedTypeDeclaration();
         if (type == null)
            return false;
         return ModelUtil.isDynamicType(type);
      }

      UIIcon findIcon() {
        switch (type) {
           case Root:
           case Comment:
              return null;
           case ParentType:
           case Type:
              if (isDynamic())
                 return GlobalResources.classDynIcon;
              else
                 return GlobalResources.classIcon;
           case ParentObject:
           case Object:
              if (isDynamic())
                 return GlobalResources.objectDynIcon;
              else
                 return GlobalResources.objectIcon;
           case ParentEnum:
           case ParentEnumConstant:
           case EnumConstant:
           case Enum:
              if (isDynamic())
                 return GlobalResources.enumDynIcon;
              else
                 return GlobalResources.enumIcon;
           case ParentInterface:
           case Interface:
              if (isDynamic())
                 return GlobalResources.interfaceDynIcon;
              else
                 return GlobalResources.interfaceIcon;
           case LayerDir:
           case LayerFile:
              if (layer != null && layer.dynamic)
                 return GlobalResources.layerDynIcon;
              else
                 return GlobalResources.layerIcon;
           case InactiveLayer:
              return GlobalResources.inactiveLayerIcon;
           case Primitive:
              String tn = value;
              if (tn.equals("String") || tn.equals("char"))
                 return GlobalResources.stringIcon;
              else if (tn.equals("int") || tn.equals("short") || tn.equals("byte") || tn.equals("long"))
                 return GlobalResources.intIcon;
              else if (tn.equals("float") || tn.equals("double"))
                 return GlobalResources.floatIcon;
              else if (tn.equals("boolean"))
                 return GlobalResources.booleanIcon;
              else
                 System.err.println("*** Unknown primitive type: " + tn);
              break;
           case Instance:
              return GlobalResources.instanceIcon;
         }
         return null;
      }

      void processEntry() {
         processTypeInfo();

        if (childEnts != null) {
            Collections.sort(childList);
            for (TreeEnt childEnt:childEnts.values()) {
               childEnt.processEntry();
            }
         }
/*
         if (childList == null || childList.size() == 0) {
            needsOpenClose = false;
            hasChildren = false;
         } */

         // Accumulate the entCodeTypes and Functions from all child nodes so that we know whether or not to
         // show a DirEnt even when it's subDirs and entries are not populated on the client.
         if (entCodeTypes == null) {
            if (childEnts != null) {
               entCodeTypes = new ArrayList<CodeType>();
               for (TreeEnt childEnt:childEnts.values()) {
                  addCodeTypes(childEnt.entCodeTypes, entCodeTypes);
               }
            }
         }
      }

      private void addCodeTypes(ArrayList<CodeType> srcTypes, ArrayList<CodeType> dstTypes) {
         if (srcTypes == null)
            return;
         for (int i = 0; i < srcTypes.size(); i++) {
            CodeType ct = srcTypes.get(i);
            if (!dstTypes.contains(ct))
               dstTypes.add(ct);
         }
      }
   }

   boolean rebuildDirEnts() {
      //TreeEnt rootEnts = new TreeEnt(EntType.Root, "All Types", this, null, null);
      //rootDirEnt = rootEnts;

      TreeEnt rootEnts = rootDirEnt;

      if (treeModel.includePrimitives) {
         // First add the primitive types
         for (String primTypeName:treeModel.getExtendedPrimitiveTypeNames()) {
            TreeEnt ent = new TreeEnt(EntType.Primitive, primTypeName, this, primTypeName, null);
            ent.prependPackage = true;

            // Primitives are treated like imported types since they are not defined inside layers as src
            ent.imported = true;
            ent.hasSrc = false;
            rootEnts.addChild(ent);
         }
      }

      Set<String> srcTypeNames = treeModel.getSrcTypeNames();

      // Then build our TreeEnt structure from the Set of src type names we get back
      for (String srcTypeName:srcTypeNames) {
         if (srcTypeName.equals("java.math.BigDecimal"))
            System.out.println("***");
         if (!treeModel.isFilteredType(srcTypeName)) {
            addModel(srcTypeName, true);
            if (++treeModel.typesCreated >= treeModel.MAX_TYPES) {
               System.out.println("*** Skipping some types due to max types setting of: " + treeModel.MAX_TYPES);
               break;
            }
         }
      }

      // This retrieves all of the layer definitions in the system and registers them in the
      // type index.

      Map<String,LayerIndexInfo> allLayerIndex = treeModel.system.getAllLayerIndex();
      for (LayerIndexInfo lii:allLayerIndex.values()) {
          if (!treeModel.includeInactive)
             break;
         // Do not replace a system layer with one from the index
         //if (system.getLayerByDirName(lii.layerDirName) == null) {
            String pkg = lii.packageName;
            if (pkg == null)
               pkg = "<Global Layers>";

            TreeEnt pkgEnts = lookupPackage(rootEnts, pkg, EntType.Package, null, null, true, true);

            TreeEnt ent = new TreeEnt(EntType.InactiveLayer, lii.layerDirName, this, lii.layerDirName, null);
            ent.prependPackage = true;

            pkgEnts.addChild(ent);
         //}
      }

      rootEnts.open = true;
      rootEnts.processEntry();

      return true;
   }

   TreeEnt lookupPackage(TreeEnt parentEnt, String pkgName, EntType type, String srcPrefix, Layer layer, boolean create, boolean isTypeTree) {
      Map<String, TreeEnt> index = parentEnt.childEnts;
      String root = CTypeUtil.getHeadType(pkgName);
      String tail = CTypeUtil.getTailType(pkgName);

      if (root == null) {
         TreeEnt ents = index == null ? null : index.get(pkgName);
         if (ents == null) {
            if (!create)
               return null;

            ents = new TreeEnt(type, pkgName, this, CTypeUtil.prefixPath(srcPrefix, pkgName), layer);
            ents.prependPackage = true;

            String srcTypeName = ents.srcTypeName = CTypeUtil.prefixPath(srcPrefix, pkgName);

            // If this name is defined in an import and we do not have a src file for it, set the imported flag.
            ents.imported = treeModel.system.getImportDecl(null, null, CTypeUtil.getClassName(srcTypeName)) != null;

            ents.hasSrc = treeModel.system.getSrcTypeDeclaration(srcTypeName, null, true) != null;

            // Need to initialize this even if there are no children so we can tell the difference between an unfetched map and an empty local map.
            ents.initChildLists();

            parentEnt.addChild(ents);
         }
         return ents;
      }
      else {
         TreeEnt ents = index == null ? null : index.get(root);

         // Layer dir's should replace InactiveLayers when we add them.
         if (type == EntType.LayerDir && ents.type == EntType.InactiveLayer)
            ents = null;

         if (ents == null) {
            if (!create)
               return null;

            ents = new TreeEnt(type, root, this, (type == EntType.LayerGroup ? "layerGroup:" : "") + CTypeUtil.prefixPath(srcPrefix, root), layer);
            ents.imported = false;
            ents.hasSrc = true;
            ents.prependPackage = true;
            ents.initChildLists();
            parentEnt.addChild(ents);
         }

         return lookupPackage(ents, tail, type, CTypeUtil.prefixPath(srcPrefix, root), layer, create, isTypeTree);
      }
   }

   TreeEnt addModel(String srcTypeName, boolean prependPackage) {
      String pkg = CTypeUtil.getPackageName(srcTypeName);
      String className = CTypeUtil.getClassName(srcTypeName);
      TreeEnt ent = new TreeEnt(EntType.Type, className, this, srcTypeName, null);
      ent.prependPackage = prependPackage;
      ent.initChildLists();

// If this name is defined in an import and we do not have a src file for it, set the imported flag.
      ent.imported = treeModel.system.getImportDecl(null, null, className) != null;

      TypeDeclaration typeDecl = treeModel.loadInnerTypesAtStartup ? treeModel.system.getSrcTypeDeclaration(srcTypeName, null, true) : null;
      if (typeDecl == null && treeModel.specifiedLayers != null) {
          System.err.println("*** specified layer names in type tree model not yet implemented");
          // TODO: need a new system.getInactiveTypeDeclaration method here.  Alternatively now thinking about just
          // creating a separate inactive layered system.   That way we can pull in all dependent layers, sort them etc. and do
          // the full type stuff on them.
      }
      if (typeDecl != null && typeDecl.isLayerType) {
          ent.type = EntType.LayerFile;
          ent.layer = typeDecl.getLayer();
      }

      if (typeDecl == null) {
         Layer layer = treeModel.system.getLayerByTypeName(srcTypeName);
         if (layer != null) {
            ent.type = EntType.LayerFile;
            ent.layer = layer;
            ent.hasSrc = true;
         }
      }
      else {
         ent.hasSrc = true;
      }

      // When loadInnerTypesAtStartup is false assuming there is src so types are visible.
      ent.hasSrc = typeDecl != null || !treeModel.loadInnerTypesAtStartup;
      if (pkg != null) {
         EntType pkgType = EntType.Package;
         if (typeDecl != null && typeDecl.getEnclosingType() != null)
            pkgType = EntType.ParentType;
         TreeEnt pkgEnts = lookupPackage(rootDirEnt, pkg, pkgType, null, null, true, true);
         if (!pkgEnts.hasChild(ent.value))
            pkgEnts.addChild(ent);
      }
      else {
         if (!rootDirEnt.hasChild(ent.value))
            rootDirEnt.addChild(ent);
      }
      return ent;
   }

   TreeEnt getTreeEnt(String srcTypeName) {
      String pkg = CTypeUtil.getPackageName(srcTypeName);
      String className = CTypeUtil.getClassName(srcTypeName);

      TreeEnt pkgEnt;
      TreeEnt foundEnt = null;

      if (pkg != null)
         pkgEnt = lookupPackage(rootDirEnt, pkg, EntType.Package, null, null, false, true);
      else
         pkgEnt = rootDirEnt;

      if (pkgEnt == null || pkgEnt.childEnts == null)
         return null;

      TreeEnt childDir = pkgEnt.childEnts.get(srcTypeName);
      if (childDir != null) {
         return childDir;
      }
      return foundEnt;
   }

   TreeEnt removeType(String srcTypeName) {
      String pkg = CTypeUtil.getPackageName(srcTypeName);
      String className = CTypeUtil.getClassName(srcTypeName);

      TreeEnt pkgEnt;
      TreeEnt foundEnt = null;

      if (pkg != null)
         pkgEnt = lookupPackage(rootDirEnt, pkg, EntType.Package, null, null, false, true);
      else
         pkgEnt = rootDirEnt;

      if (pkgEnt == null || pkgEnt.childEnts == null)
         return null;

      TreeEnt childDir = pkgEnt.childEnts.get(className);
      if (childDir != null) {
         pkgEnt.childEnts.remove(className);
         pkgEnt.removeEntry(childDir);
      }
      return foundEnt;
   }

   TreeEnt removeLayer(Layer layer, boolean remove) {
      String srcTypeName = layer.getLayerModelTypeName();
      String pkg = CTypeUtil.getPackageName(srcTypeName);
      String className = CTypeUtil.getClassName(srcTypeName);

      TreeEnt pkgEnt;
      TreeEnt foundEnt = null;

      if (pkg != null)
         pkgEnt = lookupPackage(rootDirEnt, pkg, EntType.Package, null, null, false, true);
      else
         pkgEnt = rootDirEnt;

      if (pkgEnt == null)
         return null;

      for (TreeEnt childEnt:pkgEnt.childEnts.values()) {
         if (childEnt.value.equals(className)) {
            foundEnt = childEnt;

            if (remove) {
               pkgEnt.childEnts.remove(foundEnt.srcTypeName);
               pkgEnt.removeEntry(foundEnt);
            }
            else {
               foundEnt.type = EntType.InactiveLayer;
               foundEnt.value = layer.layerDirName;
               foundEnt.srcTypeName = layer.layerDirName;
               foundEnt.removeChildren();
            }
            break;
         }
      }
      return foundEnt;
   }
}