import sc.layer.LayerIndexInfo;

ByLayerTypeTree {
   rootDirEnt = new TreeEnt(EntType.Root, "By Layer", this, null, null);
   rootTreeNode = createTreeNode(rootDirEnt);

   TreeEnt addModel(ILanguageModel m, boolean prependPackage) {
      Layer layer = m.layer;
      if (layer == null)
         return null;

      SrcEntry src = m.srcFile;

      String layerName = layer.layerName;

      String layerGroup = CTypeUtil.getPackageName(layerName);
      String layerFile = CTypeUtil.getClassName(layerName);
      TreeEnt layerParentEnt = rootDirEnt;
      if (layerGroup != null)
         layerParentEnt = lookupPackage(rootDirEnt, layerGroup, EntType.LayerGroup, null, null, true, false);

      TreeEnt layerDirEnt = lookupPackage(layerParentEnt, layerFile, EntType.LayerGroup, layer.packagePrefix, null, true, false);
      layerDirEnt.srcTypeName = "layerdir:" + layerDirEnt.srcTypeName; // Need to munch this so selecting the dir does not select the file

      String layerType = src.relTypeName;
      String fileDir = CTypeUtil.getPackageName(layerType);
      String fileTail = CTypeUtil.getClassName(layerType);
      TreeEnt ent = new TreeEnt(EntType.Type, fileTail, this, m.getModelTypeDeclaration().getFullTypeName(), layer);
      // If this entity is imported into this layer from outside, set the imported flag
      ent.imported = layer.getImportDecl(fileTail, false) != null;
      ent.prependPackage = prependPackage;
      Object td = ent.typeDeclaration;
      JavaModel jm = null;
      if (td != null)
         jm = ModelUtil.getJavaModel(td);
      ent.hasSrc = layer.findSrcFile(jm == null ? layerType : jm.getSrcFile().relFileName, true) != null;
      if (fileDir == null) {
         layerDirEnt.addChild(ent);
      }
      else {
         TreeEnt fileEnt = lookupPackage(layerDirEnt, fileDir, EntType.Type, layer.packagePrefix, layer, true, false);
         fileEnt.addChild(ent);
      }

      return ent;
   }

   TreeEnt removeModel(ILanguageModel m) {
      Layer layer = m.layer;
      if (layer == null)
         return null;

      SrcEntry src = m.srcFile;

      String layerName = layer.layerName;
      String layerGroup = CTypeUtil.getPackageName(layerName);
      String layerFile = CTypeUtil.getClassName(layerName);
      TreeEnt layerParentEnt = rootDirEnt;
      if (layerGroup != null)
         layerParentEnt = lookupPackage(rootDirEnt, layerGroup, EntType.LayerGroup, null, null, false, false);

      if (layerParentEnt == null)
         return null;

      TreeEnt layerDirEnt = lookupPackage(layerParentEnt, layerFile, EntType.LayerGroup, layer.packagePrefix, null, false, false);

      if (layerDirEnt == null)
         return null;

      String layerType = src.relTypeName;
      String fileDir = CTypeUtil.getPackageName(layerType);
      String fileTail = CTypeUtil.getClassName(layerType);

      if (fileDir != null) {
         layerDirEnt = lookupPackage(layerDirEnt, fileDir, EntType.Type, layer.packagePrefix, layer, true, false);
      }
      if (layerDirEnt == null)
         return null;

      TreeEnt foundEnt = null;
      for (TreeEnt ent:layerDirEnt.childEnts.values()) {
         if (ent.value.equals(fileTail)) {
            foundEnt = ent;
            layerDirEnt.removeChild(ent);
            break;
         }
      }
      return foundEnt;
   }

   TreeEnt removeType(ITypeDeclaration itd) {
      if (!(itd instanceof BodyTypeDeclaration))
         return null;

      BodyTypeDeclaration td = (BodyTypeDeclaration) itd;
      Layer layer = td.layer;
      if (layer == null)
         return null;

      JavaModel model = td.getJavaModel();
      SrcEntry src = model.srcFile;

      String layerName = layer.layerName;
      String layerGroup = CTypeUtil.getPackageName(layerName);
      String layerFile = CTypeUtil.getClassName(layerName);
      TreeEnt layerParentEnt = rootDirEnt;
      if (layerGroup != null)
         layerParentEnt = lookupPackage(rootDirEnt, layerGroup, EntType.LayerGroup, null, null, false, false);

      if (layerParentEnt == null)
         return null;

      TreeEnt layerDirEnt = lookupPackage(layerParentEnt, layerFile, EntType.LayerGroup, layer.packagePrefix, null, false, false);

      if (layerDirEnt == null)
         return null;

      String layerType = CTypeUtil.prefixPath(src.getRelDir(), td.getInnerTypeName());
      String fileDir = CTypeUtil.getPackageName(layerType);
      String fileTail = CTypeUtil.getClassName(layerType);

      if (fileDir != null) {
         layerDirEnt = lookupPackage(layerDirEnt, fileDir, EntType.Type, layer.packagePrefix, layer, true, false);
      }
      if (layerDirEnt == null)
         return null;

      TreeEnt foundEnt = null;
      if (layerDirEnt.childEnts != null) {
         for (TreeEnt ent:layerDirEnt.childEnts.values()) {
            if (ent.value.equals(fileTail)) {
               foundEnt = ent;
               layerDirEnt.childEnts.remove(layerDirEnt.srcTypeName);
               layerDirEnt.removeEntry(foundEnt);
               break;
            }
         }
      }
      return foundEnt;
   }

   TreeEnt removeLayer(Layer layer, boolean remove) {
      String layerName = layer.layerName;
      String layerGroup = CTypeUtil.getPackageName(layerName);
      String layerFile = CTypeUtil.getClassName(layerName);
      TreeEnt layerParentEnt = rootDirEnt;
      if (layerGroup != null)
         layerParentEnt = lookupPackage(rootDirEnt, layerGroup, EntType.LayerGroup, null, null, false, false);

      if (layerParentEnt == null)
         return null;

      TreeEnt layerDirEnt = lookupPackage(layerParentEnt, layerFile, EntType.LayerGroup, layer.packagePrefix, null, false, false);

      if (layerDirEnt == null)
         return null;

      if (remove) {
         layerParentEnt.childEnts.remove(layerFile);
         layerParentEnt.removeEntry(layerDirEnt);
      }
      else {
         layerDirEnt.type = EntType.InactiveLayer;
         layerDirEnt.value = CTypeUtil.getClassName(layer.layerDirName);
         layerDirEnt.srcTypeName = layer.layerDirName;
         if (layerDirEnt.childEnts != null) {
            layerDirEnt.childEnts.clear();
            layerDirEnt.childList.clear();
         }
      }

      return layerDirEnt;
   }

   TreeEnt findType(BodyTypeDeclaration td) {
      Layer typeLayer = td.getLayer();

      TreeEnt layerDirEnt = lookupPackage(rootDirEnt, typeLayer.layerName, EntType.LayerGroup, typeLayer.packagePrefix, null, false, false);
      if (layerDirEnt == null || layerDirEnt.childEnts == null)
         return null;

      int i = 0;
      for (TreeEnt ent:layerDirEnt.childEnts.values()) {
         if (ent.srcTypeName.equals(td.getFullTypeName())) {
            return ent;
         }
         i++;
      }
      return null;
   }

   TreeEnt addType(ITypeDeclaration itd) {
      if (!(itd instanceof BodyTypeDeclaration))
         return null;

      BodyTypeDeclaration td = (BodyTypeDeclaration) itd;
      Layer layer = td.getLayer();

      if (layer == null)
         return null;

      String layerName = layer.layerName;

      String layerGroup = CTypeUtil.getPackageName(layerName);
      String layerFile = CTypeUtil.getClassName(layerName);
      TreeEnt layerParentEnt = rootDirEnt;
      if (layerGroup != null)
         layerParentEnt = lookupPackage(rootDirEnt, layerGroup, EntType.LayerGroup, null, null, true, false);

      TreeEnt layerDirEnt = lookupPackage(layerParentEnt, layerFile, EntType.LayerGroup, layer.packagePrefix, null, true, false);
      layerDirEnt.srcTypeName = "layerdir:" + layerDirEnt.srcTypeName; // Need to munch this so selecting the dir does not select the file

      SrcEntry src = td.getJavaModel().getSrcFile();
      String layerType = CTypeUtil.prefixPath(src.getRelDir(), td.getInnerTypeName());
      String fileDir = CTypeUtil.getPackageName(layerType);
      String fileTail = CTypeUtil.getClassName(layerType);
      TreeEnt ent = new TreeEnt(EntType.Type, fileTail, this, td.getFullTypeName(), layer);
      ent.prependPackage = true;
      ent.layer = layer;
      // If this entity is imported into this layer from outside, set the imported flag
      ent.imported = layer.getImportDecl(fileTail, false) != null;
      ent.hasSrc = true;
      if (fileDir == null) {
         layerDirEnt.addChild(ent);
      }
      else {
         TreeEnt fileEnt = lookupPackage(layerDirEnt, fileDir, EntType.Type, layer.packagePrefix, layer, true, false);
         fileEnt.addChild(ent);
      }
      return ent;
   }

   void addLayerType(String layerType, EntType parentType, Layer srcLayer, Layer fileLayer, boolean transparentLayer, TreeEnt layerDirEnt, boolean prependPackage, boolean imported, boolean addInnerTypes) {
       String fileDir = CTypeUtil.getPackageName(layerType);
       String fileTail = CTypeUtil.getClassName(layerType);

       TreeEnt layerParent;
       if (fileDir == null) {
          layerParent = layerDirEnt;
       }
       else {
          layerParent = lookupPackage(layerDirEnt, fileDir, parentType, fileLayer.packagePrefix, fileLayer, true, false);
       }

       if (transparentLayer) {
          boolean found = false;
          // Do not add elements which are already here
          if (layerParent.hasChild(fileTail))
             return;
       }

       boolean isLayerFile = fileDir == null && fileTail.equals(CTypeUtil.getClassName(srcLayer.layerName));

       if (!transparentLayer || !isLayerFile) {
          TreeEnt ent = new TreeEnt(isLayerFile ? EntType.LayerFile : EntType.Type, fileTail, this, prependPackage && !imported ? CTypeUtil.prefixPath(fileLayer.packagePrefix, layerType) : layerType, fileLayer);

          if (treeModel.isFilteredType(ent.srcTypeName))
             return;

          // Here we are filtering types just to limit the number for testing - but don't filter if we've already go the type in the type tree.
          // This let's us test more functionality and creates a reasonable subset of the types.
          if (++treeModel.typesCreated >= treeModel.MAX_TYPES && treeModel.typeTree.getTreeEnt(ent.srcTypeName) == null) {
             System.out.println("*** Skipping type: " + ent.srcTypeName + " due to MAX_TYPES: " + treeModel.MAX_TYPES);
             return;
          }

          // If this entity is imported into this layer from outside, set the imported flag
          ent.imported = fileLayer.getImportDecl(fileTail, false) != null;
          ent.transparent = transparentLayer;
          ent.prependPackage = prependPackage;
          if (treeModel.loadInnerTypesAtStartup) {
             Object td = ent.typeDeclaration;
             JavaModel jm = null;
             if (td != null)
                jm = ModelUtil.getJavaModel(td);
             if (ent.type == EntType.LayerFile)
                ent.hasSrc = true;
             else
                ent.hasSrc = fileLayer.findSrcFile(jm == null ? layerType : jm.getSrcFile().relFileName, true) != null;
          }
          else {
             ent.hasSrc = true; // TODO: can't we compute this without parsing the type!
          }

          ent.initChildLists();
          layerParent.addChild(ent);

          if (treeModel.loadInnerTypesAtStartup && addInnerTypes) {
             Object typeDecl = ent.typeDeclaration;
             if (typeDecl != null) {
                Set<String> innerTypes = srcLayer.getInnerTypeNames(layerType, typeDecl, true);
                if (innerTypes != null) {
                   for (String innerType:innerTypes) {
                      addLayerType(innerType, EntType.Type, srcLayer, fileLayer, transparentLayer, layerDirEnt, true, false, false);
                   }
                }
             }
          }
       }
   }

   boolean rebuildDirEnts() {
      //TreeEnt rootEnts = new TreeEnt(EntType.Root, "By Layer", this, null, null);
      //rootDirEnt = rootEnts;
      TreeEnt rootEnts = rootDirEnt;

      for (int i = 0; i < treeModel.system.layers.size(); i++) {
         Layer layer = treeModel.system.layers.get(i);
         if (layer.getVisibleInEditor() && !treeModel.isFilteredPackage(layer.packagePrefix))
            addLayerDirEnt(layer);

         if (++treeModel.layersCreated >= treeModel.MAX_LAYERS) {
            System.out.println("*** skipping layers due to MAX_LAYERS setting: " + treeModel.MAX_LAYERS);
            break;
         }
      }

      TreeEnt cent = emptyCommentNode = new TreeEnt(EntType.Comment, "<No visible layers>", this, null, null);

      Map<String,LayerIndexInfo> allLayerIndex = treeModel.system.getAllLayerIndex();
      for (LayerIndexInfo lii:allLayerIndex.values()) {
          if (!treeModel.includeInactive)
             break;
         // Do not replace a system layer with one from the index
         //if (system.getLayerByDirName(lii.layerDirName) == null) {
            String layerDirName = lii.layerDirName;
            String layerGroup = CTypeUtil.getPackageName(layerDirName);

            TreeEnt pkgEnts;
            if (layerGroup != null)
               pkgEnts = lookupPackage(rootEnts, layerGroup, EntType.LayerGroup, null, null, true, false);
            else
               pkgEnts = rootEnts;

            TreeEnt ent = new TreeEnt(EntType.InactiveLayer, CTypeUtil.getClassName(lii.layerDirName), this, lii.layerDirName, null);
            ent.prependPackage = true;

            pkgEnts.addChild(ent);
         //}
      }
      rootEnts.open = true;
      rootEnts.processEntry();

      return true;
   }

   TreeEnt addLayerDirEnt(Layer layer) {
      return addLayerFilesWithName(layer, layer, false);
   }

   /**
     * The srcLayer is used to retrieve the source file names.  The fileLayer is the layer used to register those files.  They are
     * the same unless transparentLayer is true... in that case, we are adding the files from srcFile to fileLayer's tree unless those files
     * already exist in fileLayer
     */
   TreeEnt addLayerFilesWithName(Layer srcLayer, Layer fileLayer, boolean transparentLayer) {
      String layerName = fileLayer.layerName;
      String layerGroup = CTypeUtil.getPackageName(layerName);
      String layerFile = CTypeUtil.getClassName(layerName);
      TreeEnt layerParentEnt = rootDirEnt;
      if (layerGroup != null)
         layerParentEnt = lookupPackage(rootDirEnt, layerGroup, EntType.LayerGroup, null, null, true, false);

      TreeEnt layerDirEnt = lookupPackage(layerParentEnt, layerFile, EntType.LayerDir, fileLayer.packagePrefix, null, true, false);
      layerDirEnt.type = EntType.LayerDir; // Might exist as Inactive if we're loading this new

      layerDirEnt.layer = fileLayer;
      Set<SrcEntry> layerSrcEntries = srcLayer.getSrcEntries();
      for (SrcEntry srcEnt:layerSrcEntries) {
         String layerType = srcEnt.relTypeName;
         addLayerType(layerType, EntType.Package, srcLayer, fileLayer, transparentLayer, layerDirEnt, srcEnt.prependPackage, false, true);
      }
      // Add the layer file itself
      addLayerType(layerFile, EntType.LayerDir, srcLayer, fileLayer, false, layerDirEnt, true, false, true);

      //Set<String> layerImported = srcLayer.getImportedTypeNames();
      //for (String importedType:layerImported) {
         // Do not set prependPackage here since the imported types are always absolute and not relative to this layer necessarily
      //   addLayerType(importedType, srcLayer, fileLayer, transparentLayer, layerDirEnt, false, true, false);
      //}

      // If this layer is transparent, any any files which exist in the base layers
      if (fileLayer.transparent && fileLayer.baseLayers != null && !transparentLayer) {
         for (Layer base:fileLayer.baseLayers) {
            addLayerFilesWithName(base, fileLayer, true);
         }
      }

      // Transparent layers will already have inherited the imported types
      if (!transparentLayer) {
         // Adding imported names.  If the layer prefix matches, keep the file organization.  If not, add these types
         // to the top level.  If they are imported, their names must not conflict.  Layers can then reorganize types
         // in different packages.
         /*
           Not sure why this was here but it was putting types in the wrong layer directory.
         for (String layerFullType:fileLayer.getImportedTypeNames()) {
            String layerType;
            if (layerFullType.startsWith(fileLayer.packagePrefix)) {
               int pplen = fileLayer.packagePrefix.length();
               layerType = pplen == 0 ? layerFullType : layerFullType.substring(pplen+1);
            }
            else {
               // TODO: this is not right - we should not include types not defined in the layer here
               layerType = CTypeUtil.getClassName(layerFullType);
            }
            String fileDir = CTypeUtil.getPackageName(layerType);
            String fileTail = CTypeUtil.getClassName(layerType);
            TreeEnt layerParent;
            if (fileDir == null)
               layerParent = layerDirEnt;
            else
               layerParent = lookupPackage(layerDirEnt, fileDir, EntType.Type, fileLayer.packagePrefix, fileLayer, true, false);

            if (!layerParent.hasChild(fileTail)) {
               TreeEnt ent = new TreeEnt(EntType.Type, fileTail, this, layerFullType, fileLayer);
               ent.prependPackage = true;
               // If this entity is imported into this layer from outside, set the imported flag
               ent.imported = true;
               ent.hasSrc = fileLayer.findSrcFile(layerType.replace('.', '/'), true) != null;
               layerParent.addChild(ent);
            }
         }
         */
      }

      return layerDirEnt;
   }
}
