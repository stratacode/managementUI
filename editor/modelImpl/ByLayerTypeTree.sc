import sc.layer.LayerIndexInfo;

ByLayerTypeTree {
   boolean rebuildDirEnts() {
      TreeEnt rootEnts = new TreeEnt(EntType.Root, "By Layer", this, null, null);
      rootDirEnt = rootEnts;

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
         addLayerType(layerType, srcLayer, fileLayer, transparentLayer, layerDirEnt, srcEnt.prependPackage, false, true);
      }
      // Add the layer file itself
      addLayerType(layerFile, srcLayer, fileLayer, false, layerDirEnt, true, false, true);

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
         for (String layerFullType:fileLayer.getImportedTypeNames()) {
            String layerType;
            if (layerFullType.startsWith(fileLayer.packagePrefix)) {
               int pplen = fileLayer.packagePrefix.length();
               layerType = pplen == 0 ? layerFullType : layerFullType.substring(pplen+1);
            }
            else {
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
      }

      return layerDirEnt;
   }
}