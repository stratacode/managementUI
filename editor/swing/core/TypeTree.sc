import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

TypeTree {
   Map<String, List<TreePath>> rootPathIndex;

   DefaultTreeModel rootTreeModel;

   TreeNode extends DefaultMutableTreeNode {
      TreeNode(Object userObj) {
          super(userObj);
          if (userObj instanceof TreeEnt)
             ent = (TreeEnt) userObj;
      }

      public int getNumChildren() {
         return getChildCount();
      }

      public TreeNode getChildNode(int ix) {
         return (TreeNode) getChildAt(ix);
      }


      void removeChildAt(int ix) {
         // For some reason removing a selected node, even when there are multiple selections causes a change event
         // showing null paths.  When swapping visibility of nodes, this is a PITA.
         treeModel.ignoreSelectionEvents = true;
         try {
            if (ent != null) {
               TypeTree.TreeNode child = (TypeTree.TreeNode) getChildAt(ix);
               ent.typeTree.rootTreeModel.removeNodeFromParent(child);
            }
         }
         finally {
            treeModel.ignoreSelectionEvents = false;
         }
      }
   }


   TreeEnt {
       // The path for this node in the tree
       TreePath path;

       selected =: refreshNode();

       void refreshNode() {
          if (treeNode != null) {
             if (updateInstances())
                refreshChildren();
          }
       }

       void refreshChildren() {
           updatePackageContents(this, treeNode, rootPathIndex, path);
       }
   }

   TreePath getTreePath(String typeName) {
      List<TreePath> l = rootPathIndex.get(typeName);
      if (l == null || l.size() == 0)
         return null;
      // TODO: look for current selected layer and choose that one?
      return l.get(0);
   }

   void refreshTree() {
      if (treeModel.codeTypes == null)
         return;

      boolean needsOpenRoot = false;
      if (rootDirEnt == null) {
         needsOpenRoot = true;
         rebuildDirEnts();
      }

      rootPathIndex = new HashMap<String, List<TreePath>>();

      String rootName = getRootName();;
      if (rootTreeNode == null) {
         // Now build the TreeNodes from that, sorting as we go
         rootTreeNode = new TreeNode(rootName);
         rootTreeModel = new DefaultTreeModel(rootTreeNode);
      }
      else {
         rootTreeNode.setUserObject(rootName);
         rootTreeModel.nodeChanged(rootTreeNode);
      }

      TreeNode defaultNode = rootTreeNode.findChildNodeForEnt(emptyCommentNode);
      if (defaultNode != null) {
         rootTreeNode.removeChildNode(emptyCommentNode);
         needsOpenRoot = true;
      }
      updatePackageContents(rootDirEnt, rootTreeNode, rootPathIndex, new TreePath(rootTreeNode));
      if (rootTreeNode.getChildCount() == 0) {
         if (editorModel.codeTypes.size() == CodeType.allSet.size())
            emptyCommentNode.value = "<No types>";
         else
            emptyCommentNode.value = "<No matching types>";
         defaultNode = new TreeNode(emptyCommentNode);
         rootTreeModel.insertNodeInto(defaultNode, rootTreeNode, 0);
         needsOpenRoot = true;
      }

      treeModel.uiBuilt = true;

      if (needsOpenRoot) {
         treeModel.openRoot++;
      }
   }

   // Keep an index of the visible nodes in the tree so we can do reverse selection - i.e. go from type name
   // to TreePath for the selection.
   TreePath addToIndex(TreeEnt childEnt, TreeNode treeNode, Map<String,List<TreePath>> index, TreePath parent) {
      Object[] parentPaths = parent.getPath();
      int pl = parentPaths.length;
      Object[] paths = new Object[pl + 1];
      System.arraycopy(parentPaths, 0, paths, 0, pl);
      paths[pl] = treeNode;
      TreePath tp = new TreePath(paths);

      String childKey = childEnt.type == EntType.Instance ? childEnt.typeName + ":" + childEnt.instance.toString() : childEnt.typeName;

      if (childEnt.isSelectable()) {
         List<TreePath> l = index.get(childKey);
         if (l == null) {
            l = new ArrayList<TreePath>();

            // For Instances, use a key which is unique to that instance so when we select the instance
            // in one view, it will show up in the others
            if (childEnt.type != EntType.LayerDir)
               index.put(childKey, l);

            if (childEnt.type == EntType.Package || childEnt.type == EntType.LayerDir)
               index.put(TypeTreeModel.PKG_INDEX_PREFIX + childEnt.value, l);
         }
         l.add(tp);
      }

      return tp;
   }

   void updatePackageContents(TreeEnt ents, TreeNode treeNode,
                              Map<String, List<TreePath>> index, TreePath parent) {

      ents.treeNode = treeNode;
      ents.path = parent;
      ArrayList<TreeEnt> subDirs = ents.childList;
      int pos = 0;
      int rix;

      ents.updateInstances();

      if (subDirs != null) {
         for (TreeEnt childEnt:subDirs) {
            rix = -1;
            int tix = ents.removed != null ? ents.removed.indexOf(childEnt) : -2;
            if ((ents.removed != null && (rix = ents.removed.indexOf(childEnt)) != -1) || !childEnt.isVisible(byLayer)) {
               treeNode.removeChildNode(childEnt);
               childEnt.treeNode = null;
               if (rix != -1)
                  ents.removed.remove(rix);
               continue;
            }

            TreeNode childTree = treeNode.findChildNodeForEnt(childEnt);
            if (childTree == null) {
               childTree = new TreeNode(childEnt);

               ents.typeTree.rootTreeModel.insertNodeInto(childTree, treeNode, pos);
            }
            TreePath path = addToIndex(childEnt, childTree, index, parent);
            updatePackageContents(childEnt, childTree, index, path);
            pos++;
         }
      }
      if (ents.removed != null) {
         // TODO: no longer need this loop?
         for (TreeEnt rem:ents.removed) {
            treeNode.removeChildNode(rem);
            rem.treeNode = null;
         }
         ents.removed = null;
      }
   }

   TreeNode findChildNode(TreeNode parent, Object userObj) {
      int ix = findChildNodeIndex(parent, userObj);
      if (ix == -1)
         return null;
      return (TreeNode) parent.getChildAt(ix);
   }


   int findChildNodeIndex(TreeNode parent, Object userObj) {
      for (int i = 0; i < parent.getChildCount(); i++) {
         Object childUserObj = ((TreeNode) parent.getChildAt(i)).getUserObject();
         if (DynUtil.equalObjects(childUserObj, userObj))
            return i;
      }
      return -1;
   }
}
