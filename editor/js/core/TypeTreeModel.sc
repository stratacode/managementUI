import sc.layer.CodeType;
import sc.layer.CodeFunction;

import sc.obj.Sync;
import sc.obj.SyncMode;

// The Javascript implementation of the type/layer tree in the UI.  This code should really be in coreui so it is shared. 
// The swingui code when written uses TreePath as part of its core and needs to be refactored to remove this
// swing dependency.  Fortunately we can also cleanly separate the classes until it's worth the time to refactor and bring them
// back in sync.
TypeTreeModel {
   createMode =: selectionChanged();
   addLayerMode =: selectionChanged();
   createLayerMode =: selectionChanged();
   propertyMode =: refresh();

   // The swing version does a rebuild on the first refresh call but the web version doesn't
   // need to do that.
   rebuildFirstTime = false;

   // Disabling because this type is not sync'd to the client in this configuration
   @Sync(syncMode=SyncMode.Disabled)
   class TreeNode {
      TreeEnt ent;
      TreeNode(TreeEnt e) {
         ent = e;
      }

      boolean getHasChildren() {
         return false;
      }

      boolean getNeedsOpenClose() {
         return ent.needsOpenClose;
      }
   }

   class DirTreeNode extends TreeNode {
      ArrayList<TreeNode> children = new ArrayList<TreeNode>();;
      DirTreeNode(DirEnt e) {
         super(e);
      }

      boolean getHasChildren() {
         return true;
      }

      boolean getNeedsOpenClose() {
         return ent.needsOpenClose;
      }
   }

   // These are transient so they are not synchronized from client to the server.  That's
   // because we will build these data structures on the server or client 
   transient DirTreeNode rootTypeTreeNode;
   transient DirTreeNode rootLayerTreeNode;

   // These will be populated from the server automatically.  Sometime after those values
   // are set, we need to refresh the tree.
   rootTypeDirEnt =: refresh();
   rootLayerDirEnt =: refresh();

   transient Map<String, List<TreeNode>> rootLayerTreeIndex;
   transient Map<String, List<TreeNode>> rootTypeTreeIndex;

   void refreshTypeTree() {
      if (codeTypes == null || codeFunctions == null)
         return;

      if (rootTypeDirEnt == null) {
         if (!rebuildTypeDirEnts()) {
            return;
         }
      }

      rootTypeTreeIndex = new HashMap<String, List<TreeNode>>();

      String rootName = getTypeRootName();;
      DirTreeNode rootNode;
      if (rootTypeTreeNode == null) {
         // Now build the TreeNodes from that, sorting as we go
         rootNode = new DirTreeNode(rootTypeDirEnt);
      }
      else {
         rootTypeTreeNode.ent = rootTypeDirEnt;
         rootNode = rootTypeTreeNode;
      }

      TreeNode defaultNode = findChildNode(rootNode, typeEmptyCommentNode);
      if (defaultNode != null) {
         removeChildNode(rootNode, typeEmptyCommentNode);
      }
      updatePackageContents(rootTypeDirEnt, rootNode, rootTypeTreeIndex, false);
      if (rootNode.children.size() == 0) {
      /*
       * The layer tree nodes get populated on the server
         if (editorModel.codeFunctions.size() == CodeFunction.allSet.size() && editorModel.codeTypes.size() == CodeType.allSet.size())
            typeEmptyCommentNode.value = "<No types>";
         else
            typeEmptyCommentNode.value = "<No matching types>";
      */
         defaultNode = new TreeNode(typeEmptyCommentNode);
         rootNode.children.add(defaultNode);
      }
      typeTreeBuilt = true;

      // Update this after it's completely built so we can bind to this value and see a fully
      // populated tree.
      if (rootTypeTreeNode != rootNode)
         rootTypeTreeNode = rootNode;
   }

   public final static String PKG_INDEX_PREFIX = "<pkg>:";

   void updatePackageContents(DirEnt ents, DirTreeNode treeNode, Map<String, List<TreeNode>> index, boolean byLayer) {
       Map<String,DirEnt> subDirs = ents.subDirs;
       int pos = 0;
       int rix;
       if (subDirs != null) {
          for (DirEnt childEnt:subDirs.values()) {
             rix = -1;
             int tix = ents.removed != null ? ents.removed.indexOf(childEnt) : -2;
             if ((ents.removed != null && (rix = ents.removed.indexOf(childEnt)) != -1) || !childEnt.isVisible(byLayer)) {
                removeChildNode(treeNode, childEnt);
                if (rix != -1)
                   ents.removed.remove(rix);
                continue;
             }

             TreeNode childTree = findChildNode(treeNode, childEnt);
             if (childTree == null) {
                childTree = new DirTreeNode(childEnt);

                treeNode.children.add(pos, childTree);
             }
             addToIndex(childEnt, childTree, index);
             updatePackageContents(childEnt, (DirTreeNode) childTree, index, byLayer);
             pos++;
          }
       }
       if (ents.entries != null) {
          for (TreeEnt element:ents.entries) {
             rix = -1;
             int tix = ents.removed != null ? ents.removed.indexOf(element) : -2;
             // Make sure that we don't call isVisible or anything on a removed element - the src might not be there, the class might be there and it could get loaded unnecessarily
             if ((ents.removed != null && (rix = ents.removed.indexOf(element)) != -1) || !element.isVisible(byLayer)) {
                removeChildNode(treeNode, element);
                if (rix != -1)
                   ents.removed.remove(rix);
                continue;
             }

             int ix = findChildNodeIndex(treeNode, element);

             // If there's already a directory element for this node, don't add a second one for the leaves
             if (subDirs == null || subDirs.get(element.value) == null) {
                TreeNode childNode;
                if (ix == -1) {
                   childNode = new TreeNode(element);
                   treeNode.children.add(pos, childNode);
                }
                else {
                   if (ix == pos)
                      childNode = (TreeNode) treeNode.children.get(ix);
                   else {
                      // Need to reorder this element
                      childNode = treeNode.children.remove(ix);
                      treeNode.children.add(pos, childNode);
                   }
                }
                pos++;
                addToIndex(element, childNode, index);
             }
             else if (ix != -1)
                removeChildNode(treeNode, element);
          }
       }
       if (ents.removed != null) {
          // TODO: no longer need this loop?
          for (TreeEnt rem:ents.removed) {
             removeChildNode(treeNode, rem);
          }
          ents.removed = null;
       }
   }

   // Keep an index of the visible nodes in the tree so we can do reverse selection - i.e. go from type name
   // to list of visible tree nodes that refer to it.
   void addToIndex(TreeEnt childEnt, TreeNode treeNode, Map<String,List<TreeNode>> index) {
      if (childEnt.isSelectable()) {
         List<TreeNode> l = index.get(childEnt.typeName);
         if (l == null) {
            l = new ArrayList<TreeNode>();

            if (childEnt.type != EntType.LayerDir)
               index.put(childEnt.typeName, l);

            if (childEnt.type == EntType.Package || childEnt.type == EntType.LayerDir)
               index.put(PKG_INDEX_PREFIX + childEnt.value, l);
         }
         l.add(treeNode);
      }
   }

   TreeNode findChildNode(DirTreeNode parent, TreeEnt treeEnt) {
      int ix = findChildNodeIndex(parent, treeEnt);
      if (ix == -1)
         return null;
      return (TreeNode) parent.children.get(ix);
   }

   void removeChildNode(DirTreeNode parent, TreeEnt treeEnt) {
      int ix = findChildNodeIndex(parent, treeEnt);
      if (ix == -1) {
         return;
      }
      parent.children.remove(ix);
   }

   int findChildNodeIndex(DirTreeNode parent, TreeEnt treeEnt) {
      int sz = parent.children.size();
      for (int i = 0; i < sz; i++) {
         if (((TreeNode) parent.children.get(i)).ent == treeEnt)
            return i;
      }
      return -1;
   }

   void refreshLayerTree() {
      if (codeTypes == null || codeFunctions == null)
         return;

      if (rootLayerDirEnt == null) {
         if (!rebuildLayerDirEnts())
            return;
      }

      String rootName = getLayerRootName();

      if (rootLayerTreeNode == null) {
         // Now build the TreeNodes from that, sorting as we go
         rootLayerTreeNode = new DirTreeNode(rootLayerDirEnt);
      }
      else
         rootLayerTreeNode.ent = rootLayerDirEnt;

      rootLayerTreeIndex = new HashMap<String, List<TreeNode>>();

      TreeNode defaultNode = findChildNode(rootLayerTreeNode, layerEmptyCommentNode);
      if (defaultNode != null) {
         removeChildNode(rootLayerTreeNode, layerEmptyCommentNode);
      }
      updatePackageContents(rootLayerDirEnt, rootLayerTreeNode, rootLayerTreeIndex, true);

      if (rootLayerTreeNode.children.size() == 0) {
      /*
         if (editorModel.codeFunctions.size()== CodeFunction.allSet.size() && editorModel.codeTypes.size()== CodeType.allSet.size())
            layerEmptyCommentNode.value = "<No layers>";
         else
            layerEmptyCommentNode.value = "<No matching layers>";
       */
         defaultNode = new TreeNode(layerEmptyCommentNode);
         rootLayerTreeNode.children.add(defaultNode);
      }

      layerTreeBuilt = true;
   }

   boolean nodeExists(String typeName) {
      return rootTypeTreeIndex.get(typeName) != null;
   }
}
