TypeTree {
   TreeNode {
      TreeNode(TreeEnt treeEnt) {
         if (treeEnt == null)
            System.out.println("*** Error - no treeEnt for node!");
          ent = treeEnt;
      }
      ArrayList<TreeNode> children = new ArrayList<TreeNode>();;

      int getNumChildren() {
         return children.size();
      }

      TreeNode getChildNode(int ix) {
         return children.get(ix);
      }

      void removeChildAt(int ix) {
         children.remove(ix);
      }
   }

   TreeEnt {
       void refreshNode() {
          if (treeNode != null) {
             if (updateInstances())
                refreshChildren();
          }
       }

      void toggleOpen() {
          refreshNode();
          super.toggleOpen();
      }

      void refreshChildren() {
          updatePackageContents(this, treeNode, !isTypeTree());
      }
   }

   void refreshTree() {
      if (treeModel.codeTypes == null)
         return;

      if (rootDirEnt == null) {
         if (!rebuildDirEnts()) {
            return;
         }
      }
      // The children have not been fetched yet
      if (rootDirEnt.childEnts == null)
         return;

      rootTreeIndex = new HashMap<String, List<TreeNode>>();

      String rootName = getRootName();;
      TreeNode rootNode;
      if (rootTreeNode == null) {
         // Now build the TreeNodes from that, sorting as we go
         rootNode = new TreeNode(rootDirEnt);
      }
      else {
         rootTreeNode.ent = rootDirEnt;
         rootNode = rootTreeNode;
      }

      TreeNode defaultNode = rootNode.findChildNodeForEnt(emptyCommentNode);
      if (defaultNode != null) {
         rootNode.removeChildNode(emptyCommentNode);
      }
      updatePackageContents(rootDirEnt, rootNode, false);
      if (rootNode.children.size() == 0) {
      /*
       * The layer tree nodes get populated on the server
         if (editorModel.codeTypes.size() == CodeType.allSet.size())
            typeEmptyCommentNode.value = "<No types>";
         else
            typeEmptyCommentNode.value = "<No matching types>";
      */
         defaultNode = new TreeNode(emptyCommentNode);
         rootNode.children.add(defaultNode);
      }
      treeModel.uiBuilt = true;

      // Update this after it's completely built so we can bind to this value and see a fully
      // populated tree.
      if (rootTreeNode != rootNode)
         rootTreeNode = rootNode;
   }

   void updatePackageContents(TreeEnt ents, TreeNode treeNode, boolean byLayer) {
       ents.treeNode = treeNode;
       ArrayList<TreeEnt> subList = ents.childList;
       int pos = 0;
       int rix;

       // If there are instances in this node, we need to build up the treeEnts for them.
       ents.updateInstances();

       boolean changed = false;
       treeNode.clearMarkedFlag();
       if (subList != null) {
          for (TreeEnt childEnt:subList) {
             rix = -1;
             int tix = ents.removed != null ? ents.removed.indexOf(childEnt) : -2;
             if ((ents.removed != null && (rix = ents.removed.indexOf(childEnt)) != -1) || !childEnt.isVisible(byLayer)) {
                treeNode.removeChildNode(childEnt);
                changed = true;
                if (rix != -1)
                   ents.removed.remove(rix);
                continue;
             }

             TreeNode childTree = treeNode.findChildNodeForEnt(childEnt);
             if (childTree == null) {
                childTree = new TreeNode(childEnt);

                treeNode.children.add(pos, childTree);
                changed = true;
             }
             childTree.marked = true;
             addToIndex(childEnt, childTree);
             updatePackageContents(childEnt, (TreeNode) childTree, byLayer);
             pos++;
          }
       }
       if (ents.removed != null) {
          // TODO: no longer need this loop?
          for (TreeEnt rem:ents.removed) {
             treeNode.removeChildNode(rem);
             changed = true;
          }
          ents.removed = null;
       }
       if (treeNode.removeUnmarkedChildren())
          changed = true;

       if (changed) {
          ents.sendChangedEvent();
          treeNode.sendChangedEvent();
       }
   }

   void updateInstanceTreeNodes(TreeEnt ents, TreeNode treeNode) {
       List<InstanceWrapper> insts = null;
       if (treeModel.includeInstances) {
          if (ents.cachedTypeDeclaration == null && ents.open) {
              ents.needsType = true;
          }
          if (ents.cachedTypeDeclaration != null) {
             insts = editorModel.ctx.getInstancesOfType(ents.cachedTypeDeclaration, 10, false, null, false);
          }
       }
       ents.updateInstances(insts);
       // Now called in refreshChildren
       //updatePackageContents(ents, treeNode, !isTypeTree());
   }

}