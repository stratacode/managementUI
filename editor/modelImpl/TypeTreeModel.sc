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
         /*
         void instancedAdded(ITypeDeclaration td, Object inst) {
            addNewInstance(td, inst);
         }
         void instancedRemoved(ITypeDeclaration td, Object inst) {
         }
         */
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
   static final int MAX_TYPES = 20000; // 100;
   static final int MAX_LAYERS = 20000; // 10;

   boolean includeInactive = false;
   boolean includePrimitives = false;

   boolean isFilteredType(String typeName) {
      if (includePackages == null && excludePackages == null)
         return false;
      if (typeName == null)
         return true;
      if (excludePackages != null) {
         for (String pkg:excludePackages) {
            if (typeName.startsWith(pkg))
               return true;
         }
      }
      if (includePackages != null) {
         for (String pkg:includePackages) {
            if (typeName.startsWith(pkg))
               return false;
         }
      }
      else
         return false;
      return true;
   }

   boolean isFilteredPackage(String pkgName) {
      if (includePackages == null)
         return false;
      if (pkgName == null)
         return false;
      for (String pkgFilter:includePackages) {
         if (pkgName.startsWith(pkgFilter) || pkgFilter.startsWith(pkgName))
            return false;
      }
      return true;
   }

   Set<String> getSrcTypeNames() {
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
      return srcTypeNames;
   }

   void addNewModel(ILanguageModel m) {
      if (!uiBuilt)
         return;

      TypeDeclaration td = m.getUnresolvedModelTypeDeclaration();
      boolean needsRefresh = false;
      if (td != null) {
         String typeName = td.fullTypeName;
         if (!nodeExists(typeName)) {
            for (TypeTree typeTree:typeTrees) {
               // TODO: hide this difference inside of the TypeTree implementation?
               if (!(typeTree instanceof ByLayerTypeTree)) {
                  TypeTree.TreeEnt e = typeTree.addModel(typeName, m.getPrependPackage());
                  if (e != null) {
                     e.processEntry();
                     needsRefresh = true;
                  }
               }
               else {
                  TypeTree.TreeEnt childEnt = ((ByLayerTypeTree) typeTree).findType(td);
                  if (childEnt != null) {
                     if (childEnt.transparent) {
                        childEnt.transparent = false;
                     }
                  }
                  else {
                     TypeTree.TreeEnt e = ((ByLayerTypeTree) typeTree).addModel(m, m.getPrependPackage());
                     if (e != null) {
                        e.processEntry();
                        needsRefresh = true;
                     }
                  }
               }
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
               if (system.getSrcTypeDeclaration(fullTypeName, null, true) == null) {
                  for (TypeTree typeTree:typeTrees)
                     typeTree.removeType(fullTypeName);
               }
               else
                  pruneChildren(btd);
            }
         }
      }
   }

   void removeModel(ILanguageModel m) {
      if (!uiBuilt)
         return;

      // Has been removed so use the unresolved type here
      TypeDeclaration td = m.getUnresolvedModelTypeDeclaration();
      boolean needsRefresh = false;
      if (td != null) {
         String typeName = td.fullTypeName;
         if (!nodeExists(typeName))
            return;

         TypeTree.TreeEnt e;

         // Only remove from the type tree if this is the last file defining this type
         if (system.getSrcTypeDeclaration(typeName, null, true) == null) {
            for (TypeTree typeTree:typeTrees) {
               e = typeTree.removeType(typeName);
               if (e != null) {
                  needsRefresh = true;
               }
            }

            // In this case, not removing any of the inner types - we detach the tree parent tree node and so discard the children automatically.
         }
         else {
            // But if there's another version of the same type, we do need to see if any sub-objects have been removed.
            pruneChildren(td);
         }

         for (TypeTree typeTree:typeTrees) {
            // TODO: move down this method into TypeTree
            if (typeTree instanceof ByLayerTypeTree) {
               e = ((ByLayerTypeTree)typeTree).removeModel(m);
               if (e != null)
                  needsRefresh = true;
            }
         }

      }
      // Now still need to go and refresh the visible nodes so we add a new one for this guy.
      if (needsRefresh)
         refresh();
   }

   void addNewType(ITypeDeclaration itd) {
      if (!uiBuilt || !(itd instanceof BodyTypeDeclaration))
         return;

      BodyTypeDeclaration td = (BodyTypeDeclaration) itd;

      boolean needsRefresh = false;
      if (td != null) {
         String typeName = td.fullTypeName;
         for (TypeTree typeTree:typeTrees) {
            if (!(typeTree instanceof ByLayerTypeTree)) {
               if (!nodeExists(typeName)) {
                  TypeTree.TreeEnt e = typeTree.addModel(typeName, true);
                  if (e != null) {
                     e.processEntry();
                     needsRefresh = true;
                  }
               }
            }
            else {
               ByLayerTypeTree blTree = (ByLayerTypeTree) typeTree;
               TypeTree.TreeEnt childEnt = blTree.findType(td);

               if (childEnt != null) {
                  if (childEnt.transparent) {
                     childEnt.transparent = false;
                  }
               }
               else {
                  TypeTree.TreeEnt e = blTree.addType(td);
                  if (e != null) {
                     e.processEntry();
                     needsRefresh = true;
                  }
               }
            }
         }
      }
      // Now still need to go and refresh the visible nodes so we add a new one for this guy.
      if (needsRefresh)
         refresh();
   }

   void removeType(ITypeDeclaration td) {
      if (!uiBuilt)
         return;

      boolean needsRefresh = false;
      if (td != null) {
         String typeName = td.fullTypeName;
         if (!nodeExists(typeName))
            return;

         TypeTree.TreeEnt e;

         for (TypeTree typeTree:typeTrees) {
            if (!(typeTree instanceof ByLayerTypeTree)) {
               // Only remove from the type tree if this is the last file defining this type
               if (system.getSrcTypeDeclaration(typeName, null, true) == null) {
                  e = typeTree.removeType(typeName);
                  if (e != null) {
                     needsRefresh = true;
                  }
               }
            }
            else {
               e = ((ByLayerTypeTree) typeTree).removeType(td);
               if (e != null)
                  needsRefresh = true;
            }
         }
      }
      // Now still need to go and refresh the visible nodes so we add a new one for this guy.
      if (needsRefresh)
         refresh();
   }


   void removeLayer(Layer layer) {
      if (!uiBuilt)
         return;

      boolean needsRefresh = false;

      for (TypeTree typeTree:typeTrees) {
         TypeTree.TreeEnt e = typeTree.removeLayer(layer, true);
         if (e != null) {
            needsRefresh = true;
         }
      }

      // Now still need to go and refresh the visible nodes so we add a new one for this guy.
      if (needsRefresh)
         refresh();
   }

   int typesCreated = 0;
   int layersCreated = 0;

   void addNewLayer(Layer layer) {
      if (!uiBuilt)
         return;

      if (byLayerTypeTree != null) {
         TypeTree.TreeEnt ent = byLayerTypeTree.addLayerDirEnt(layer);
         ent.processEntry();
      }

      if (typeTree != null) {
         // Then build our DirEnt structure from the Set of src type names we get back
         Set<String> srcTypeNames = layer.getSrcTypeNames(true, true, false, true);
         for (String srcTypeName:srcTypeNames) {
            // TODO: need to fix the setting of prependPackage - to handle files in the type tree
            TypeTree.TreeEnt e = typeTree.addModel(srcTypeName, true);
         }
         // Re-process everything?
         typeTree.rootDirEnt.processEntry();
      }

      refresh();
   }


   /*
   void addNewInstance(ITypeDeclaration itd, Object inst) {
      if (!uiBuilt || !(itd instanceof BodyTypeDeclaration))
         return;

      BodyTypeDeclaration td = (BodyTypeDeclaration) itd;

      boolean needsRefresh = false;
      if (td != null) {
         String typeName = td.fullTypeName;
         for (TypeTree typeTree:typeTrees) {
            List<TypeTree.TreeNode> typeEnts = typeTree.rootTreeIndex.get(typeName);
            if (typeEnts != null) {
               for (TypeTree.TreeNode node:typeEnts)
                  if (node.ent.updateInstances())
                     needsRefresh = true;
            }
         }
      }

      if (needsRefresh)
         refresh();
   }
   */

   void stop() {
      if (listener != null) {
         system.removeNewModelListener(listener);
      }
   }
}
