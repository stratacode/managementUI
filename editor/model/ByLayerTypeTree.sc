import sc.lang.ILanguageModel;
import sc.lang.java.ModelUtil;
import sc.lang.java.BodyTypeDeclaration;
import sc.lang.java.ITypeDeclaration;

class ByLayerTypeTree extends TypeTree {
   public ByLayerTypeTree(TypeTreeModel model) {
      super(model);
   }

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

   String getRootName() {
      String rootName;
      if (treeModel.createMode) {
         if (treeModel.propertyMode)
            rootName = "Select Property Type by Layer";
         else if (treeModel.addLayerMode)
            rootName = "Select Layer to Include by File";
         else if (treeModel.createLayerMode)
            rootName = "Select Extends Layers by File";
         else
            rootName = "Select Extends Type by Layer";
      }
      else
         rootName = "Application Types by Layer";
      return rootName;
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

   void addLayerType(String layerType, Layer srcLayer, Layer fileLayer, boolean transparentLayer, TreeEnt layerDirEnt, boolean prependPackage, boolean imported, boolean addInnerTypes) {
       String fileDir = CTypeUtil.getPackageName(layerType);
       String fileTail = CTypeUtil.getClassName(layerType);

       TreeEnt layerParent;
       if (fileDir == null) {
          layerParent = layerDirEnt;
       }
       else {
          layerParent = lookupPackage(layerDirEnt, fileDir, EntType.Type, fileLayer.packagePrefix, fileLayer, true, false);
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

          layerParent.addChild(ent);

          if (treeModel.loadInnerTypesAtStartup && addInnerTypes) {
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

   public String getIdPrefix() {
       return "L";
   }
}