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

class TypeTree {
   TypeTreeModel treeModel;
   boolean byLayer = false;

   // Define the rootDirEnt in the model here so that rootTreeNode is never null for the the TreeView's tree property (now a
   // constructor property. This avoids null checks for the tree node. Need to set initDefault=true so that this value is synchronized to the client
   // when the TypeTree is initialized. Because it's initialized in both client and server layers, the default is to
   // not init it by default which means we are not listening for changes to the children.
   @sc.obj.Sync(initDefault=true)
   TreeEnt rootDirEnt = new TreeEnt(EntType.Root, "All Types", this, null, null);

   transient TypeTreeSelectionListener selectionListener;

   transient TreeEnt emptyCommentNode = new TreeEnt(EntType.Comment, "No visible types", this, null, null);

   // These are transient so they are not synchronized from client to the server.  That's
   // because we will build these data structures on the server or client
   transient TreeNode rootTreeNode = new TreeNode(rootDirEnt);

   transient Map<String, List<TreeNode>> rootTreeIndex = new HashMap<String, List<TreeNode>>();

   public TypeTree(TypeTreeModel model) {
      treeModel = model;
   }

   public TypeTree() {
   }

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

   @Sync(syncMode=SyncMode.Disabled)
   EditorModel getEditorModel() {
      return treeModel.editorModel;
   }

   // Disabling because this type is not sync'd to the client in this configuration.  Instead we sync the TreeEnt's
   // and then filter then on the client to produce the TreeNodes.  The TreeNodes correspond to UI objects and so
   // will turn into native UI elements as well that reflect this basic interface
   @Sync(syncMode=SyncMode.Disabled)
   class TreeNode {
      TreeEnt ent;
      // Flag used temporarily to do fast and orderly removal of elements that are no longer being used
      boolean marked;

      boolean getHasChildren() {
         return ent.hasChildren;
      }

      boolean getNeedsOpenClose() {
         return ent.needsOpenClose;
      }

      abstract int getNumChildren();

      abstract TreeNode getChildNode(int ix);

      int findChildIndexForEnt(TreeEnt ent) {
          int ct = getNumChildren();
          for (int i = 0; i < ct; i++) {
             if (getChildNode(i).ent == ent)
                return i;
          }
          return -1;
      }

      TreeNode findChildNodeForEnt(TreeEnt ent) {
          int ct = getNumChildren();
          for (int i = 0; i < ct; i++) {
             TreeNode node = getChildNode(i);
             if (node.ent == ent)
                return node;
          }
          return null;
      }

      void removeChildNode(TreeEnt childEnt) {
          int ix = findChildIndexForEnt(childEnt);
          if (ix != -1)
             removeChildAt(ix);
      }

      abstract void removeChildAt(int ix);

      void clearMarkedFlag() {
         int numChildren = getNumChildren();
         for (int i = 0; i < numChildren; i++) {
            TreeNode child = getChildNode(i);
            child.marked = false;
         }
      }

      boolean removeUnmarkedChildren() {
         int numChildren = getNumChildren();
         boolean any = false;
         for (int i = 0; i < numChildren; i++) {
            TreeNode child = getChildNode(i);
            if (!child.marked) {
               removeChildAt(i);
               i--;
               numChildren--;
               any = true;
            }
         }
         return any;
      }

      void sendChangedEvent() {
        sc.bind.Bind.sendEvent(sc.bind.IListener.VALUE_CHANGED, this, null);
      }
   }

   @sc.obj.Sync(onDemand=true)
   class TreeEnt implements Comparable<TreeEnt>, sc.obj.IObjectId {
      EntType type;
      String value;
      String srcTypeName;
      Layer layer;
      // For TreeEnts which represent instances, the wrapper for the instance
      InstanceWrapper instance;

      // Note: order-wise, we are putting childList in front of childEnts so that the initial on-demand sync'ing will choose the names in the order in which we want to display them.
      @sc.obj.Sync(onDemand=true)
      // Parallel to the childEnts map, but retains sort order for display
      ArrayList<TreeEnt> childList;
      @sc.obj.Sync(onDemand=true)
      HashMap<String,TreeEnt> childEnts;
      ArrayList<TreeEnt> removed = null;

      childList =: childrenAvailable();

      boolean imported;
      boolean hasSrc;
      boolean transparent; // Entry does not exist on the file system yet
      TypeTree typeTree; // Or the layer tree
      boolean prependPackage; // Is this a type in the type tree or a file like web.xml which does not use a type name

      @Sync(syncMode=SyncMode.Disabled)
      boolean marked; // Temp flag used to mark in-use objects

      String objectId;

      // Stores the swing entity that corresponds to this treeEnt (or null if not associated with one)
      @Sync(syncMode=SyncMode.Disabled)
      TreeNode treeNode;

      TreeEnt(EntType type, String value, TypeTree typeTree, String srcTypeName, Layer layer) {
         this.type = type;
         this.value = value;
         this.typeTree = typeTree;
         this.srcTypeName = srcTypeName;
         this.layer = layer;
         // These get created on the client so it's best if we just set the icon up front rather than wait for the server to determine it
         if (type == EntType.Instance)
            icon = GlobalResources.instanceIcon;
      }

      ArrayList<CodeType> entCodeTypes; // Which types and functions is this ent visible?

      // The value of getTypeDeclaration, once it's been fetched
      Object cachedTypeDeclaration;

      cachedTypeDeclaration =: typeAvailable();

      // Set to true when you need the type fetched
      boolean needsType = false;

      boolean selected = false, instanceSelected = false;
      /*
      public void setSelected(boolean sel) {
         if (srcTypeName != null && srcTypeName.equals("sc.util.ArrayList"))
            System.out.println("***");
         this.selected = sel;
      }
      public boolean getSelected() {
         return this.selected;
      }
      */

      boolean closed = false; // Have we explicitly closed this node.  if so, don't reopen it

      boolean createModeSelected = false;

      void setCachedTypeDeclaration(Object ctd) {
         cachedTypeDeclaration = ctd;
      }

      // We change this on the client for instances as a shortcut.  Don't need to sync that change to the server since
      // the server will set it to the thing anyway
      @Sync(syncMode=SyncMode.ServerToClient)
      UIIcon icon;

      boolean hasVisibleChildren;

      // When the childEnts property is populated or changes we need to
      // refresh.
      childEnts =: treeModel.refresh();

      String toString() {
         return value;
      }

      void toggleOpen() {
         if (!open)
            open = true;
         else {
            open = false;
            closed = true; // Track when we explicit close it and then don't re-open it again
         }
      }

      private boolean open = false;

      void setNeedsType(boolean needsType) {
         this.needsType = needsType;
      }

      void setOpen(boolean newOpen) {
         boolean orig = open;
         open = newOpen;
         if (!orig && newOpen) {
            DynUtil.invokeLater(
                new Runnable() {
                    void run() {
                        initChildren();
                        refreshNode();
                    }
                }, 0);
         }
      }

      abstract void refreshNode();

      boolean getOpen() {
         return open;
      }

      public void initChildren() {
         // childEnts and childList are marked @Sync(onDemand=true) so they are left out of the sync when the
         // parent object is synchronized.  When a user opens the node, the startSync call begins
         // synchronizing this property.  On the client this causes a fetch of the data.
         // On the server, it pushes this property to the client on the next sync.
         // When childEnts is set, the change of that property calls refresh().
         SyncManager.startSync(this, "childEnts");
         SyncManager.startSync(this, "childList");
      }

      void selectType(boolean append) {
         if (selectionListener != null)
            selectionListener.selectTreeEnt(this, append);
         selected = true;
      }

      void typeAvailable() {
         if (cachedTypeDeclaration == null)
            return;
         if (selectionListener != null)
            selectionListener.treeTypeAvailable(this);
      }

      void childrenAvailable() {
         // Once our children are available, might need to open our children and so on
         if (childEnts != null) {
            updateSelected();
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
          if (layer != null && !layer.matchesFilter(treeModel.codeTypes))
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
                   return true;

                case InactiveLayer:
                   // Can't add a layer twice - only show inactive layers which are really not active
                   return treeModel.layerMode && treeModel.system.getLayerByDirName(value) == null;

                case LayerDir:   // A layer directory itself
                   if (treeModel.addLayerMode) // the layer filters were applied above.  When adding layers though, don't show layers that are already there
                      return false;
                   if (byLayer && hasAVisibleChild(byLayer))
                      return true;
                   return getTypeIsVisible();

                case LayerFile:
                   if (treeModel.createMode) {
                      // In the type tree, we display the layer file.  In the layer tree, We display the layerDir as the layer in layer mode
                      if (treeModel.layerMode)
                         return !byLayer && !treeModel.addLayerMode;
                      else
                         return false;
                   }
                   return getTypeIsVisible();

                case ParentObject:
                   if (treeModel.layerMode)
                      return false;
                   if (hasAVisibleChild(byLayer))
                      return true;
                case Object:
                   if (treeModel.createMode || treeModel.layerMode)
                       return false;
                   // FALL THROUGH to do type processing in app type mode.
                case ParentType:
                case ParentEnum:
                case ParentEnumConstant:
                case ParentInterface:
                   if (treeModel.layerMode)
                      return false;

                   if (hasAVisibleChild(byLayer))
                      return true;
                case Interface:
                case Enum:
                case EnumConstant:
                case Type:
                   if (treeModel.layerMode)
                      return false;

                   if (!getTypeIsVisible())
                      return false;

                   // Create mode:
                   //    show imported types or those which are in the current layer, or those imported which are defined in this layer
                   // Application mode:
                   //    just make sure we have the src for the type.  We'll have already doen the application check above.
                   //    if it's a transparent item, even if hasSrc is false we display it.
                   return (treeModel.createMode && (imported || editorModel.currentLayer == null || layer == editorModel.currentLayer)) || hasSrc || transparent;

                case Primitive:
                   return treeModel.createMode && treeModel.propertyMode;
                case Instance:
                   return treeModel.includeInstances;
             }
          }
          return true;
      }

      Object getTypeDeclaration() {
         if (cachedTypeDeclaration != null)
            return cachedTypeDeclaration;
         if (typeName == null)
            return null;
         if (editorModel.ctx.system == null)
            return null;
         //return DynUtil.resolveName(typeName, false);
         return editorModel.ctx.system.getSrcTypeDeclaration(typeName, null);
      }

      boolean getTypeIsVisible() {
         if (treeModel.createMode && treeModel.currentCreateMode == CreateMode.Instance) {
            if (srcTypeName != null && editorModel.ctx.isCreateInstType(srcTypeName))
               return true;
            return false;
         }

         if (entCodeTypes != null && treeModel.codeTypes != null) {
            boolean vis = false;
            for (int i = 0; i < entCodeTypes.size(); i++) {
               if (treeModel.codeTypes.contains(entCodeTypes.get(i))) {
                  vis = true;
                  break;
               }
            }
            if (!vis)
               return false;
         }
         return true;
      }

      boolean needsInstances() {
         switch(type) {
            case ParentType:
            case Type:
            case ParentObject:
            case Object:
            case ParentEnum:
            case Enum:
            case ParentEnumConstant:
            case EnumConstant:
            case ParentInterface:
            case Interface:
            case Instance:
               return true;
         }
         return false;
      }

      @Constant
      String getObjectId() {
         if (objectId != null)
            return objectId;
         if (type == null || value == null)
            return null;
         String valuePart = CTypeUtil.escapeIdentifierString(value == null ? "" : "_" + value.toString());
         String typeNamePart = srcTypeName == null ? "" : "_" + CTypeUtil.escapeIdentifierString(srcTypeName);
         String entTypePart = type == null ? "_unknown" : "_" + getTypePart(type);
         String layerPart = layer == null ? "" : "_" + CTypeUtil.escapeIdentifierString(layer.layerName);
         objectId = "TE" + getIdPrefix() + entTypePart + layerPart + typeNamePart + valuePart;
         return objectId;
      }

      // Because we update the type from "Type" to "ParentObject", etc. need to keep the id from changing during that change
      private String getTypePart(EntType type) {
         switch(type) {
            case ParentType:
            case Type:
            case ParentObject:
            case Object:
            case ParentEnumConstant:
            case EnumConstant:
            case ParentEnum:
            case Enum:
            case ParentInterface:
            case Interface:
               return "Type";
            case Instance:
               return "Instance";
         }
         return type.toString();
      }

      void initChildLists() {
         if (childEnts == null)
            childEnts = new HashMap<String,TreeEnt>();
         if (childList == null)
            childList = new ArrayList<TreeEnt>();
      }

      void addChild(TreeEnt ent) {
         if (ent == null) {
            System.out.println("*** Error - null child");
            return;
         }
         initChildLists();

         childList.add(ent);
         TreeEnt old = childEnts.put(ent.nodeId, ent);
         if (old != null)
            childList.remove(old);
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
         // Need to assume we have children until they are fetched
         return getNumChildren() > 0 || childList == null;
         //return true;
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
             return true;
         for (TreeEnt childEnt:childEnts.values()) {
             if (childEnt.isVisible(byLayer) || childEnt.hasAVisibleChild(byLayer))
                return hasVisibleChildren = true;
         }
         return hasVisibleChildren = false;
      }

      boolean updateSelected() {
         boolean hasSelectedType = editorModel.typeNames.length > 0;

         if (type == EntType.Instance || type == EntType.Object || type == EntType.ParentObject) {
            // To make sure we update the instance here the first time this type is selected elsewhere
            if (instance == null && hasSelectedType && srcTypeName.equals(editorModel.typeNames[0])) {
               if (cachedTypeDeclaration == null) {
                  cachedTypeDeclaration = getTypeDeclaration();
               }
               if (cachedTypeDeclaration != null)
                  updateInstances();
               else
                  needsType = true;
            }

            boolean newSel = instance != null && editorModel.selectedInstances != null && editorModel.selectedInstances.contains(instance);
            if (newSel != selected)
               selected = newSel;
            return false;
         }


         // Automatically open package nodes that contain the selected type
         if (type == EntType.Package && childList == null && hasSelectedType) {
            String packageName = srcTypeName;
            String typeName = editorModel.typeNames[0];
            if (typeName.startsWith(packageName) && typeName.length() > packageName.length() && typeName.charAt(packageName.length()) == '.') {
               setOpen(true);
            }
         }

         if (hasSelectedType && (type == EntType.LayerGroup || type == EntType.LayerDir)) {
            String entName = srcTypeName;
            if (type == EntType.LayerDir && layer != null)
               entName = layer.getLayerName();
            else if (type == EntType.LayerGroup && entName.startsWith("layerGroup:"))
               entName = entName.substring(entName.indexOf(':')+1);
            Layer selLayer = editorModel.currentLayer;
            if (selLayer != null) {
               String selLayerName = selLayer.getLayerName();
               int srcLen = entName.length();
               int layerNameLen = selLayerName.length();
               if (selLayerName.startsWith(entName) && (layerNameLen == srcLen || layerNameLen > srcLen && selLayerName.charAt(srcLen) == '.'))
                  setOpen(true);
            }
         }

         boolean needsRefresh = false;
         if (typeName == null) {
            if (selected) {
               // Once there's a current type, all directories are deselected
               if (editorModel.typeNames.length > 0)
                  selected = false;
               // Leave the folder selected as long as it marks the current package
               else if (!DynUtil.equalObjects(srcTypeName, editorModel.currentPackage))
                  selected = false;

               if (!selected)
                  needsRefresh = true;
            }
         }
         else {
            boolean newSel = editorModel.isTypeNameSelected(typeName);
            if (newSel != selected) {
               selected = newSel;
               needsRefresh = true;
            }
            boolean newCreate = editorModel.isCreateModeTypeNameSelected(typeName);
            if (newCreate != selected) {
               createModeSelected = newCreate;
               needsRefresh = true;
            }
         }
         if (needsOpen() && !open && !closed) {
            open = true;
            needsRefresh = true;
         }
         if (childList != null) {
            for (int i = 0; i < childList.size(); i++) {
               TreeEnt childEnt = childList.get(i);
               if (childEnt == null)
                  System.out.println("*** Error missing child in childList!");
               else if (childEnt == this)
                  System.err.println("*** Error - invalid recursive tree!");
               else if (childEnt.updateSelected())
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
      
      TreeEnt getSoloVisibleChild() {
         if (childList == null)
            return null;
         TreeEnt lastVis = null;
         for (TreeEnt childEnt:childList) {
            if (childEnt.isVisible(byLayer)) {
               if (lastVis != null) // More than one visible child so don't open below this node
                  return null;
               lastVis = childEnt;
            }
         }
         return lastVis;
      }

      public boolean updateInstances() {
         if (!needsInstances() || type == EntType.Instance)
            return false;
         List<InstanceWrapper> insts = null;
         if (cachedTypeDeclaration == null && (open || selected || instanceSelected)) {
            needsType = true;
         }
         if (cachedTypeDeclaration != null) {
            insts = editorModel.ctx.getInstancesOfType(cachedTypeDeclaration, 10, false, null, false);
         }
         return updateInstances(insts);
      }

      boolean updateInstances(List<InstanceWrapper> insts) {
         clearMarkedFlag();
         boolean anyChanges = false;
         if (insts != null) {
            if (insts.size() == 1) { // TODO: should we make sure this is really a singleton definition?  rootedObject is too strict
               InstanceWrapper mainInst = insts.get(0);
               anyChanges = !DynUtil.equalObjects(instance, mainInst);
               if (anyChanges) {
                  instance = mainInst;
               }
            }
            else {
               instance = null;
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
                      String nodeDisplayName = getNodeIdFromInstance(inst.theInstance);
                      childEnt = new TreeEnt(EntType.Instance, nodeDisplayName, typeTree, inst.typeName, null);
                      childEnt.instance = inst;
                      childEnt.prependPackage = true;
                      if (childEnt.srcTypeName == null)
                         childEnt.srcTypeName = inst.typeName;
                      if (srcTypeName.equals(inst.typeName))
                         childEnt.cachedTypeDeclaration = cachedTypeDeclaration;
                      else
                         childEnt.cachedTypeDeclaration = childEnt.getTypeDeclaration();

                      if (childEnt.cachedTypeDeclaration == null)
                         childEnt.needsType = true;
                      addChild(childEnt);
                   }
                   anyChanges = true;
                   childEnt.marked = true;
               }
            }
         }
         if (removeUnmarkedInstances())
            anyChanges = true;

         return anyChanges;
      }

      abstract void refreshChildren();

      void addInstance(Object inst) {
      }

      public void clearMarkedFlag() {
         if (childList != null) {
            for (TreeEnt child:childList)
               child.marked = false;
         }
      }

      public boolean removeUnmarkedInstances() {
          boolean any = false;
          if (childList != null) {
             for (int i = 0; i < childList.size(); i++) {
                TreeEnt child = childList.get(i);
                if (!child.marked && child.type == EntType.Instance && child.instance != null) {
                   childList.remove(i);
                   i--;
                   childEnts.remove(child.nodeId);
                   removeEntry(child);
                   any = true;
                }
             }
          }
          return any;
      }

      public String getNodeId() {
         switch (type) {
            case Instance:
               return getNodeIdFromInstance(instance.theInstance);
            default:
               return value;
         }
      }

      void sendChangedEvent() {
        sc.bind.Bind.sendEvent(sc.bind.IListener.VALUE_CHANGED, this, null);
      }

      String getIndexKey() {
         if (type == EntType.Instance && instance == null) {
            System.err.println("*** Weird case in type tree index key");
            return typeName;
         }
         return type == EntType.Instance ? typeName + ":" + instance.toString() : typeName;
      }
   }

   public String getIdPrefix() {
      return "T";
   }

   String getRootName() {
      String rootName;

      if (treeModel.createMode) {
         if (treeModel.propertyMode)
            rootName = "Select property type";
         else if (treeModel.addLayerMode)
            rootName = "Select layer to include";
         else if (treeModel.createLayerMode)
            rootName = "Select extends layers";
         else if (treeModel.currentCreateMode == CreateMode.Instance)
            rootName = "Select type for new instance";
         else
            rootName = "Select extends type";
      }
      else
         rootName = "Application Types";

      return rootName;
   }

   public boolean isTypeTree() {
      return !(this instanceof ByLayerTypeTree);
   }

   public boolean rebuildDirEnts() {
      return false;
   }

   public abstract void refreshTree();

   // Keep an index of the visible nodes in the tree so we can do reverse selection - i.e. go from type name
   // to list of visible tree nodes that refer to it.
   void addToIndex(TreeEnt childEnt, TreeNode treeNode) {
      if (childEnt.isSelectable()) {
         List<TreeNode> l = rootTreeIndex.get(childEnt.getIndexKey());
         if (l == null) {
            l = new ArrayList<TreeNode>();

            if (childEnt.type != EntType.LayerDir)
               rootTreeIndex.put(childEnt.getIndexKey(), l);

            if (childEnt.type == EntType.Package || childEnt.type == EntType.LayerDir)
               rootTreeIndex.put(TypeTreeModel.PKG_INDEX_PREFIX + childEnt.value, l);
         }
         if (!l.contains(treeNode))
            l.add(treeNode);
      }
   }
   
   public TreeEnt getOpenToRootEnt() {
      if (rootDirEnt == null)
         return null;
      TreeEnt treeEnt = rootDirEnt;
      do {
         TreeEnt next = treeEnt.getSoloVisibleChild();
         if (next == null)
            return treeEnt;
         treeEnt = next;
      } while (true);
   }

   static String getNodeIdFromInstance(Object inst) {
      return inst == null ? "<null>" : CTypeUtil.getClassName(DynUtil.getInstanceName(inst));
   }

}
