import sc.layer.LayeredSystem;
import sc.layer.IModelListener;
import sc.layer.LayerIndexInfo;

import sc.type.Type;

import sc.lang.java.BodyTypeDeclaration;
import sc.lang.java.ITypeDeclaration;
import sc.lang.java.TypeDeclaration;
import sc.lang.java.ModelUtil;
import sc.lang.java.JavaModel;

import sc.lang.ILanguageModel;

import java.util.Collections;

TypeTreeModel {
   ArrayList<String> packageFilter;


   // TODO: not implemented yet Populated from specifiedLayerNames.  Defines the layers from which we are doing source files.
   ArrayList<Layer> specifiedLayers;

   system = LayeredSystem.getCurrent();

   // TODO: fix this!  Ideally we do not load the types until you click on them.  At that point, we need to load all types, including those which extend the selected type.
   // It might be simpler to optimize this at the package level.  We'll load the inner types of all types when we initialize the type.  The obstacle now is that we need to
   // create DirEnt's for each type once it's been selected.  Maybe we use the addTypeToLayer and addModelType methods?  Essentially re-adding these types... that will add new entries to the subDirs for the parent and push those changes to the client.
   static boolean loadInnerTypesAtStartup = true;

   IModelListener listener;

   void start() {
      system.addNewModelListener(listener = new IModelListener() {
         void modelAdded(ILanguageModel m) {
            addNewModel(m);
         }
         void innerTypeAdded(ITypeDeclaration td) {
            addNewType(td);
         }
         void layerAdded(Layer l) {
            addNewLayer(l);
         }
         void modelRemoved(ILanguageModel m) {
            removeModel(m);
         }
         void innerTypeRemoved(ITypeDeclaration td) {
            removeType(td);
         }
         void layerRemoved(Layer l) {
            removeLayer(l);
         }
         void runtimeAdded(LayeredSystem sys) {
         }
      });
   }

   ArrayList<String> getExtendedPrimitiveTypeNames() {
      Set<String> primNames = Type.getPrimitiveTypeNames();
      ArrayList<String> res = new ArrayList<String>(primNames.size()+1);
      res.addAll(primNames);
      res.remove("void");
      res.add("String");
      return res;
   }


   // For testing use these to cut down the number of types or layers
   private static int MAX_TYPES = 20000; // 100;
   private static int MAX_LAYERS = 20000; // 10;

   boolean includeInactive = false;
   boolean includePrimitives = false;

   boolean isFilteredType(String typeName) {
      if (packageFilter == null)
         return false;
      if (typeName == null)
         return true;
      for (String pkg:packageFilter) {
         if (typeName.startsWith(pkg))
            return false;
      }
      return true;
   }

   boolean isFilteredPackage(String pkgName) {
      if (packageFilter == null)
         return false;
      if (pkgName == null)
         return false;
      for (String pkgFilter:packageFilter) {
         if (pkgName.startsWith(pkgFilter) || pkgFilter.startsWith(pkgName))
            return false;
      }
      return true;
   }

   boolean rebuildTypeDirEnts() {
      DirEnt rootEnts = new DirEnt(EntType.Root, "All Types", true, null, null);
      rootTypeDirEnt = rootEnts;

      if (includePrimitives) {
         // First add the primitive types
         for (String primTypeName:getExtendedPrimitiveTypeNames()) {
            TreeEnt ent = new TreeEnt(EntType.Primitive, primTypeName, true, primTypeName, null);
            ent.prependPackage = true;

            // Primitives are treated like imported types since they are not defined inside layers as src
            ent.imported = true;
            ent.hasSrc = false;
            rootEnts.entries.add(ent);
         }
      }

      TreeEnt cent = typeEmptyCommentNode = new TreeEnt(EntType.Comment, "No visible types", true, null, null);

      Set<String> srcTypeNames = system.getSrcTypeNames(true, loadInnerTypesAtStartup, false, true, true);
      if (specifiedLayerNames != null) {
         specifiedLayers = new ArrayList<Layer>(specifiedLayerNames.length);
         for (int i = 0; i < specifiedLayerNames.length; i++) {
            Layer layer = system.getInactiveLayer(specifiedLayerNames[i], true, true, true, false);
            if (layer == null)
               System.err.println("*** TypeTreeModel: Unable to find layer with specifiedLayerName: " + specifiedLayerNames[i]);
            else {
               // TODO: we should put these into dependency order but we can't use position cause these are inactive.
               specifiedLayers.add(layer);
            }
         }
         Set<String> additionalNames = system.getSrcTypeNames(specifiedLayers, true, loadInnerTypesAtStartup, false, true);
         if (additionalNames != null) {
            if (srcTypeNames == null)
               srcTypeNames = additionalNames;
            else
               srcTypeNames.addAll(additionalNames);
         }
      }

      // Then build our DirEnt structure from the Set of src type names we get back
      for (String srcTypeName:srcTypeNames) {
         if (!isFilteredType(srcTypeName)) {
            addModelToTypeTree(srcTypeName, true);
            if (++typesCreated >= MAX_TYPES) {
               System.out.println("*** Skipping some types due to max types setting of: " + MAX_TYPES);
               break;
            }
         }
      }

      // This retrieves all of the layer definitions in the system and registers them in the
      // type index.

      Map<String,LayerIndexInfo> allLayerIndex = system.getAllLayerIndex();
      for (LayerIndexInfo lii:allLayerIndex.values()) {
          if (!includeInactive)
             break;
         // Do not replace a system layer with one from the index
         //if (system.getLayerByDirName(lii.layerDirName) == null) {
            String pkg = lii.packageName;
            if (pkg == null)
               pkg = "<Global Layers>";

            DirEnt pkgEnts = lookupPackage(rootEnts.subDirs, pkg, EntType.Package, null, null, true, true);

            TreeEnt ent = new TreeEnt(EntType.InactiveLayer, lii.layerDirName, true, lii.layerDirName, null);
            ent.prependPackage = true;

            pkgEnts.entries.add(ent);
         //}
      }

      rootEnts.processEntry();

      return true;
   }

   TreeEnt addModelToTypeTree(String srcTypeName, boolean prependPackage) {
      String pkg = CTypeUtil.getPackageName(srcTypeName);
      String className = CTypeUtil.getClassName(srcTypeName);
      TreeEnt ent = new TreeEnt(EntType.Type, className, true, srcTypeName, null);
      ent.prependPackage = prependPackage;

      // If this name is defined in an import and we do not have a src file for it, set the imported flag.
      ent.imported = system.getImportDecl(null, null, className) != null;

      TypeDeclaration typeDecl = loadInnerTypesAtStartup ? system.getSrcTypeDeclaration(srcTypeName, null, true) : null;
      if (typeDecl == null && specifiedLayers != null) {
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
         Layer layer = system.getLayerByTypeName(srcTypeName);
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
      ent.hasSrc = typeDecl != null || !loadInnerTypesAtStartup;
      if (pkg != null) {
         EntType pkgType = EntType.Package;
         if (typeDecl != null && typeDecl.getEnclosingType() != null)
            pkgType = EntType.ParentType;
         DirEnt pkgEnts = lookupPackage(rootTypeDirEnt.subDirs, pkg, pkgType, null, null, true, true);
         if (!pkgEnts.hasChild(ent.value))
            pkgEnts.entries.add(ent);
      }
      else {
         if (!rootTypeDirEnt.hasChild(ent.value))
            rootTypeDirEnt.entries.add(ent);
      }
      return ent;
   }

   TreeEnt getTypeTreeEnt(String srcTypeName) {
      String pkg = CTypeUtil.getPackageName(srcTypeName);
      String className = CTypeUtil.getClassName(srcTypeName);

      DirEnt pkgEnt;
      TreeEnt foundEnt = null;

      if (pkg != null)
         pkgEnt = lookupPackage(rootTypeDirEnt.subDirs, pkg, EntType.Package, null, null, false, true);
      else
         pkgEnt = rootTypeDirEnt;

      if (pkgEnt == null || pkgEnt.subDirs == null)
         return null;

      DirEnt childDir = pkgEnt.subDirs.get(className);
      if (childDir != null) {
         return childDir;
      }

      if (pkgEnt.entries != null) {
         for (int i = 0; i < pkgEnt.entries.size(); i++) {
            if (pkgEnt.entries.get(i).value.equals(className)) {
               foundEnt = pkgEnt.entries.get(i);
               break;
            }
         }
      }
      return foundEnt;
   }

   TreeEnt removeTypeFromTypeTree(String srcTypeName) {
      String pkg = CTypeUtil.getPackageName(srcTypeName);
      String className = CTypeUtil.getClassName(srcTypeName);

      DirEnt pkgEnt;
      TreeEnt foundEnt = null;

      if (pkg != null)
         pkgEnt = lookupPackage(rootTypeDirEnt.subDirs, pkg, EntType.Package, null, null, false, true);
      else
         pkgEnt = rootTypeDirEnt;

      if (pkgEnt == null || pkgEnt.subDirs == null)
         return null;

      DirEnt childDir = pkgEnt.subDirs.get(className);
      if (childDir != null) {
         pkgEnt.subDirs.remove(className);
         pkgEnt.removeEntry(childDir);
      }

      if (pkgEnt.entries != null) {
         for (int i = 0; i < pkgEnt.entries.size(); i++) {
            if (pkgEnt.entries.get(i).value.equals(className)) {
               foundEnt = pkgEnt.entries.get(i);

               pkgEnt.entries.remove(i);
               pkgEnt.removeEntry(foundEnt);

               break;
            }
         }
      }
      return foundEnt;
   }

   TreeEnt removeLayerFromTypeTree(Layer layer, boolean remove) {
      String srcTypeName = layer.getLayerModelTypeName();
      String pkg = CTypeUtil.getPackageName(srcTypeName);
      String className = CTypeUtil.getClassName(srcTypeName);

      DirEnt pkgEnt;
      TreeEnt foundEnt = null;

      if (pkg != null)
         pkgEnt = lookupPackage(rootTypeDirEnt.subDirs, pkg, EntType.Package, null, null, false, true);
      else
         pkgEnt = rootTypeDirEnt;

      if (pkgEnt == null)
         return null;

      for (int i = 0; i < pkgEnt.entries.size(); i++) {
         if (pkgEnt.entries.get(i).value.equals(className)) {
            foundEnt = pkgEnt.entries.get(i);

            if (remove) {
               pkgEnt.entries.remove(i);
               pkgEnt.removeEntry(foundEnt);
            }
            else {
               foundEnt.type = EntType.InactiveLayer;
               foundEnt.value = layer.layerDirName;
               foundEnt.srcTypeName = layer.layerDirName;
               if (foundEnt instanceof DirEnt) {
                  DirEnt de = (DirEnt) foundEnt;
                  de.subDirs.clear();
                  de.entries.clear();
               }
            }
            break;
         }
      }
      return foundEnt;
   }

   TreeEnt addModelToLayerTree(ILanguageModel m, boolean prependPackage) {
      Layer layer = m.layer;
      if (layer == null)
         return null;

      SrcEntry src = m.srcFile;

      String layerName = layer.layerName;

      String layerGroup = CTypeUtil.getPackageName(layerName);
      String layerFile = CTypeUtil.getClassName(layerName);
      DirEnt layerParentEnt = rootLayerDirEnt;
      if (layerGroup != null)
         layerParentEnt = lookupPackage(rootLayerDirEnt.subDirs, layerGroup, EntType.LayerGroup, null, null, true, false);

      DirEnt layerDirEnt = lookupPackage(layerParentEnt.subDirs, layerFile, EntType.LayerGroup, layer.packagePrefix, null, true, false);
      layerDirEnt.srcTypeName = "layerdir:" + layerDirEnt.srcTypeName; // Need to munch this so selecting the dir does not select the file

      String layerType = src.relTypeName;
      String fileDir = CTypeUtil.getPackageName(layerType);
      String fileTail = CTypeUtil.getClassName(layerType);
      TreeEnt ent = new TreeEnt(EntType.Type, fileTail, false, m.getModelTypeDeclaration().getFullTypeName(), layer);
      // If this entity is imported into this layer from outside, set the imported flag
      ent.imported = layer.getImportDecl(fileTail, false) != null;
      ent.prependPackage = prependPackage;
      Object td = ent.typeDeclaration;
      JavaModel jm = null;
      if (td != null)
         jm = ModelUtil.getJavaModel(td);
      ent.hasSrc = layer.findSrcFile(jm == null ? layerType : jm.getSrcFile().relFileName, true) != null;
      if (fileDir == null) {
         layerDirEnt.entries.add(ent);
      }
      else {
         DirEnt fileEnt = lookupPackage(layerDirEnt.subDirs, fileDir, EntType.Type, layer.packagePrefix, layer, true, false);
         fileEnt.entries.add(ent);
      }

      return ent;
   }

   TreeEnt addTypeToLayerTree(ITypeDeclaration itd) {
      if (!(itd instanceof BodyTypeDeclaration))
         return null;

      BodyTypeDeclaration td = (BodyTypeDeclaration) itd;
      Layer layer = td.getLayer();

      if (layer == null)
         return null;

      String layerName = layer.layerName;

      String layerGroup = CTypeUtil.getPackageName(layerName);
      String layerFile = CTypeUtil.getClassName(layerName);
      DirEnt layerParentEnt = rootLayerDirEnt;
      if (layerGroup != null)
         layerParentEnt = lookupPackage(rootLayerDirEnt.subDirs, layerGroup, EntType.LayerGroup, null, null, true, false);

      DirEnt layerDirEnt = lookupPackage(layerParentEnt.subDirs, layerFile, EntType.LayerGroup, layer.packagePrefix, null, true, false);
      layerDirEnt.srcTypeName = "layerdir:" + layerDirEnt.srcTypeName; // Need to munch this so selecting the dir does not select the file

      SrcEntry src = td.getJavaModel().getSrcFile();
      String layerType = CTypeUtil.prefixPath(src.getRelDir(), td.getInnerTypeName());
      String fileDir = CTypeUtil.getPackageName(layerType);
      String fileTail = CTypeUtil.getClassName(layerType);
      TreeEnt ent = new TreeEnt(EntType.Type, fileTail, false, td.getFullTypeName(), layer);
      ent.prependPackage = true;
      ent.layer = layer;
      // If this entity is imported into this layer from outside, set the imported flag
      ent.imported = layer.getImportDecl(fileTail, false) != null;
      ent.hasSrc = true;
      if (fileDir == null) {
         layerDirEnt.entries.add(ent);
      }
      else {
         DirEnt fileEnt = lookupPackage(layerDirEnt.subDirs, fileDir, EntType.Type, layer.packagePrefix, layer, true, false);
         fileEnt.entries.add(ent);
      }
      return ent;
   }

   TreeEnt removeModelFromLayerTree(ILanguageModel m) {
      Layer layer = m.layer;
      if (layer == null)
         return null;

      SrcEntry src = m.srcFile;

      String layerName = layer.layerName;
      String layerGroup = CTypeUtil.getPackageName(layerName);
      String layerFile = CTypeUtil.getClassName(layerName);
      DirEnt layerParentEnt = rootLayerDirEnt;
      if (layerGroup != null)
         layerParentEnt = lookupPackage(rootLayerDirEnt.subDirs, layerGroup, EntType.LayerGroup, null, null, false, false);

      if (layerParentEnt == null)
         return null;

      DirEnt layerDirEnt = lookupPackage(layerParentEnt.subDirs, layerFile, EntType.LayerGroup, layer.packagePrefix, null, false, false);

      if (layerDirEnt == null)
         return null;

      String layerType = src.relTypeName;
      String fileDir = CTypeUtil.getPackageName(layerType);
      String fileTail = CTypeUtil.getClassName(layerType);

      if (fileDir != null) {
         layerDirEnt = lookupPackage(layerDirEnt.subDirs, fileDir, EntType.Type, layer.packagePrefix, layer, true, false);
      }
      if (layerDirEnt == null)
         return null;

      TreeEnt foundEnt = null;
      for (int i = 0; i < layerDirEnt.entries.size(); i++) {
         TreeEnt ent = layerDirEnt.entries.get(i);
         if (ent.value.equals(fileTail)) {
            foundEnt = ent;
            layerDirEnt.entries.remove(i);
            layerDirEnt.removeEntry(foundEnt);
            break;
         }
      }
      return foundEnt;
   }

   TreeEnt removeTypeFromLayerTree(ITypeDeclaration itd) {
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
      DirEnt layerParentEnt = rootLayerDirEnt;
      if (layerGroup != null)
         layerParentEnt = lookupPackage(rootLayerDirEnt.subDirs, layerGroup, EntType.LayerGroup, null, null, false, false);

      if (layerParentEnt == null)
         return null;

      DirEnt layerDirEnt = lookupPackage(layerParentEnt.subDirs, layerFile, EntType.LayerGroup, layer.packagePrefix, null, false, false);

      if (layerDirEnt == null)
         return null;

      String layerType = CTypeUtil.prefixPath(src.getRelDir(), td.getInnerTypeName());
      String fileDir = CTypeUtil.getPackageName(layerType);
      String fileTail = CTypeUtil.getClassName(layerType);

      if (fileDir != null) {
         layerDirEnt = lookupPackage(layerDirEnt.subDirs, fileDir, EntType.Type, layer.packagePrefix, layer, true, false);
      }
      if (layerDirEnt == null)
         return null;

      TreeEnt foundEnt = null;
      for (int i = 0; i < layerDirEnt.entries.size(); i++) {
         TreeEnt ent = layerDirEnt.entries.get(i);
         if (ent.value.equals(fileTail)) {
            foundEnt = ent;
            layerDirEnt.entries.remove(i);
            layerDirEnt.removeEntry(foundEnt);
            break;
         }
      }
      return foundEnt;
   }

   TreeEnt removeLayerFromLayerTree(Layer layer, boolean remove) {
      String layerName = layer.layerName;
      String layerGroup = CTypeUtil.getPackageName(layerName);
      String layerFile = CTypeUtil.getClassName(layerName);
      DirEnt layerParentEnt = rootLayerDirEnt;
      if (layerGroup != null)
         layerParentEnt = lookupPackage(rootLayerDirEnt.subDirs, layerGroup, EntType.LayerGroup, null, null, false, false);

      if (layerParentEnt == null)
         return null;

      DirEnt layerDirEnt = lookupPackage(layerParentEnt.subDirs, layerFile, EntType.LayerGroup, layer.packagePrefix, null, false, false);

      if (layerDirEnt == null)
         return null;

      if (remove) {
         layerParentEnt.subDirs.remove(layerFile);
         layerParentEnt.removeEntry(layerDirEnt);
      }
      else {
         layerDirEnt.type = EntType.InactiveLayer;
         layerDirEnt.value = CTypeUtil.getClassName(layer.layerDirName);
         layerDirEnt.srcTypeName = layer.layerDirName;
         layerDirEnt.subDirs.clear();
         layerDirEnt.entries.clear();
      }

      return layerDirEnt;
   }

   TreeEnt findTypeInLayerTree(BodyTypeDeclaration td) {
      Layer typeLayer = td.getLayer();

      DirEnt layerDirEnt = lookupPackage(rootLayerDirEnt.subDirs, typeLayer.layerName, EntType.LayerGroup, typeLayer.packagePrefix, null, false, false);
      if (layerDirEnt == null)
         return null;

      int i = 0;
      for (TreeEnt ent:layerDirEnt.entries) {
         if (ent.srcTypeName.equals(td.getFullTypeName())) {
            return ent;
         }
         i++;
      }

      return null;
   }

   void addNewModel(ILanguageModel m) {
      if (!typeTreeBuilt || !layerTreeBuilt)
         return;

      TypeDeclaration td = m.getUnresolvedModelTypeDeclaration();
      boolean needsRefresh = false;
      if (td != null) {
         String typeName = td.fullTypeName;
         if (!nodeExists(typeName)) {
            TreeEnt e = addModelToTypeTree(typeName, m.getPrependPackage());
            if (e != null) {
               e.processEntry();
               needsRefresh = true;
            }
         }
         TreeEnt childEnt = findTypeInLayerTree(td);
         if (childEnt != null) {
            if (childEnt.transparent) {
               childEnt.transparent = false;
            }
         }
         else {
            TreeEnt e = addModelToLayerTree(m, m.getPrependPackage());
            if (e != null) {
               e.processEntry();
               needsRefresh = true;
            }
         }
      }
      // Now still need to go and refresh the visible nodes so we add a new one for this guy.
      if (needsRefresh)
         refresh();
   }

   void pruneChildren(BodyTypeDeclaration td) {
      Object[] innerTypes = ModelUtil.getAllInnerTypes(td, null, true);
      if (innerTypes != null) {
         for (Object innerType:innerTypes) {
            if (innerType instanceof BodyTypeDeclaration) {
               BodyTypeDeclaration btd = (BodyTypeDeclaration) innerType;
               String fullTypeName = btd.getFullTypeName();
               if (system.getSrcTypeDeclaration(fullTypeName, null, true) == null)
                  removeTypeFromTypeTree(fullTypeName);
               else
                  pruneChildren(btd);
            }
         }
      }

   }

   abstract boolean nodeExists(String typeName);

   void removeModel(ILanguageModel m) {
      if (!typeTreeBuilt || !layerTreeBuilt)
         return;

      // Has been removed so use the unresolved type here
      TypeDeclaration td = m.getUnresolvedModelTypeDeclaration();
      boolean needsRefresh = false;
      if (td != null) {
         String typeName = td.fullTypeName;
         if (!nodeExists(typeName))
            return;

         TreeEnt e;

         // Only remove from the type tree if this is the last file defining this type
         if (system.getSrcTypeDeclaration(typeName, null, true) == null) {
            e = removeTypeFromTypeTree(typeName);
            if (e != null) {
               needsRefresh = true;
            }

            // In this case, not removing any of the inner types - we detach the tree parent tree node and so discard the children automatically.
         }
         else {
            // But if there's another version of the same type, we do need to see if any sub-objects have been removed.
            pruneChildren(td);
         }

         e = removeModelFromLayerTree(m);
         if (e != null)
            needsRefresh = true;

      }
      // Now still need to go and refresh the visible nodes so we add a new one for this guy.
      if (needsRefresh)
         refresh();
   }

   void addNewType(ITypeDeclaration itd) {
      if (!typeTreeBuilt || !layerTreeBuilt || !(itd instanceof BodyTypeDeclaration))
         return;

      BodyTypeDeclaration td = (BodyTypeDeclaration) itd;

      boolean needsRefresh = false;
      if (td != null) {
         String typeName = td.fullTypeName;
         if (!nodeExists(typeName)) {
            TreeEnt e = addModelToTypeTree(typeName, true);
            if (e != null) {
               e.processEntry();
               needsRefresh = true;
            }
         }
         TreeEnt childEnt = findTypeInLayerTree(td);
         if (childEnt != null) {
            if (childEnt.transparent) {
               childEnt.transparent = false;
            }
         }
         else {
            TreeEnt e = addTypeToLayerTree(td);
            if (e != null) {
               e.processEntry();
               needsRefresh = true;
            }
         }
      }
      // Now still need to go and refresh the visible nodes so we add a new one for this guy.
      if (needsRefresh)
         refresh();
   }

   void removeType(ITypeDeclaration td) {
      if (!typeTreeBuilt || !layerTreeBuilt)
         return;

      boolean needsRefresh = false;
      if (td != null) {
         String typeName = td.fullTypeName;
         if (!nodeExists(typeName))
            return;

         TreeEnt e;

         // Only remove from the type tree if this is the last file defining this type
         if (system.getSrcTypeDeclaration(typeName, null, true) == null) {
            e = removeTypeFromTypeTree(typeName);
            if (e != null) {
               needsRefresh = true;
            }
         }
         e = removeTypeFromLayerTree(td);
         if (e != null)
            needsRefresh = true;
      }
      // Now still need to go and refresh the visible nodes so we add a new one for this guy.
      if (needsRefresh)
         refresh();
   }


   void removeLayer(Layer layer) {
      if (!typeTreeBuilt || !layerTreeBuilt)
         return;

      boolean needsRefresh = false;
      TreeEnt e = removeLayerFromTypeTree(layer, true);
      if (e != null) {
         needsRefresh = true;
      }
      // TODO: and also if we add "removeLayer" set last arg to true
      e = removeLayerFromLayerTree(layer, false);
      if (e != null)
         needsRefresh = true;

      // Now still need to go and refresh the visible nodes so we add a new one for this guy.
      if (needsRefresh)
         refresh();
   }

   DirEnt lookupPackage(Map<String,DirEnt> index, String pkgName, EntType type, String srcPrefix, Layer layer, boolean create, boolean isTypeTree) {
      String root = CTypeUtil.getHeadType(pkgName);
      String tail = CTypeUtil.getTailType(pkgName);

      if (root == null) {
         DirEnt ents = index.get(pkgName);
         if (ents == null) {
            if (!create)
               return null;

            ents = new DirEnt(type, pkgName, isTypeTree, CTypeUtil.prefixPath(srcPrefix, pkgName), layer);
            ents.prependPackage = true;

            String srcTypeName = ents.srcTypeName = CTypeUtil.prefixPath(srcPrefix, pkgName);

            // If this name is defined in an import and we do not have a src file for it, set the imported flag.
            ents.imported = system.getImportDecl(null, null, CTypeUtil.getClassName(srcTypeName)) != null;

            ents.hasSrc = system.getSrcTypeDeclaration(srcTypeName, null, true) != null;

            index.put(pkgName, ents);
         }
         return ents;
      }
      else {
         DirEnt ents = index.get(root);

         // Layer dir's should replace InactiveLayers when we add them.
         if (type == EntType.LayerDir && ents.type == EntType.InactiveLayer)
            ents = null;

         if (ents == null) {
            if (!create)
               return null;

            ents = new DirEnt(type, root, isTypeTree, (type == EntType.LayerGroup ? "layerGroup:" : "") + CTypeUtil.prefixPath(srcPrefix, root), layer);
            ents.imported = false;
            ents.hasSrc = true;
            ents.prependPackage = true;
            index.put(root, ents);
         }

         Map<String,DirEnt> childIndex = ents.subDirs;
         return lookupPackage(childIndex, tail, type, CTypeUtil.prefixPath(srcPrefix, root), layer, create, isTypeTree);
      }
   }

   /** ---- Layer tree ---- */

   boolean rebuildLayerDirEnts() {
      DirEnt rootEnts = new DirEnt(EntType.Root, "By Layer", false, null, null);
      rootLayerDirEnt = rootEnts;

      for (int i = 0; i < system.layers.size(); i++) {
         Layer layer = system.layers.get(i);
         if (layer.getVisibleInEditor() && !isFilteredPackage(layer.packagePrefix))
            addLayerDirEnt(layer);

         if (++layersCreated >= MAX_LAYERS) {
            System.out.println("*** skipping layers due to MAX_LAYERS setting: " + MAX_LAYERS);
            break;
         }

      }

      TreeEnt cent = layerEmptyCommentNode = new TreeEnt(EntType.Comment, "<No visible layers>", false, null, null);

      Map<String,LayerIndexInfo> allLayerIndex = system.getAllLayerIndex();
      for (LayerIndexInfo lii:allLayerIndex.values()) {
          if (!includeInactive)
             break;
         // Do not replace a system layer with one from the index
         //if (system.getLayerByDirName(lii.layerDirName) == null) {
            String layerDirName = lii.layerDirName;
            String layerGroup = CTypeUtil.getPackageName(layerDirName);

            DirEnt pkgEnts;
            if (layerGroup != null)
               pkgEnts = lookupPackage(rootEnts.subDirs, layerGroup, EntType.LayerGroup, null, null, true, false);
            else
               pkgEnts = rootEnts;

            TreeEnt ent = new TreeEnt(EntType.InactiveLayer, CTypeUtil.getClassName(lii.layerDirName), false, lii.layerDirName, null);
            ent.prependPackage = true;

            pkgEnts.entries.add(ent);
         //}
      }
      rootEnts.processEntry();

      return true;
   }

   DirEnt addLayerDirEnt(Layer layer) {
      return addLayerFilesWithName(layer, layer, false);
   }

   int typesCreated = 0;
   int layersCreated = 0;

   void addLayerType(String layerType, Layer srcLayer, Layer fileLayer, boolean transparentLayer, DirEnt layerDirEnt, boolean prependPackage, boolean imported, boolean addInnerTypes) {
       String fileDir = CTypeUtil.getPackageName(layerType);
       String fileTail = CTypeUtil.getClassName(layerType);

       DirEnt layerParent;
       if (fileDir == null) {
          layerParent = layerDirEnt;
       }
       else {
          layerParent = lookupPackage(layerDirEnt.subDirs, fileDir, EntType.Type, fileLayer.packagePrefix, fileLayer, true, false);
       }

       if (transparentLayer) {
          boolean found = false;
          // Do not add elements which are already here
          if (layerParent.hasChild(fileTail))
             return;
       }

       boolean isLayerFile = fileDir == null && fileTail.equals(CTypeUtil.getClassName(srcLayer.layerName));

       if (!transparentLayer || !isLayerFile) {
          TreeEnt ent = new TreeEnt(isLayerFile ? EntType.LayerFile : EntType.Type, fileTail, false, prependPackage && !imported ? CTypeUtil.prefixPath(fileLayer.packagePrefix, layerType) : layerType, fileLayer);

          if (isFilteredType(ent.srcTypeName))
             return;

          // Here we are filtering types just to limit the number for testing - but don't filter if we've already go the type in the type tree.
          // This let's us test more functionality and creates a reasonable subset of the types.
          if (++typesCreated >= MAX_TYPES && getTypeTreeEnt(ent.srcTypeName) == null) {
             System.out.println("*** Skipping type: " + ent.srcTypeName + " due to MAX_TYPES: " + MAX_TYPES);
             return;
          }

          // If this entity is imported into this layer from outside, set the imported flag
          ent.imported = fileLayer.getImportDecl(fileTail, false) != null;
          ent.transparent = transparentLayer;
          ent.prependPackage = prependPackage;
          if (loadInnerTypesAtStartup) {
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

          layerParent.entries.add(ent);

          if (loadInnerTypesAtStartup && addInnerTypes) {
             Object typeDecl = ent.typeDeclaration;
             if (typeDecl != null) {
                Set<String> innerTypes = srcLayer.getInnerTypeNames(layerType, typeDecl, true);
                if (innerTypes != null) {
                   for (String innerType:innerTypes) {
                      addLayerType(innerType, srcLayer, fileLayer, transparentLayer, layerDirEnt, true, false, false);
                   }
                }
             }
          }
       }
   }

   /**
     * The srcLayer is used to retrieve the source file names.  The fileLayer is the layer used to register those files.  They are
     * the same unless transparentLayer is true... in that case, we are adding the files from srcFile to fileLayer's tree unless those files
     * already exist in fileLayer
     */
   DirEnt addLayerFilesWithName(Layer srcLayer, Layer fileLayer, boolean transparentLayer) {
      String layerName = fileLayer.layerName;
      String layerGroup = CTypeUtil.getPackageName(layerName);
      String layerFile = CTypeUtil.getClassName(layerName);
      DirEnt layerParentEnt = rootLayerDirEnt;
      if (layerGroup != null)
         layerParentEnt = lookupPackage(rootLayerDirEnt.subDirs, layerGroup, EntType.LayerGroup, null, null, true, false);

      //TreeEnt ent = new TreeEnt();
      //ent.srcTypeName = layer.layerUniqueName;
      //ent.value = layerFile;
      //ent.type = EntType.Layer;
      //layerParentEnt.entries.add(ent);

      DirEnt layerDirEnt = lookupPackage(layerParentEnt.subDirs, layerFile, EntType.LayerDir, fileLayer.packagePrefix, null, true, false);
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
            DirEnt layerParent;
            if (fileDir == null)
               layerParent = layerDirEnt;
            else
               layerParent = lookupPackage(layerDirEnt.subDirs, fileDir, EntType.Type, fileLayer.packagePrefix, fileLayer, true, false);

            if (!layerParent.hasChild(fileTail)) {
               TreeEnt ent = new TreeEnt(EntType.Type, fileTail, false, layerFullType, fileLayer);
               ent.prependPackage = true;
               // If this entity is imported into this layer from outside, set the imported flag
               ent.imported = true;
               ent.hasSrc = fileLayer.findSrcFile(layerType.replace('.', '/'), true) != null;
               layerParent.entries.add(ent);
            }
         }
      }

      return layerDirEnt;
   }

   void addNewLayer(Layer layer) {
      if (!typeTreeBuilt || !layerTreeBuilt)
         return;

      DirEnt ent = addLayerDirEnt(layer);
      ent.processEntry();

      // Then build our DirEnt structure from the Set of src type names we get back
      Set<String> srcTypeNames = layer.getSrcTypeNames(true, true, false, true);
      for (String srcTypeName:srcTypeNames) {
         // TODO: need to fix the setting of prependPackage - to handle files in the type tree
         TreeEnt e = addModelToTypeTree(srcTypeName, true);
      }
      // Re-process everything?
      rootTypeDirEnt.processEntry();

      refresh();
   }

   TreeEnt {
      // When needsType is true, we set the cachedTypeDeclaration property which syncs it to the client
      needsType =: fetchType();

      void fetchType() {
         if (needsType) {
            cachedTypeDeclaration = getTypeDeclaration();
            processEntry();
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
            Layer layer = system.getLayerByTypeName(typeName);
            if (layer != null) {
               return layer.model.getModelTypeDeclaration();
            }
            return null;
         }

         // First try to get the src version
         Object res = system.getSrcTypeDeclaration(typeName, null, prependPackage);
         // Fall back to the class but only for things which are real types.
         if (res == null && prependPackage)
            res = system.getClassWithPathName(typeName);
         return res;
      }

      boolean getTypeIsVisible() {
         // TODO: method to find the set of layers for a file name - filter those layers
         if (cachedTypeDeclaration != null || loadInnerTypesAtStartup) {
            Object type = getTypeDeclaration();
            if (type != null) {
               if (!ModelUtil.matchesLayerFilter(type, codeTypes, codeFunctions)) {
                  return false;
               }
               if (!createMode && !ModelUtil.isApplicationType(type))
                  return false;
            }
         }
         return true;
      }

      void processEntry() {
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
               newIcon = findIcon();
               if (newIcon != null && newIcon != icon)
                  icon = newIcon;
               return;
            case LayerDir:
            case LayerFile:
               if (layer != null) {
                  // We already pull these out of the layer so no need to set them here.  They are used in TypeIsVisible which is not used for layers anyway.
                  //entCodeTypes = new ArrayList<CodeType>(Collections.singletonList(layer.codeType));
                  //entCodeFunctions = new ArrayList<CodeFunction>(Collections.singletonList(layer.codeFunction));
                  newIcon = findIcon();
                  if (newIcon != null && newIcon != icon)
                     icon = newIcon;
               }
               return;
         }
         if (newIcon != null && newIcon != icon)
            icon = newIcon;

         Object typeDecl = loadInnerTypesAtStartup ? getTypeDeclaration() : cachedTypeDeclaration;
         if (typeDecl != null) {
            EntType newType = null;
            switch (ModelUtil.getDeclarationType(typeDecl)) {
               case CLASS:
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
               entCodeFunctions = new ArrayList<CodeFunction>();
               if (isTypeTree || layer == null) {
                   ModelUtil.getFiltersForType(typeDecl, entCodeTypes, entCodeFunctions, isTypeTree);
               }
               // For typeDir ents, we don't want the most specific layer of the type only the layer associated with the directory
               else {
                  entCodeTypes.add(layer.codeType);
                  entCodeFunctions.add(layer.codeFunction);
               }
            }
         }
         else {
            Layer layer = system.getLayerByName(srcTypeName);
            if (layer == null) {
               layer = system.getLayerByTypeName(srcTypeName);
            }
            if (layer != null) {
               type = EntType.LayerFile;
            }
            // Package
         }
         icon = findIcon();
      }

      boolean isDynamic() {
         Object type = loadInnerTypesAtStartup ? getTypeDeclaration() : getCachedTypeDeclaration();
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
         }
         return null;
      }
   }

   DirEnt {
      subDirs = new LinkedHashMap<String,DirEnt>();
      entries = new ArrayList<TreeEnt>();

      void processEntry() {
         super.processEntry();

         Collections.sort(entries);
         for (DirEnt childEnt:subDirs.values()) {
             childEnt.processEntry();
         }
         // Find all of the sub-dirs which have sub-types for them
         for (TreeEnt childEnt:entries) {
            childEnt.processEntry();
         }

         // Accumulate the entCodeTypes and Functions from all child nodes so that we know whether or not to
         // show a DirEnt even when it's subDirs and entries are not populated on the client.
         if (entCodeTypes == null || entCodeFunctions == null) {
            entCodeTypes = new ArrayList<CodeType>();
            entCodeFunctions = new ArrayList<CodeFunction>();
            for (DirEnt childEnt:subDirs.values()) {
               addCodeTypes(childEnt.entCodeTypes, entCodeTypes);
               addCodeFunctions(childEnt.entCodeFunctions, entCodeFunctions);
            }
            // Find all of the sub-dirs which have sub-types for them
            for (TreeEnt childEnt:entries) {
               addCodeTypes(childEnt.entCodeTypes, entCodeTypes);
               addCodeFunctions(childEnt.entCodeFunctions, entCodeFunctions);
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

      private void addCodeFunctions(ArrayList<CodeFunction> src, ArrayList<CodeFunction> dst) {
         if (src == null)
            return;
         for (int i = 0; i < src.size(); i++) {
            CodeFunction s = src.get(i);
            if (!dst.contains(s))
               dst.add(s);
         }
      }

      void fetchType() {
         if (needsType) {
            super.fetchType();
            if (cachedTypeDeclaration != null) {
               initChildren();
            }
         }
      }
   }

   void stop() {
      if (listener != null) {
         system.removeNewModelListener(listener);
      }
   }
}
