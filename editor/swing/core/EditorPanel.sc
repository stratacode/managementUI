import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.border.EtchedBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.BevelBorder;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.ToolTipManager;

import java.util.Arrays;

import sc.layer.Layer;
import sc.layer.LayeredSystem;
import sc.lang.java.ModelUtil;

import sc.type.CTypeUtil;
import sc.util.StringUtil;
import sc.dyn.DynUtil;

EditorPanel extends JPanel implements EditorPanelStyle {
   LayeredSystem system = LayeredSystem.getCurrent();

   int buttonPanelHeight = 45;
   int toolBarHeight = 24 + 2*ypad;

   int yframeoff = 5;

   int toolBarY := ypad;
   int belowToolBarY := toolBarY + toolBarHeight + ypad;

   int editorX := treeWidth + 2*xpad;
   int editorY := belowToolBarY + buttonPanelHeight + 2*ypad;
   int editorWidth := (int) EditorPanel.this.size.width - treeWidth - 3*xpad;
   int editorHeight := (int) EditorPanel.this.size.height - editorY - 2*ypad - yframeoff - statusPanelHeight;

   int statusLineHeight = 32 + 2*ypad;
   int statusPanelHeight = statusLineHeight * 3;

   class ToolBarButton extends JButton {
      size := preferredSize;
      rolloverEnabled = true;
   }

   class BevelToggle extends JToggleButton {
      //border := selected ? BorderFactory.createLoweredBevelBorder() : BorderFactory.createRaisedBevelBorder();
      //border := selected ? BorderFactory.createEtchedBorder(EtchedBorder.LOWERED) : BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
      border := selected ? new SoftBevelBorder(BevelBorder.LOWERED) : new SoftBevelBorder(BevelBorder.RAISED);
   }

   class TextBevelToggle extends BevelToggle {
      size := SwingUtil.dimension(preferredSize.width + 12, preferredSize.height + 5);
   }

   class ToolBarToggleButton extends BevelToggle {
      size := preferredSize;
      rolloverEnabled = true;
   }

   object globalToolBar extends JToolBar {
      location := SwingUtil.point(xpad, toolBarY);
      size := SwingUtil.dimension(treeWidth, toolBarHeight);

      floatable = false;
      rollover = true;

      object saveButton extends ToolBarButton {
        toolTipText = "Save changes and update application";
        clickCount =: editorModel.ctx.save();
        enabled := editorModel.ctx.needsSave;
        icon = new ImageIcon(EditorPanel.class.getResource("images/save.gif"), "Save");
      }

      object undoButton extends ToolBarButton {
        toolTipText = "Undo changes";
        clickCount =: editorModel.ctx.undo();
        enabled := editorModel.ctx.canUndo;
        icon = new ImageIcon(EditorPanel.class.getResource("images/undo.gif"), "Undo");
      }

      object redoButton extends ToolBarButton {
        toolTipText = "Redo last undo";
        clickCount =: editorModel.ctx.redo();
        enabled := editorModel.ctx.canRedo;
        icon = new ImageIcon(EditorPanel.class.getResource("images/redo.gif"), "Redo");
      }

      object refreshButton extends ToolBarButton {
        toolTipText := system.staleCompiledModel ? "Restart application to apply changes: " + system.shortStaleInfo : "Refresh application - load changed source files";
        clickCount =: doRefresh();
        icon := new ImageIcon(EditorPanel.class.getResource(system.staleCompiledModel ? "images/needsrestart.gif":"images/refresh.gif"), "Refresh") ;
      }

      void doRefresh() {
         // Let them restart without a refresh if it's changed before they press
         if (system.staleCompiledModel) {
             showRestartRefreshDialog();
         }
         else  {
            // Always refresh if we did not restart
            system.refreshSystem();
            // Only show the restart dialog if we did not show it up front
            if (system.staleCompiledModel && system.canRestart) {
                showRestartDialog();
            }
         }
      }

      void showRestartDialog() {
         Object[] options = {"Restart", "Cancel"};
         int n = JOptionPane.showOptionDialog(EditorPanel.this,
             "Restart required to apply changes: " + system.staleInfo,
             "Restart?",
             JOptionPane.YES_NO_OPTION,
             JOptionPane.QUESTION_MESSAGE,
             null,     //do not use a custom Icon
             options,  //the titles of buttons
             options[0]); //default button title
          if (n == 0)
             editorModel.ctx.restart();
      }

      void showRestartRefreshDialog() {
         if (!system.canRestart) {
            Object[] options = {"Refresh", "Cancel"};
            int n = JOptionPane.showOptionDialog(EditorPanel.this,
                "Manual restart required to apply changes: \n" + system.staleInfo,
                "Manual restart required",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,     //do not use a custom Icon
                options,  //the titles of buttons
                options[0]); //default button title
             if (n == 0)
                system.refreshSystem();
         }
         else {
            Object[] options = {"Restart", "Refresh", "Cancel"};
            int n = JOptionPane.showOptionDialog(EditorPanel.this,
                "Restart required to apply changes: " + system.staleInfo,
                "Restart or refresh?",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,     //do not use a custom Icon
                options,  //the titles of buttons
                options[0]); //default button title
             if (n == 0)
                editorModel.ctx.restart();
             else if (n == 1)
                system.refreshSystem();
          }
      }
   }

   object editorToolBar extends JToolBar {
      location := SwingUtil.point(globalToolBar.location.x + globalToolBar.size.width + editorWidth - 30*3, toolBarY);
      size := SwingUtil.dimension(editorWidth, toolBarHeight);

      floatable = false;
      rollover = true;

      // TODO: remove these as they are not used
      int mergeLayerCt :=: editorModel.mergeLayerCt;
      boolean isMerged := mergeLayerCt > 0;

      int inheritTypeCt :=: editorModel.inheritTypeCt;
      boolean isInherited = inheritTypeCt > 0;

      /*
      object mergedViewButton extends ToolBarToggleButton {
         toolTipText := !isMerged ? "Show only the current layer of the selected types" : "Merge " + (mergeLayerCt+1) + " layers of the selected types";
         selected := isMerged;
         selected =: isMerged ? mergeLayerCt = 0 : mergeLayerCt = 1; // TODO add mouseEventListener to get shift status and increment mergeLayerCt
         icon := !isMerged ? new ImageIcon(EditorPanel.class.getResource("images/sepview.png"), "Separate View") :
                            new ImageIcon(EditorPanel.class.getResource("images/mergeview.png"), "Merged View");
      }

      object inheritButton extends ToolBarToggleButton {
         toolTipText := isInherited ? "Show only properties using the current type." : "Include properties inherited from the extends type.";
         selected := isInherited;
         selected =: isInherited ? inheritTypeCt = 0 : inheritTypeCt = 1;
         icon := !isInherited ? new ImageIcon(EditorPanel.class.getResource("images/noinherit.png"), "No Inherit") :
                                new ImageIcon(EditorPanel.class.getResource("images/inherit.png"), "Inherit");
      }
      */

      /*
      object sep extends JSeparator {
         orientation = SwingConstants.VERTICAL;
      }
      */

      object dataViewButton extends ToolBarToggleButton {
         toolTipText = "Show data view of selected instances";
         selected = true;
         selected =: selected ? viewType = ViewType.DataViewType : null;
         icon = new ImageIcon(EditorPanel.class.getResource("images/dataview.png"), "Data View");
      }

      object formViewButton extends ToolBarToggleButton {
         toolTipText = "Show form view of selected types";
         selected =: selected ? viewType = ViewType.FormViewType : null;
         icon = new ImageIcon(EditorPanel.class.getResource("images/formview.png"), "Form View");
      }

      object codeViewButton extends ToolBarToggleButton {
         toolTipText = "Show the source code for the selected types";
         selected =: selected ? viewType = ViewType.CodeViewType : null;
         icon = new ImageIcon(EditorPanel.class.getResource("images/codeview.png"), "Code View");
      }

      object buttonGroup extends ButtonGroup {
         buttons = {dataViewButton, formViewButton, codeViewButton};
      }
   }

   typeTreeModel {
      // Enable to create properties so prim values are displayed
      addLayerMode := statusPanel.createPanel.addLayerMode;
      createLayerMode := statusPanel.createPanel.createLayerMode;

      typeTree {
         selectionListener = splitPane.scrollTypeTree.typeTree;
      }
      byLayerTypeTree {
         selectionListener = splitPane.scrollLayerTree.layerTree;
      }
   }

   BaseTypeTree extends JTree {
      int openRoot := typeTreeModel.openRoot;
      openRoot =: openRootNode();

      boolean lastCreateMode = true;
      String lastPackageNode = null;

      void openRootNode() {
         if (model == null)
            return;

         /*
         DefaultMutableTreeNode rootNode = ((DefaultMutableTreeNode) model.getRoot());
         if (rootNode == null)
            return;
         */

         DefaultMutableTreeNode rootNode = myTypeTree.getOpenToRootEnt().treeNode;
         if (rootNode == null)
            rootNode = ((DefaultMutableTreeNode) model.getRoot());

         TreePath tp = new TreePath(rootNode.getPath());
         expandPath(tp);
         makeVisible(tp);
      }

      {
         selectionModel.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION;

         cellRenderer = typeTreeModel.cellRenderer;

         addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
               TreePath[] paths = selectionPaths;
               boolean inProgress = selectionChangeInProgress;

               try {
                  // Is this the original selection?  If so keep track of which one is the original and only clear out
                  // the selection if we are and there are no items selected (since there can be items selected in the other tree view)
                  if (!selectionChangeInProgress) {
                     selectionChangeInProgress = true;
                     typeTreeSelection = byLayer;

                     if (paths == null) {
                       if (!typeTreeModel.createMode)
                          selectedTypeNames = new String[0];
                       return;
                     }
                  }

                  if (paths != null) {
                     ArrayList<TypeTree.TreeEnt> treeEnts = new ArrayList<TypeTree.TreeEnt>();
                     for (TreePath path:paths) {
                        Object userObj = ((DefaultMutableTreeNode) path.lastPathComponent).userObject;
                        treeEnts.add((TypeTree.TreeEnt) userObj);
                     }
                     selectTreeNodes(treeEnts);
                  }
               }
               finally {
                  if (!inProgress)
                     selectionChangeInProgress = false;
               }
           }
         });
         addTreeWillExpandListener(new TreeWillExpandListener() {
            public void treeWillExpand(TreeExpansionEvent e) {
               TreePath expandPath = e.getPath();

               if (typeTreeModel.includeInstances) {
                  Object userObj = ((DefaultMutableTreeNode) expandPath.lastPathComponent).userObject;
                  if (userObj instanceof TypeTree.TreeEnt) {
                     TypeTree.TreeEnt treeEnt = (TypeTree.TreeEnt) userObj;
                     if (!treeEnt.open) {
                        treeEnt.open = true;
                        treeEnt.refreshNode();
                     }
                  }
               }
           }
           public void treeWillCollapse(TreeExpansionEvent e) {
           }
         });
      }

      void updateListSelection() {
         if (StringUtil.arraysEqual(lastSelectedTypeNames, selectedTypeNames) && lastCreateMode == typeTreeModel.createMode &&
             StringUtil.equalStrings(currentPackageNode, lastPackageNode) &&
             DynUtil.equalObjects(lastSelectedInstances, selectedInstances))
            return;
         updateSelectionCount++;
         String[] selTypeNames = selectedTypeNames;
         lastCreateMode = typeTreeModel.createMode;
         lastPackageNode = currentPackageNode;
         List<TreePath> paths = new ArrayList<TreePath>(selectedTypeNames.length);

         // In create mode, we immediately clear the selection because it just temporarily copies the value into the
         // dialog
         if (!typeTreeModel.createMode) {
            boolean instSelected = false;
            if (selectedInstances != null && selectedInstances.size() > 0) {
               // Need to fetch type and update the instances before we look for the treePaths here
               for (int i = 0; i < selTypeNames.length; i++) {
                  String selType = selTypeNames[i];
                  typeTreeModel.updateInstancesForType(selType, byLayer);
               }
               for (int i = 0; i < selectedInstances.size(); i++) {
                  InstanceWrapper wrapper = selectedInstances.get(i);
                  // note: for singleton tree nodes, we have treeEnt.instance set but don't create a special node in the index so just use
                  // the selectedTypeName to do the selection in that case.
                  if (typeTreeModel.addTreePaths(paths, byLayer, wrapper.typeName + ":" + wrapper.toString(), false))
                     instSelected = true;
               }
            }
            if (!instSelected) {
               for (int i = 0; i < selTypeNames.length; i++) {
                  typeTreeModel.addTreePaths(paths, byLayer, selTypeNames[i], false);
               }
            }
            // If we've selected a package in non create mode, that's just a regular selection, as the target for
            // a create perhaps.
            if (currentPackageNode != null) {
               typeTreeModel.addTreePaths(paths, byLayer, currentPackageNode, true);
            }
         }
         TreePath[] newPaths = paths.toArray(new TreePath[paths.size()]);
         if (!Arrays.equals(newPaths, selectionModel.selectionPaths)) {
            selectionModel.selectionPaths = newPaths;
         }

         super.updateListSelection();
      }
   }

   object splitPane extends JSplitPane {
      topComponent = scrollTypeTree;
      bottomComponent = scrollLayerTree;
      int treeHeight := (int) (EditorPanel.this.size.height - belowToolBarY - 2 * ypad - yframeoff - statusPanelHeight);
      dividerPosition := (int) (treeHeight / 2 - 2*ypad);
      orientation = VERTICAL_SPLIT;
      location := SwingUtil.point(xpad, belowToolBarY);
      size := SwingUtil.dimension(treeWidth, treeHeight);

      object scrollTypeTree extends JScrollPane {
         viewportView = typeTree;

         object typeTree extends BaseTypeTree {
            model := typeTreeModel.typeTree.rootTreeModel;
            myTypeTree := typeTreeModel.typeTree;

            rootTreeNode =: setCellRenderer(typeTreeModel.getCellRenderer());
         }
      }

      object scrollLayerTree extends JScrollPane {
         viewportView = layerTree;

         object layerTree extends BaseTypeTree {
            byLayer = true;
            myTypeTree := typeTreeModel.byLayerTypeTree;
            model := typeTreeModel.byLayerTypeTree.rootTreeModel;
         }
      }
   }

   editorModel {
      typeLayers =: viewTabsScroll.viewTabs.validateTabs();
   }

   object viewTabsScroll extends JScrollPane {
      location := SwingUtil.point(treeWidth + 2*xpad, belowToolBarY);
      viewportView = viewTabs;
      size := SwingUtil.dimension(editorWidth, buttonPanelHeight);
      object viewTabs extends JPanel {
         location := SwingUtil.point(0, 0);
         preferredSize := SwingUtil.dimension(lastComponent == null ? 300 : lastComponent.location.x + lastComponent.size.width + 2*xpad, buttonPanelHeight - 20);

         int centerY := (int) ((size.height - preferredSize.height) / 2);

         int layersLabelHeight := horizontalScrollBarVisible ? 6 : 12;

         object layerLabel extends JLabel {
            location := SwingUtil.point(xpad, layersLabelHeight);
            size := preferredSize;
            text = "Layers";
            visible := editorModel.typeLayers.size() > 0;
         }

         class LayerToggle extends TextBevelToggle {
            Layer layer;
            JComponent prev;

            icon := layer.dynamic ? GlobalResources.layerDynIcon.icon : GlobalResources.layerIcon.icon;

            text := layer.layerName;
            override @sc.bind.NoBindWarn
            location := SwingUtil.point((prev != null ? prev.location.x + prev.size.width : 0) + xpad, centerY);

            userSelected =: editorModel.ctx.layerSelected(layer, mouseEvent != null && mouseEvent.isShiftDown());
         }

/*
         object allToggle extends TextBevelToggle {
            text := editorModel.mergeLayers ? "All Layers" : "First Layer";
            location := SwingUtil.point(xpad, centerY);
            selected =: selected ? selectedLayer = null : null;
         }

         object mergeToggle extends BevelToggle {
            text = "Merge layers";
            size := preferredSize;
            selected :=: editorModel.mergeLayers;
         }

         object inheritToggle extends BevelToggle {
            text = "Inherit";
            location := SwingUtil.point(mergeToggle.location.x + mergeToggle.size.width + xpad, 0);
            size := preferredSize;
            selected :=: editorModel.inherit;
         }
*/

         List<Layer> currentLayers := editorModel.ctx.currentLayers;
         currentLayers =: validateSelected();
         JComponent lastComponent = null; // allToggle;

         /*
         object buttonGroup extends ButtonGroup {
            //buttons = Arrays.asList(new AbstractButton[]{allToggle});
            buttons = Arrays.asList(new AbstractButton[]{});
         }
         */

         void removeTabs() {
            // Leave the allToggle but remove the rest from bottom to top
            JComponent lastComp = layerLabel;
            int stop = 0;
            if (lastComp != null) {
               while (getComponent(stop++) != lastComp)
                  ;
            }
            for (int i = getComponentCount()-1; i >= stop; i--) {
               AbstractButton button = (AbstractButton) getComponent(i);
               //buttonGroup.remove(button);
               remove(i);

               // Remove bindings from the toggles and unregister them from the instance list
               DynUtil.dispose(button);
            }
         }

         void validateTabs() {
            removeTabs();

            JComponent lastComp = layerLabel;

            if (editorModel != null && editorModel.typeLayers != null) {
               for (int i = 0; i < editorModel.typeLayers.size(); i++) {
                   LayerToggle tog = new LayerToggle();
                   tog.prev = lastComp;
                   tog.layer = editorModel.typeLayers.get(i);
                   if (editorModel.ctx.currentLayers.contains(tog.layer))
                      tog.selected = true;
                   add(tog);
                   //buttonGroup.add(tog);
                   lastComp = tog;
               }
               lastComponent = lastComp;
            }
            else
               lastComponent = layerLabel;

            //if (model.currentLayer == null)
            //   allToggle.selected = true;
            viewTabsScroll.this.invalidate();
            invalidate();
            viewTabsScroll.this.doLayout();
            doLayout();
            viewTabsScroll.super.validate();
            validate();

            viewTabsScroll.repaint();
         }

         void validateSelected() {
            // Leave the all toggle, remove the rest from bottom to top
            for (int i = 0; i < getComponentCount(); i++) {
               java.awt.Component comp = getComponent(i);
               if (comp instanceof LayerToggle) {
                  LayerToggle button = (LayerToggle) comp;
                  Layer buttonLayer;
                  buttonLayer = button.layer;
                  boolean sel = editorModel.ctx.currentLayers.contains(buttonLayer);
                  if (sel != button.selected)
                     button.selected = sel;
               }
               //else if (button == allToggle) {
               //   button.selected = selectedLayer == null;
               //}
            }
         }

         void stop() {
            removeTabs();
         }
      }
   }

   object dataView extends DataView {
      location := SwingUtil.point(editorX, editorY);
      size := SwingUtil.dimension(editorWidth, editorHeight);
      editorModel := EditorPanel.this.editorModel;
      viewVisible = true;
   }

   object formView extends FormView {
      location := SwingUtil.point(editorX, editorY);
      size := SwingUtil.dimension(editorWidth, editorHeight);
      editorModel := EditorPanel.this.editorModel;
      viewVisible = false;
   }

   object codeView extends CodeView {
      location := SwingUtil.point(editorX, editorY);
      size := SwingUtil.dimension(editorWidth, editorHeight);
      editorModel := EditorPanel.this.editorModel;
      viewVisible = false;
   }

   object statusPanel extends JPanel {
      location := SwingUtil.point(0, editorY + editorHeight + ypad);
      size := SwingUtil.dimension(EditorPanel.this.size.width, statusPanelHeight);
      object addButton extends StatusButton {
         icon := new ImageIcon(EditorPanel.class.getResource(editorModel.createMode ? "images/minus.png" : "images/add.png"), editorModel.createMode ? "Return to Edit Mode" : "Enter Create Mode");
         location := SwingUtil.point(xpad, ypad);
         clickCount =: editorModel.createMode = !editorModel.createMode;
         focusPainted = false;
         borderPainted = false;
      }
      int editStart := (int) (addButton.location.x + addButton.size.width + xpad);
      object editFieldPanel extends EditFieldPanel {
         treeAlignedWidth := treeWidth - editStart;
         visible := !editorModel.createMode;
         location := SwingUtil.point(editStart, ypad);
         size := SwingUtil.dimension(statusPanel.size.width - editStart, statusPanelHeight);
         editorModel := EditorPanel.this.editorModel;
      }
      object createPanel extends CreatePanel {
         editorModel = EditorPanel.this.editorModel;
         visible := editorModel.createMode;
         location := SwingUtil.point(editStart, ypad);
         size := SwingUtil.dimension(statusPanel.size.width - editStart, statusPanelHeight);

         opComplete =: editFieldPanel.currentTypeTextField.requestFocus();

         void ensureViewType(ViewType type) {
            EditorPanel.this.ensureViewType(type);
         }
      }
   }

   createPanel = statusPanel.createPanel;

   // When viewType changes, update the visible status of each view
   viewType =: validateViewType();

   void validateViewType() {
      dataView.viewVisible = viewType == ViewType.DataViewType;
      formView.viewVisible = viewType == ViewType.FormViewType;
      codeView.viewVisible = viewType == ViewType.CodeViewType;
   }
}
