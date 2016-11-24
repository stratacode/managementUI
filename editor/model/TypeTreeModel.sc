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
import sc.layer.CodeFunction;

import sc.dyn.DynUtil;

import sc.sync.SyncManager;

@sc.obj.Component
class TypeTreeModel {
   EditorModel editorModel;
   LayeredSystem system;

   ArrayList<CodeType> codeTypes :=: editorModel.codeTypes;
   ArrayList<CodeFunction> codeFunctions :=: editorModel.codeFunctions;

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

   transient boolean typeTreeBuilt = false, layerTreeBuilt = false;

   // These are the two main trees of TreeEnt objects.  This tree is not directly displayed but is referenced from the TreeNode classes which are displayed.
   TreeEnt rootTypeDirEnt;
   TreeEnt rootLayerDirEnt;

   TypeTreeSelectionListener typeListener;
   TypeTreeSelectionListener layerListener;

   TreeEnt typeEmptyCommentNode, layerEmptyCommentNode;

   // Rules controlling when to refresh.  
   codeTypes =: refresh();
   codeFunctions =: refresh();

   includeInstances =: refresh();

   // When the current type in the model changes, if we're in create mode we need to refresh to reflect the newly visible/highlighted elements.
   editorModel =: createMode || layerMode ? refresh() : null;

   enum EntType {
      Root,
      Comment,
      LayerGroup,
      LayerDir,
      LayerFile,
      InactiveLayer, // Layer in the index of all layers - one which has not yet been adding to the system
      ParentType,    // a class with children
      Type,          // a leaf class/object
      ParentObject,
      Object,
      ParentEnum,
      Enum,
      ParentEnumConstant,
      EnumConstant,
      ParentInterface,
      Interface,
      Package,
      Primitive,
      Instance;
   }

   @sc.obj.Sync(onDemand=true)
   class TreeEnt implements Comparable<TreeEnt>, sc.obj.IObjectId {
      EntType type;
      String value;
      String srcTypeName;
      Layer layer;
      // For TreeEnts which represent instances, the wrapper for the instance
      InstanceWrapper instance;

      @sc.obj.Sync(onDemand=true)
      HashMap<String,TreeEnt> childEnts;
      // Parallel to the childEnts map, but retains sort order for display
      ArrayList<TreeEnt> childList;
      ArrayList<TreeEnt> removed = null;

      boolean imported;
      boolean hasSrc;
      boolean transparent; // Entry does not exist on the file system yet
      boolean isTypeTree; // Or the layer tree
      boolean prependPackage; // Is this a type in the type tree or a file like web.xml which does not use a type name
      boolean marked; // Temp flag used to mark in-use objects
      String objectId;

      TreeEnt(EntType type, String value, boolean isTypeTree, String srcTypeName, Layer layer) {
         this.type = type;
         this.value = value;
         this.isTypeTree = isTypeTree;
         this.srcTypeName = srcTypeName;
         this.layer = layer;
      }

      ArrayList<CodeType> entCodeTypes; // Which types and functions is this ent visible?
      ArrayList<CodeFunction> entCodeFunctions;

      // The value of getTypeDeclaration, once it's been fetched
      Object cachedTypeDeclaration;

      cachedTypeDeclaration =: typeAvailable();

      // Set to true when you need the type fetched
      boolean needsType = false;

      boolean selected = false;

      boolean closed = false; // Have we explicitly closed this node.  if so, don't reopen it

      boolean createModeSelected = false;

      UIIcon icon;

      boolean hasVisibleChildren;

      children =: refresh();

      String toString() {
         return value;
      }

      void toggleOpen() {
         // Root element is always open
         if (type == EntType.Root)
            return;
         if (!open) 
            open = true;
         else {
            open = false;
            closed = true; // Track when we explicit close it and then don't re-open it again
         }
         selectType(false);
      }

      private boolean open = false;

      void setOpen(boolean newOpen) {
         boolean orig = open;
         open = newOpen;
         if (!orig && newOpen) {
            initChildren();
         }
      }

      boolean getOpen() {
         return open;
      }

      public void initChildren() {
         // subDirs is marked @Sync(onDemand=true) so it's left out of the sync when the
         // parent object is synchronized.  When a user opens the node, the startSync call begins
         // synchronizing this property.  On the client this causes a fetch of the data.
         // On the server, it pushes this property to the client on the next sync.
         SyncManager.startSync(this, "childEnts");
      }

      void selectType(boolean append) {
         if (isTypeTree) {
            if (typeListener != null)
               typeListener.selectTreeEnt(this, append);
         }
         else {
            if (layerListener != null)
               layerListener.selectTreeEnt(this, append);
         }
      }

      void typeAvailable() {
         if (cachedTypeDeclaration == null)
            return;
         if (isTypeTree) {
            if (typeListener != null)
               typeListener.treeTypeAvailable(this);
         }
         else {
            if (layerListener != null)
               layerListener.treeTypeAvailable(this);
         }
      }

      String getTypeName() {
         String typeName = srcTypeName;
         //if (layer != null)
         //   typeName = TypeUtil.prefixPath(layer.packagePrefix, srcTypeName);

         return typeName;
      }

      String getPackageName() {
         Object type = getTypeDeclaration();
         if (type != null)
            return ModelUtil.getPackageName(type);
         return null;
      }

      boolean isSelectable() {
         return type != EntType.LayerGroup && type != EntType.Root;
      }

      int compareTo(TreeEnt c) {
         return value.compareTo(c.value);
      }

      boolean getNeedsOpenClose() {
         return type != EntType.Root && hasChildren;
      }

      public boolean isVisible(boolean byLayer) {
          // Always keep the selected guys visible
          if (editorModel.isTypeNameSelected(getTypeName()))
             return true;

          // This is a per-layer type so only show it if the layer matches
          if (layer != null && !layer.matchesFilter(codeTypes, codeFunctions))
             return false;

          if (srcTypeName != null) {
             switch (type) {
                case Root:
                case Comment:
                   return true;
                case Package:
                case LayerGroup: // A folder that includes layer directories
                   if (!hasAVisibleChild(byLayer))
                      return false;
                   return getTypeIsVisible();

                case InactiveLayer:
                   // Can't add a layer twice - only show inactive layers which are really not active
                   return layerMode && system.getLayerByDirName(value) == null;

                case LayerDir:   // A layer directory itself
                   if (addLayerMode) // the layer filters were applied above.  When adding layers though, don't show layers that are already there
                      return false;
                   return getTypeIsVisible();

                case LayerFile:
                   if (createMode) {
                      // In the type tree, we display the layer file.  In the layer tree, We display the layerDir as the layer in layer mode
                      if (layerMode)
                         return !byLayer && !addLayerMode;
                      else
                         return false;
                   }
                   return getTypeIsVisible();

                case ParentObject:
                   if (layerMode)
                      return false;
                   if (hasAVisibleChild(byLayer))
                      return true;
                case Object:
                   if (createMode || layerMode)
                       return false;
                   // FALL THROUGH to do type processing in app type mode.
                case ParentType:
                case ParentEnum:
                case ParentEnumConstant:
                case ParentInterface:
                   if (layerMode)
                      return false;

                   if (hasAVisibleChild(byLayer))
                      return true;
                case Interface:
                case Enum:
                case EnumConstant:
                case Type:
                   if (layerMode)
                      return false;

                   if (!getTypeIsVisible())
                      return false;

                   // Create mode:
                   //    show imported types or those which are in the current layer, or those imported which are defined in this layer
                   // Application mode:
                   //    just make sure we have the src for the type.  We'll have already doen the application check above.
                   //    if it's a transparent item, even if hasSrc is false we display it.
                   return (createMode && (imported || editorModel.currentLayer == null || layer == editorModel.currentLayer)) || hasSrc || transparent;

                case Primitive:
                   return createMode && propertyMode;
                case Instance:
                   return includeInstances;
             }
          }
          return true;
      }

      Object getTypeDeclaration() {
         if (cachedTypeDeclaration != null)
            return cachedTypeDeclaration;
         if (typeName == null)
            return null;
         return DynUtil.resolveName(typeName, false);
      }

      boolean getTypeIsVisible() {
         if (entCodeTypes != null && codeTypes != null) {
            boolean vis = false;
            for (int i = 0; i < entCodeTypes.size(); i++) {
               if (codeTypes.contains(entCodeTypes.get(i))) {
                  vis = true;
                  break;
               }
            }
            if (!vis)
               return false;
         }
         if (entCodeFunctions != null && codeFunctions != null) {
            boolean vis = false;
            for (int i = 0; i < entCodeFunctions.size(); i++) {
               if (codeFunctions.contains(entCodeFunctions.get(i))) {
                  vis = true;
                  break;
               }
            }
            if (!vis)
               return false;
         }
         return true;
      }

      @Constant
      String getObjectId() {
         if (objectId != null)
            return objectId;
         if (type == null || value == null)
            return null;
         String valuePart = CTypeUtil.escapeIdentifierString(srcTypeName == null ? (value == null ? "" : "_" + value.toString()) : "_" + srcTypeName);
         String typePart = type == null ? "_unknown" : "_" + type;
         String layerPart = layer == null ? "" : "_" + CTypeUtil.escapeIdentifierString(layer.layerName);
         objectId = "TE" + (isTypeTree ? "T" : "L") + typePart + layerPart + valuePart;
         return objectId;
      }

      void addChild(TreeEnt ent) {
         if (childEnts == null)
            childEnts = new HashMap<String,TreeEnt>();

         TreeEnt old = childEnts.put(ent.nodeId, ent);
         if (childList == null)
            childList = new ArrayList<TreeEnt>();
         if (old != null)
            childList.remove(old);
         childList.add(ent);
      }

      void removeChild(TreeEnt ent) {
         childEnts.remove(ent.nodeId);
         if (childList != null)
            childList.remove(ent);
         removeEntry(ent);
      }

      void removeChildren() {
         if (childEnts != null)
            childEnts.clear();
         if (childList != null)
            childList.clear();
      }

      void removeEntry(TreeEnt toRem) {
         if (removed == null) {
            removed = new ArrayList<TreeEnt>(1);
         }
         removed.add(toRem);
      }

      boolean hasChild(String value) {
         if (childEnts != null) {
            for (TreeEnt childEnt:childEnts.values()) {
               if (childEnt.value.equals(value))
                  return true;
            }
         }
         return false;
      }

      boolean getHasChildren() {
         return true;
      }

      public int getNumChildren() {
         return (childEnts == null ? 0 : childEnts.size());
      }

      public List<TreeEnt> getChildren() {
         ArrayList<TreeEnt> children = new ArrayList<TreeEnt>();
         if (childEnts != null)
            children.addAll(childEnts.values());
         return children;
      }

      boolean hasAVisibleChild(boolean byLayer) {
         switch (type) {
            case Root:
               return true; // should the root always be visible?
         }
         // Not yet fetched so we need to assume there is something visible here
         if (childEnts == null)
             return hasVisibleChildren;
         for (TreeEnt childEnt:childEnts.values()) {
             if (childEnt.isVisible(byLayer) || childEnt.hasAVisibleChild(byLayer))
                return hasVisibleChildren = true;
         }
         return hasVisibleChildren = false;
      }

      void findTypeTreeEnts(List<TreeEnt> res, String typeName) {
         if (this.typeName != null && this.typeName.equals(typeName)) {
            res.add(this);
         }
         if (childEnts != null) {
            for (TreeEnt childEnt:childEnts.values()) {
               childEnt.findTypeTreeEnts(res, typeName);
            }
         }
      }

      boolean updateSelected() {
         boolean needsRefresh = false;
         if (typeName == null) {
            // Once there's a current type, all directories are deselected
            if (editorModel.typeNames.length > 0)
               selected = false;
            // Leave the folder selected as long as it marks the current package
            else if (!DynUtil.equalObjects(srcTypeName, editorModel.currentPackage))
               selected = false;
            return false;
         }
         boolean newSel = editorModel.isTypeNameSelected(typeName);
         if (newSel != selected)
            selected = newSel;
         boolean newCreate = editorModel.isCreateModeTypeNameSelected(typeName);
         if (newCreate != createModeSelected)
            createModeSelected = newCreate;
         if (needsOpen() && !open && !closed) {
            open = true;
            needsRefresh = true;
         }
         if (childEnts != null) {
            for (TreeEnt childEnt:childEnts.values()) {
               if (childEnt.updateSelected())
                  needsRefresh = true;
               // auto-open trees when child nodes are selected
               if (!open && childEnt.needsOpen()) {
                   open = true;
                   needsRefresh = true;
                }
            }
         }
         return needsRefresh;
      }

      boolean needsOpen() {
         if (editorModel.createMode ? createModeSelected : selected)
            return true;
         // auto-open trees when child nodes are selected
         if (childEnts != null) {
            for (TreeEnt childEnt:childEnts.values()) {
               if (childEnt.needsOpen())
                  return true;
            }
         }

         return false;
      }

      public void updateInstances(List<InstanceWrapper> insts) {
         clearMarkedFlag();
         if (insts != null) {
            for (InstanceWrapper inst:insts) {
                TreeEnt childEnt = null;
                if (childList != null) {
                   for (TreeEnt ent:childList) {
                       if (ent.instance != null && ent.instance.equals(inst)) {
                           childEnt = ent;
                           break;
                       }
                   }
                }
                if (childEnt == null) {
                   childEnt = new TreeEnt(EntType.Instance, inst.toString(), isTypeTree, inst.typeName, null);
                   childEnt.instance = inst;
                   if (childEnt.srcTypeName == null)
                      childEnt.srcTypeName = srcTypeName;
                   addChild(childEnt);
                }
                childEnt.marked = true;
            }
         }
         removeUnmarkedInstances();
      }

      public void clearMarkedFlag() {
         if (childList != null) {
            for (TreeEnt child:childList)
               child.marked = false;
         }
      }

      public void removeUnmarkedInstances() {
          if (childList != null) {
             for (int i = 0; i < childList.size(); i++) {
                TreeEnt child = childList.get(i);
                if (!child.marked && child.instance != null) {
                   childList.remove(i);
                   i--;
                   childEnts.remove(child.nodeId);
                   removeEntry(child);
                }
             }
          }
      }

      public String getNodeId() {
         switch (type) {
            case Instance:
               return instance.toString();
            default:
               return value;
         }
      }
   }

   String getTypeRootName() {
      String rootName;

      if (createMode) {
         if (propertyMode)
            rootName = "Select Property Type";
         else if (addLayerMode)
            rootName = "Select Layer to Include";
         else if (createLayerMode)
            rootName = "Select Extends Layers";
         else
            rootName = "Select Extends Type";
      }
      else
         rootName = "Application Types";

      return rootName;
   }

   String getLayerRootName() {
      String rootName; 
      if (createMode) {
         if (propertyMode)
            rootName = "Select Property Type by Layer";
         else if (addLayerMode)
            rootName = "Select Layer to Include by File";
         else if (createLayerMode)
            rootName = "Select Extends Layers by File";
         else
            rootName = "Select Extends Type by Layer";
      }
      else
         rootName = "Application Types by Layer";
      return rootName;
   }

   void selectionChanged() {
      editorModel.selectionChanged++;
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
         refreshTypeTree();
         refreshLayerTree();
      }
      catch (RuntimeException exc) {
         System.err.println("*** error refreshing tree model: " + exc.toString());
         exc.printStackTrace();
      }
      finally {
         refreshInProgress = false;
      }
   }

   void refreshTypeTree() {
      if (codeTypes == null || codeFunctions == null)
         return;

      if (rootTypeDirEnt == null) {
         if (!rebuildTypeDirEnts())
            return;
      }

      typeTreeBuilt = true;
   }

   void refreshLayerTree() {
      if (codeTypes == null || codeFunctions == null)
         return;

      if (rootLayerDirEnt == null) {
         if (!rebuildLayerDirEnts())
            return;
      }

      layerTreeBuilt = true;
   }

   // On the client we can't rebuild these - they get populated from the server on a sync.
   boolean rebuildTypeDirEnts() {
      return false;
   }

   boolean rebuildLayerDirEnts() {
      return false;
   }
}
