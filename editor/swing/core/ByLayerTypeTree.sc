import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import java.util.HashMap;

ByLayerTypeTree {
   void refreshTree() {
      if (treeModel.codeTypes == null)
         return;

      boolean needsOpenRoot = false;
      if (rootDirEnt == null || rootDirEnt.childEnts == null) {
         rebuildDirEnts();
         needsOpenRoot = true;
      }

      String rootName = getRootName();

      // Now build the TreeNodes from that, sorting as we go
      if (rootTreeNode == null) {
         rootTreeNode = new TreeNode(rootName);
      }
      else {
         rootTreeNode.setUserObject(rootName);
         if (rootTreeNode.getChildCount() == 0)
            needsOpenRoot = true;
      }
      if (rootTreeModel == null)
         rootTreeModel = new DefaultTreeModel(rootTreeNode);
      else
         rootTreeModel.nodeChanged(rootTreeNode);

      rootPathIndex = new HashMap<String, List<TreePath>>();

      TreeNode defaultNode = findChildNode(rootTreeNode, emptyCommentNode);
      if (defaultNode != null) {
         rootTreeNode.removeChildNode(emptyCommentNode);
         needsOpenRoot = true;
      }
      updatePackageContents(rootDirEnt, rootTreeNode, new TreePath(rootTreeNode));

      if (rootTreeNode.childCount == 0) {
         if (editorModel.codeTypes.size()== CodeType.allSet.size())
            emptyCommentNode.value = "<No layers>";
         else
            emptyCommentNode.value = "<No matching layers>";
         defaultNode = new TreeNode(emptyCommentNode);
         rootTreeModel.insertNodeInto(defaultNode, rootTreeNode,0);
         needsOpenRoot = true;
      }

      treeModel.uiBuilt = true;

      if (needsOpenRoot)
         treeModel.openRoot++;
   }
}