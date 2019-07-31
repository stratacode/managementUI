import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;


/** Stores the data behind the type tree, used for both the by types and by layers views. */
TypeTreeModel {
   excludePackages = {"java.awt", "javax.swing", "sc.obj", "java.util"};

   // Event property incremented each time we need to increment the "root" tree object
   int openRoot;

   boolean ignoreSelectionEvents = false;

   public void scheduleBuild() {
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            rebuild();
         }});
   }

   static Color selectedNodeColor = new UIColor(0xFF, 0xFE, 0xDF);

   class TreeCellRenderer extends DefaultTreeCellRenderer {
       public java.awt.Component getTreeCellRendererComponent(
                           javax.swing.JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

           TypeTree.TreeNode node = (TypeTree.TreeNode) value;
           Object obj = node.getUserObject();

           setBackgroundNonSelectionColor(new UIColor((java.awt.Color) UIManager.get("Tree.textBackground")));

           if (obj instanceof String) {
              setIcon(null); // Root nodes
              setTextNonSelectionColor(GlobalResources.normalTextColor);
              setBackgroundNonSelectionColor(tree.getBackground());

              super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

              return this;
           }

           if (obj instanceof TypeTree.TreeEnt) {
              TypeTree.TreeEnt nodeInfo = (TypeTree.TreeEnt) obj;

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
              /*
              if ((createMode || layerMode) && (editorModel.isTypeNameSelected(nodeInfo.getTypeName()) || (nodeInfo.type == TypeTree.EntType.Package && StringUtil.equalStrings(nodeInfo.value, editorModel.currentPackage))))
                 setBackgroundNonSelectionColor(selectedNodeColor);
              else */
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
                    break;
                 case Instance:
                    setIcon(GlobalResources.instanceIcon.icon);
                    break;
              }
           }
           else if (obj instanceof InstanceWrapper) {
              try {
                 setTextNonSelectionColor(GlobalResources.normalTextColor);

                 super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

                 setIcon(GlobalResources.instanceIcon.icon);
              }
              catch (Exception exc) {
                 System.err.println("*** exc: " + exc);
                 exc.printStackTrace();
              }
           }
           return this;
       }
   }

   TreeCellRenderer getCellRenderer() {
      return new TreeCellRenderer();
   }

   boolean addTreePaths(List<TreePath> paths, boolean byLayer, String typeOrPkgName, boolean pkgName) {
      Map<String, List<TreePath>> index = byLayer ? byLayerTypeTree.rootPathIndex : typeTree.rootPathIndex;
      if (index == null) // not initialized yet
         return false;
      if (pkgName)
         typeOrPkgName = PKG_INDEX_PREFIX + typeOrPkgName;
      List<TreePath> l = index.get(typeOrPkgName);
      if (l == null || l.size() == 0)
         return false;
      // TODO: filter by the currently selected layer?
      for (int i = 0; i < l.size(); i++) {
         paths.add(l.get(i));
      }
      return true;
   }

   boolean nodeExists(String typeName) {
       for (TypeTree typeTree:typeTrees) {
          if (typeTree.getTreePath(typeName) != null)
             return true;
       }
       return false;
   }
}
