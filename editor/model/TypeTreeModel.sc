import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.EnumSet;
import java.util.Collections;

import sc.layer.Layer;
import sc.layer.SrcEntry;

import sc.util.ArrayList;
import sc.util.LinkedHashMap;
import sc.util.StringUtil;
import sc.type.CTypeUtil;

import sc.obj.Constant;

import sc.lang.java.ModelUtil;
import sc.lang.InstanceWrapper;

import sc.layer.CodeType;

import sc.dyn.DynUtil;

import sc.sync.SyncManager;

@sc.obj.Component
class TypeTreeModel {
   EditorModel editorModel;
   LayeredSystem system;

   ArrayList<CodeType> codeTypes :=: editorModel.codeTypes;

   // Adds to the set of layers you include in the index.  These will be in active layers.
   String[] specifiedLayerNames;

   boolean createMode = false;
   boolean propertyMode = false; // when create mode is true, are we creating properties or types?

   boolean addLayerMode = false;  // Exclusive with the other two
   boolean createLayerMode = false; // When layerMode is true, are we including or creating?
   boolean layerMode := createLayerMode || addLayerMode;

   /** Should we also include instances in the type tree.  When included, they are children under their type */
   boolean includeInstances = true;

   transient boolean valid = true;
   transient boolean rebuildFirstTime = true;
   transient boolean refreshInProgress = false;

   transient boolean uiBuilt = false;

   @Constant
   ArrayList<String> includePackages;
   @Constant
   ArrayList<String> excludePackages;

   //TypeTree typeTree = new TypeTree(this);
   //ByLayerTypeTree byLayerTypeTree = new ByLayerTypeTree(this);

   object typeTree extends TypeTree {
      treeModel = TypeTreeModel.this;
   }
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

   includeInstances =: refresh();

   // When the current type in the model changes, if we're in create mode we need to refresh to reflect the newly visible/highlighted elements.
   editorModel =: createMode || layerMode ? refresh() : null;

   createMode =: selectionChanged();
   addLayerMode =: selectionChanged();
   createLayerMode =: selectionChanged();
   propertyMode =: refresh();

   // Need to refresh when any new instances are created.  TODO performance: we could reduce the scope of the refresh if necessary here since this might happen a lot in some applications
   int refreshInstancesCt := editorModel.refreshInstancesCt;
   refreshInstancesCt =: refreshInstances();

   void selectionChanged() {
      editorModel.selectionChanged++;
      refresh();
   }

   void refreshInstances() {
      if (rebuildFirstTime || !valid)
         return;
      refresh();
   }

   void refresh() {
      // IF we have an empty tree during initialization it resets the "open" state for the startup node
      if (rebuildFirstTime) {
         valid = false;
         rebuild();
         rebuildFirstTime = false;
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

      refreshInProgress = true;
      valid = true;

      try {
         for (TypeTree typeTree:typeTrees) {
            typeTree.refreshTree();
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
}
