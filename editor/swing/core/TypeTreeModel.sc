import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;


/** Stores the data behind the type tree, used for both the by types and by layers views. */
TypeTreeModel {
   createMode =: selectionChanged();
   addLayerMode =: selectionChanged();
   createLayerMode =: selectionChanged();
   propertyMode =: refresh();

   DefaultTreeModel rootTypeTreeModel, rootLayerTreeModel;

   DefaultMutableTreeNode rootTypeTreeNode;

   DefaultMutableTreeNode rootLayerTreeNode;

   Map<String, List<TreePath>> rootLayerTreeIndex;
   Map<String, List<TreePath>> rootTypeTreeIndex;

   // Event property incremented each time we need to increment the "root" tree object
   int openRoot;

   boolean ignoreSelectionEvents = false;

   public void scheduleBuild() {
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            rebuild();
         }});
   }

   void refreshTypeTree() {
      if (codeTypes == null || codeFunctions == null)
         return;

      boolean needsOpenRoot = false;
      if (rootTypeDirEnt == null) {
         needsOpenRoot = true;
         rebuildTypeDirEnts();
      }

      rootTypeTreeIndex = new HashMap<String, List<TreePath>>();

      String rootName = getTypeRootName();;
      if (rootTypeTreeNode == null) {
         // Now build the TreeNodes from that, sorting as we go
         rootTypeTreeNode = new DefaultMutableTreeNode(rootName);
         rootTypeTreeModel = new DefaultTreeModel(rootTypeTreeNode);
      }
      else {
         rootTypeTreeNode.setUserObject(rootName);
         rootTypeTreeModel.nodeChanged(rootTypeTreeNode);
      }

      DefaultMutableTreeNode defaultNode = findChildNode(rootTypeTreeNode, typeEmptyCommentNode);
      if (defaultNode != null) {
         removeChildNode(rootTypeTreeNode, typeEmptyCommentNode, rootTypeTreeModel);
         needsOpenRoot = true;
      }
      updatePackageContents(rootTypeDirEnt, rootTypeTreeNode, rootTypeTreeModel, rootTypeTreeIndex, new TreePath(rootTypeTreeNode), false);
      if (rootTypeTreeNode.getChildCount() == 0) {
         if (editorModel.codeFunctions.size() == CodeFunction.allSet.size() && editorModel.codeTypes.size() == CodeType.allSet.size())
            typeEmptyCommentNode.value = "<No types>";
         else
            typeEmptyCommentNode.value = "<No matching types>";
         defaultNode = new DefaultMutableTreeNode(typeEmptyCommentNode);
         rootTypeTreeModel.insertNodeInto(defaultNode, rootTypeTreeNode, 0);
         needsOpenRoot = true;
      }

      typeTreeBuilt = true;

      if (needsOpenRoot) {
         openRoot++;
      }

   }

   public final static String PKG_INDEX_PREFIX = "<pkg>:";

   // Keep an index of the visible nodes in the tree so we can do reverse selection - i.e. go from type name
   // to TreePath for the selection.
   TreePath addToIndex(TreeEnt childEnt, TreeNode treeNode, Map<String,List<TreePath>> index, TreePath parent) {
      Object[] parentPaths = parent.getPath();
      int pl = parentPaths.length;
      Object[] paths = new Object[pl + 1];
      System.arraycopy(parentPaths, 0, paths, 0, pl);
      paths[pl] = treeNode;
      TreePath tp = new TreePath(paths);

      if (childEnt.isSelectable()) {
         List<TreePath> l = index.get(childEnt.typeName);
         if (l == null) {
            l = new ArrayList<TreePath>();

            if (childEnt.type != EntType.LayerDir)
               index.put(childEnt.typeName, l);

            if (childEnt.type == EntType.Package || childEnt.type == EntType.LayerDir)
               index.put(PKG_INDEX_PREFIX + childEnt.value, l);
         }
         l.add(tp);
      }

      return tp;
   }

   void updatePackageContents(DirEnt ents, DefaultMutableTreeNode treeNode, DefaultTreeModel treeModel,
                              Map<String, List<TreePath>> index, TreePath parent, boolean byLayer) {
       Map<String,DirEnt> subDirs = ents.subDirs;
       int pos = 0;
       int rix;
       for (DirEnt childEnt:subDirs.values()) {
          rix = -1;
          int tix = ents.removed != null ? ents.removed.indexOf(childEnt) : -2;
          if ((ents.removed != null && (rix = ents.removed.indexOf(childEnt)) != -1) || !childEnt.isVisible(byLayer)) {
             removeChildNode(treeNode, childEnt, treeModel);
             if (rix != -1)
                ents.removed.remove(rix);
             continue;
          }

          DefaultMutableTreeNode childTree = findChildNode(treeNode, childEnt);
          if (childTree == null) {
             childTree = new DefaultMutableTreeNode(childEnt);

             treeModel.insertNodeInto(childTree, treeNode, pos);
          }
          TreePath path = addToIndex(childEnt, childTree, index, parent);
          updatePackageContents(childEnt, childTree, treeModel, index, path, byLayer);
          pos++;
       }
       for (TreeEnt element:ents.entries) {
          rix = -1;
          int tix = ents.removed != null ? ents.removed.indexOf(element) : -2;
          // Make sure that we don't call isVisible or anything on a removed element - the src might not be there, the class might be there and it could get loaded unnecessarily
          if ((ents.removed != null && (rix = ents.removed.indexOf(element)) != -1) || !element.isVisible(byLayer)) {
             removeChildNode(treeNode, element, treeModel);
             if (rix != -1)
                ents.removed.remove(rix);
             continue;
          }

          int ix = findChildNodeIndex(treeNode, element);

          // If there's already a directory element for this node, don't add a second one for the leaves
          if (subDirs.get(element.value) == null) {
                DefaultMutableTreeNode childNode;
             if (ix == -1) {
                childNode = new DefaultMutableTreeNode(element);
                treeModel.insertNodeInto(childNode, treeNode, pos);
             }
             else {
                if (ix == pos)
                   childNode = (DefaultMutableTreeNode) treeNode.getChildAt(ix);
                else {
                   // Need to reorder this element
                   treeModel.removeNodeFromParent(childNode = (DefaultMutableTreeNode) treeNode.getChildAt(ix));
                   treeModel.insertNodeInto(childNode, treeNode, pos);
                }
             }
             pos++;
             addToIndex(element, childNode, index, parent);
          }
          else if (ix != -1)
             removeChildNode(treeNode, element, treeModel);
       }
       if (ents.removed != null) {
          // TODO: no longer need this loop?
          for (TreeEnt rem:ents.removed) {
             removeChildNode(treeNode, rem, treeModel);
          }
          ents.removed = null;
       }
   }

   DefaultMutableTreeNode findChildNode(DefaultMutableTreeNode parent, Object userObj) {
      int ix = findChildNodeIndex(parent, userObj);
      if (ix == -1)
         return null;
      return (DefaultMutableTreeNode) parent.getChildAt(ix);
   }

   void removeChildNode(DefaultMutableTreeNode parent, Object userObj, DefaultTreeModel treeModel) {
      int ix = findChildNodeIndex(parent, userObj);
      if (ix == -1) {
         return;
      }

      // For some reason removing a selected node, even when there are multiple selections causes a change event
      // showing null paths.  When swapping visibility of nodes, this is a PITA.
      ignoreSelectionEvents = true;
      try {
         treeModel.removeNodeFromParent((DefaultMutableTreeNode) parent.getChildAt(ix));
      }
      finally {
         ignoreSelectionEvents = false;
      }
      // parent.remove(ix);
   }

   int findChildNodeIndex(DefaultMutableTreeNode parent, Object userObj) {
      for (int i = 0; i < parent.getChildCount(); i++) {
         if (((DefaultMutableTreeNode) parent.getChildAt(i)).getUserObject() == userObj)
            return i;
      }
      return -1;
   }

   void refreshLayerTree() {
      if (codeTypes == null || codeFunctions == null)
         return;

      boolean needsOpenRoot = false;
      if (rootLayerDirEnt == null) {
         rebuildLayerDirEnts();
         needsOpenRoot = true;
      }

      String rootName = getLayerRootName();

      // Now build the TreeNodes from that, sorting as we go
      if (rootLayerTreeNode == null) {
         rootLayerTreeNode = new DefaultMutableTreeNode(rootName);
         rootLayerTreeModel = new DefaultTreeModel(rootLayerTreeNode);
      }
      else {
         rootLayerTreeNode.setUserObject(rootName);
         if (rootLayerTreeNode.getChildCount() == 0)
            needsOpenRoot = true;
         rootLayerTreeModel.nodeChanged(rootLayerTreeNode);
      }

      rootLayerTreeIndex = new HashMap<String, List<TreePath>>();

      DefaultMutableTreeNode defaultNode = findChildNode(rootLayerTreeNode, layerEmptyCommentNode);
      if (defaultNode != null) {
         removeChildNode(rootLayerTreeNode, layerEmptyCommentNode, rootLayerTreeModel);
         needsOpenRoot = true;
      }
      updatePackageContents(rootLayerDirEnt, rootLayerTreeNode, rootLayerTreeModel, rootLayerTreeIndex, new TreePath(rootLayerTreeNode), true);

      if (rootLayerTreeNode.childCount == 0) {
         if (editorModel.codeFunctions.size()== CodeFunction.allSet.size() && editorModel.codeTypes.size()== CodeType.allSet.size())
            layerEmptyCommentNode.value = "<No layers>";
         else
            layerEmptyCommentNode.value = "<No matching layers>";
         defaultNode = new DefaultMutableTreeNode(layerEmptyCommentNode);
         rootLayerTreeModel.insertNodeInto(defaultNode, rootLayerTreeNode,0);
         needsOpenRoot = true;
      }

      layerTreeBuilt = true;

      if (needsOpenRoot)
         openRoot++;
   }

   static Color selectedNodeColor = new UIColor(0xFF, 0xFE, 0xDF);

   class TreeCellRenderer extends DefaultTreeCellRenderer {
       public java.awt.Component getTreeCellRendererComponent(
                           javax.swing.JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

           DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
           Object obj = node.getUserObject();

           setBackgroundNonSelectionColor(new UIColor((java.awt.Color) UIManager.get("Tree.textBackground")));

           if (obj instanceof String) {
              setIcon(null); // Root nodes
              setTextNonSelectionColor(GlobalResources.normalTextColor);
              setBackgroundNonSelectionColor(tree.getBackground());

              super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

              return this;
           }

           TreeEnt nodeInfo = (TreeEnt) obj;

           if (nodeInfo.transparent)
              setTextNonSelectionColor(GlobalResources.transparentTextColor);
           else if (nodeInfo.hasErrors())
              setTextNonSelectionColor(GlobalResources.errorTextColor);
           else if (nodeInfo.needsSave())
              setTextNonSelectionColor(GlobalResources.needsSaveTextColor);
           else
              setTextNonSelectionColor(GlobalResources.normalTextColor);

           super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

           // In createMode or layerMode, if the guy is selected, need to change the color so we identify the selected items
           if ((createMode || layerMode) && (editorModel.isTypeNameSelected(nodeInfo.getTypeName()) || (nodeInfo.type == EntType.Package && StringUtil.equalStrings(nodeInfo.value, editorModel.currentPackage))))
              setBackgroundNonSelectionColor(selectedNodeColor);
           else
             setBackgroundNonSelectionColor(tree.getBackground());


           switch (nodeInfo.type) {
              case Root:
              case Comment:
                 setIcon(null);
                 break;
              case ParentType:
              case Type:
                 if (ModelUtil.isDynamicType(nodeInfo.getTypeDeclaration()))
                    setIcon(GlobalResources.classDynIcon.icon);
                 else
                    setIcon(GlobalResources.classIcon.icon);
                 break;
              case ParentObject:
              case Object:
                 if (ModelUtil.isDynamicType(nodeInfo.getTypeDeclaration()))
                    setIcon(GlobalResources.objectDynIcon.icon);
                 else
                    setIcon(GlobalResources.objectIcon.icon);
                 break;
              case ParentEnum:
              case ParentEnumConstant:
              case EnumConstant:
              case Enum:
                 if (ModelUtil.isDynamicType(nodeInfo.getTypeDeclaration()))
                    setIcon(GlobalResources.enumDynIcon.icon);
                 else
                    setIcon(GlobalResources.enumIcon.icon);
                 break;
              case ParentInterface:
              case Interface:
                 if (ModelUtil.isDynamicType(nodeInfo.getTypeDeclaration()))
                    setIcon(GlobalResources.interfaceDynIcon.icon);
                 else
                    setIcon(GlobalResources.interfaceIcon.icon);
                 break;
              case LayerDir:
                 if (!layerMode)
                    break;
                 // fall through for layer mode
              case LayerFile:
                 if (nodeInfo.layer != null && nodeInfo.layer.dynamic)
                    setIcon(GlobalResources.layerDynIcon.icon);
                 else
                    setIcon(GlobalResources.layerIcon.icon);
                 break;
              case InactiveLayer:
                 setIcon(GlobalResources.inactiveLayerIcon.icon);
                 break;
              case Primitive:
                 String tn = nodeInfo.value;
                 if (tn.equals("String") || tn.equals("char"))
                    setIcon(GlobalResources.stringIcon.icon);
                 else if (tn.equals("int") || tn.equals("short") || tn.equals("byte") || tn.equals("long"))
                    setIcon(GlobalResources.intIcon.icon);
                 else if (tn.equals("float") || tn.equals("double"))
                    setIcon(GlobalResources.floatIcon.icon);
                 else if (tn.equals("boolean"))
                    setIcon(GlobalResources.booleanIcon.icon);
                 else
                    System.err.println("*** Unknown primitive type: " + tn);
           }
           return this;
       }
   }

   TreeCellRenderer getCellRenderer() {
      return new TreeCellRenderer();
   }

   void addTreePaths(List<TreePath> paths, boolean byLayer, String typeOrPkgName, boolean pkgName) {
      Map<String, List<TreePath>> index = byLayer ? rootLayerTreeIndex : rootTypeTreeIndex;
      if (index == null) // not initialized yet
         return;
      if (pkgName)
         typeOrPkgName = PKG_INDEX_PREFIX + typeOrPkgName;
      List<TreePath> l = index.get(typeOrPkgName);
      if (l == null || l.size() == 0)
         return;
      // TODO: filter by the currently selected layer?
      for (int i = 0; i < l.size(); i++) {
         paths.add(l.get(i));
      }
   }

   TreePath getTreePath(boolean byLayer, String typeName) {
      Map<String, List<TreePath>> index = byLayer ? rootLayerTreeIndex : rootTypeTreeIndex;
      List<TreePath> l = index.get(typeName);
      if (l == null || l.size() == 0)
         return null;
      // TODO: look for current selected layer and choose that one?
      return l.get(0);
   }

   boolean nodeExists(String typeName) {
      return getTreePath(false, typeName) != null;;
   }
}
