import java.util.HashMap;

ByLayerTypeTree {
   void refreshTree() {
      if (treeModel.codeTypes == null || treeModel.codeFunctions == null)
         return;

      if (rootDirEnt == null) {
         if (!rebuildDirEnts())
            return;
      }

      String rootName = getRootName();

      if (rootTreeNode == null) {
         // Now build the TreeNodes from that, sorting as we go
         rootTreeNode = new TreeNode(rootDirEnt);
      }
      else
         rootTreeNode.ent = rootDirEnt;

      rootTreeIndex = new HashMap<String, List<TreeNode>>();

      TreeNode defaultNode = rootTreeNode.findChildNodeForEnt(emptyCommentNode);
      if (defaultNode != null) {
         rootTreeNode.removeChildNode(emptyCommentNode);
      }
      updatePackageContents(rootDirEnt, rootTreeNode, rootTreeIndex, true);

      if (rootTreeNode.children.size() == 0) {
      /*
         if (editorModel.codeFunctions.size()== CodeFunction.allSet.size() && editorModel.codeTypes.size()== CodeType.allSet.size())
            layerEmptyCommentNode.value = "<No layers>";
         else
            layerEmptyCommentNode.value = "<No matching layers>";
       */
         defaultNode = new TreeNode(emptyCommentNode);
         rootTreeNode.children.add(defaultNode);
      }

      treeModel.uiBuilt = true;
   }
}