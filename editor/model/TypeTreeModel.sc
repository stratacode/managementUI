import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.EnumSet;
import java.util.Collections;

import sc.layer.Layer;
import sc.layer.SrcEntry;

import sc.util.ArrayList;
import sc.util.LinkedHashMap;
import sc.type.CTypeUtil;

import sc.obj.Constant;

import sc.lang.java.ModelUtil;
import sc.lang.InstanceWrapper;

import sc.layer.CodeType;

import sc.dyn.DynUtil;

import sc.sync.SyncManager;

@sc.obj.Component
@sc.obj.CompilerSettings(constructorProperties="editorModel,viewType")
class TypeTreeModel {
   EditorModel editorModel;
   ViewType viewType;

   LayeredSystem system;

   ArrayList<CodeType> codeTypes :=: editorModel.codeTypes;

   // Adds to the set of layers you include in the index.  These will be in active layers.
   String[] specifiedLayerNames;

   boolean createMode = false;
   boolean propertyMode = false; // when create mode is true, are we creating properties or types?

   CreateMode currentCreateMode;
   boolean addLayerMode = false;  // Exclusive with the other two
   boolean createLayerMode = false; // When layerMode is true, are we including or creating?
   boolean layerMode := createLayerMode || addLayerMode;

   /** Should we also include instances in the type tree.  When included, they are children under their type */
   @Sync(syncMode=SyncMode.Disabled)
   boolean includeInstances = true;

   transient boolean valid = true;
   transient boolean rebuildFirstTime = true;
   transient boolean refreshInProgress = false;

   transient boolean uiBuilt = false;

   transient boolean needsUpdateSelection = false;

   public final static String PKG_INDEX_PREFIX = "<pkg>:";

   @Constant
   ArrayList<String> includePackages;
   @Constant
   ArrayList<String> excludePackages;

   //TypeTree typeTree = new TypeTree(this);
   //ByLayerTypeTree byLayerTypeTree = new ByLayerTypeTree(this);

   @sc.obj.Component
   object typeTree extends TypeTree {
      treeModel = TypeTreeModel.this;
   }

   @sc.obj.Component
   object byLayerTypeTree extends ByLayerTypeTree {
      treeModel = TypeTreeModel.this;
   }

   List<TypeTree> typeTrees = new ArrayList<TypeTree>();
   {
      typeTrees.add(typeTree);
      typeTrees.add(byLayerTypeTree);
   }

   // Rules controlling when to refresh.  
   codeTypes =: refresh();

   // When the current type in the model changes, if we're in create mode we need to refresh to reflect the newly visible/highlighted elements.
   editorModel =: createMode || layerMode ? refresh() : null;

   createMode =: onSelectionChanged();
   addLayerMode =: onSelectionChanged();
   createLayerMode =: onSelectionChanged();
   currentCreateMode =: onSelectionChanged();

   // Need to refresh when any new instances are created.  TODO performance: we could reduce the scope of the refresh if necessary here since this might happen a lot in some applications
   int refreshInstancesCt := editorModel.refreshInstancesCt;
   refreshInstancesCt =: refreshInstances();

   void onSelectionChanged() {
      editorModel.selectionChanged++;
      markSelectionChanged();
   }

   int selectionChanged := editorModel.selectionChanged;
   selectionChanged =: markSelectionChanged();

   void markSelectionChanged() {
      needsUpdateSelection = true;
      refresh();
   }

   void refreshInstances() {
      if (rebuildFirstTime || !valid)
         return;
      refresh();
   }

   void refresh() {
      if (refreshInProgress)
         return;
      // IF we have an empty tree during initialization it resets the "open" state for the startup node
      if (rebuildFirstTime) {
         valid = false;
         rebuildFirstTime = false;
         rebuild();
         return;
      }
      if (valid) {
         valid = false;

         scheduleBuild();
      }
   }

   // On the client, this will run after a 0 millisecond timeout.  
   // On the server, this runs at the end of the request.
   void scheduleBuild() {
      DynUtil.invokeLater(new Runnable() {
         public void run() {
            rebuild();
         }
      }, 9);
   }

   void rebuild() {
      if (refreshInProgress || valid)
         return;

      includeInstances = !propertyMode && !createMode && !addLayerMode && !createLayerMode && viewType == ViewType.DataViewType;

      refreshInProgress = true;
      valid = true;

      try {
         for (TypeTree typeTree:typeTrees) {
            typeTree.refreshTree();
         }

         if (needsUpdateSelection) {
            for (TypeTree typeTree:typeTrees) {
               if (typeTree.selectionListener != null)
                  typeTree.selectionListener.updateListSelection();
            }
         }
      }
      catch (RuntimeException exc) {
         System.err.println("*** error refreshing tree model: " + exc.toString());
         exc.printStackTrace();
      }
      finally {
         refreshInProgress = false;
      }
   }

   // On the client we can't rebuild these - they get populated from the server on a sync.
   boolean rebuildTypeDirEnts() {
      return false;
   }

   boolean rebuildLayerDirEnts() {
      return false;
   }

   void updateInstancesForType(String typeName, boolean byLayer) {
      List<TypeTree.TreeNode> typeNodes = (byLayer ? byLayerTypeTree : typeTree).rootTreeIndex.get(typeName);
      if (typeNodes != null) {
         for (TypeTree.TreeNode typeNode:typeNodes) {
            typeNode.ent.instanceSelected = true;
            typeNode.ent.updateInstances();
            typeNode.ent.refreshChildren();
         }
      }
   }

   boolean nodeExists(String typeName) {
      return typeTree.rootTreeIndex.get(typeName) != null;
   }
}
