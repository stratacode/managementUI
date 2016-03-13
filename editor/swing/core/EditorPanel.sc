import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.border.EtchedBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.BevelBorder;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.ToolTipManager;

import java.util.Arrays;

import sc.layer.Layer;
import sc.layer.LayeredSystem;
import sc.lang.java.ModelUtil;

import sc.type.CTypeUtil;
import sc.util.StringUtil;
import sc.bind.BindingContext;
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

   newTypeNameField =: typeTreeModel.propertyMode ? statusPanel.createPanel.propertyTypeField.text : statusPanel.createPanel.objExtendsTypeField.text;

   newLayerNameField =: typeTreeModel.createLayerMode ? statusPanel.createPanel.objExtendsTypeField.text : statusPanel.createPanel.addLayerField.text;

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
      location := SwingUtil.point(globalToolBar.location.x + globalToolBar.size.width + xpad, toolBarY);
      size := SwingUtil.dimension(editorWidth, toolBarHeight);

      floatable = false;
      rollover = true;

      boolean isMerged :=: editorModel.mergeLayers;

      boolean isInherited :=: editorModel.inherit;

      object mergedViewButton extends ToolBarToggleButton {
        toolTipText := isMerged ? "Show only the current layer of the selected types" : "Show the merged view of the selected types";
        selected :=: isMerged;
        icon := !isMerged ? new ImageIcon(EditorPanel.class.getResource("images/sepview.png"), "Separate View") :
                           new ImageIcon(EditorPanel.class.getResource("images/mergeview.png"), "Merged View");
      }

      object inheritButton extends ToolBarToggleButton {
        toolTipText := isInherited ? "Show only properties using the current type." : "Include properties inherited from the extends type.";
        selected :=: isInherited;
        icon := !isInherited ? new ImageIcon(EditorPanel.class.getResource("images/noinherit.png"), "No Inherit") :
                              new ImageIcon(EditorPanel.class.getResource("images/inherit.png"), "Inherit");
      }

      object sep extends JSeparator {
         orientation = SwingConstants.VERTICAL;
      }

      object formViewButton extends ToolBarToggleButton {
        toolTipText = "Show form view of selected types";
        selected = true;
        selected =: selected ? viewType = ViewType.FormViewType : null;
        icon = new ImageIcon(EditorPanel.class.getResource("images/formview.png"), "Form View");
      }

      object codeViewButton extends ToolBarToggleButton {
        toolTipText = "Show the source code for the selected types";
        selected =: selected ? viewType = ViewType.CodeViewType : null;
        icon = new ImageIcon(EditorPanel.class.getResource("images/codeview.png"), "Code View");
      }

      object buttonGroup extends ButtonGroup {
         buttons = {formViewButton, codeViewButton};
      }
   }

   typeTreeModel {
      // Enable to create properties so prim values are displayed
      propertyMode := statusPanel.createPanel.viewMode == CreatePanel.ViewMode.Property;
      createMode := editorModel.createMode;
      addLayerMode := statusPanel.createPanel.addLayerMode;
      createLayerMode := statusPanel.createPanel.createLayerMode;
   }

   BaseTypeTree extends JTree {
      int openRoot := typeTreeModel.openRoot;
      openRoot =: openRootNode();

      String[] lastSelectedTypeNames;
      boolean lastCreateMode = true;
      String lastPackageNode = null;

      void openRootNode() {
         if (model == null)
            return;
         DefaultMutableTreeNode rootNode = ((DefaultMutableTreeNode) model.getRoot());
         if (rootNode == null)
            return;

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

               if (typeTreeModel.ignoreSelectionEvents)
                  return;

               if (paths == null) {
                 if (!typeTreeModel.createMode)
                    selectedTypeNames = new String[0];
                 return;
               }

               ArrayList<TypeTreeModel.TreeEnt> treeEnts = new ArrayList<TypeTreeModel.TreeEnt>();
               for (TreePath path:paths) {
                  Object userObj = ((DefaultMutableTreeNode) path.lastPathComponent).userObject;
                  if (userObj instanceof TypeTreeModel.TreeEnt) {
                     TypeTreeModel.TreeEnt treeEnt = (TypeTreeModel.TreeEnt) userObj;
                     treeEnts.add(treeEnt);
                  }
               }

               selectTreeEnts(treeEnts);
           }
         });
      }

      void updateListSelection() {
         if (StringUtil.arraysEqual(lastSelectedTypeNames, selectedTypeNames) && lastCreateMode == typeTreeModel.createMode && StringUtil.equalStrings(currentPackageNode, lastPackageNode))
            return;
         updateSelectionCount++;
         lastSelectedTypeNames = selectedTypeNames;
         lastCreateMode = typeTreeModel.createMode;
         lastPackageNode = currentPackageNode;

         List<TreePath> paths = new ArrayList<TreePath>(selectedTypeNames.length);

         // In create mode, we immediately clear the selection because it just temporarily copies the value into the
         // dialog
         if (!typeTreeModel.createMode) {
            for (int i = 0; i < selectedTypeNames.length; i++) {
               typeTreeModel.addTreePaths(paths, byLayer, selectedTypeNames[i], false);
            }
            // If we've selected a package in non create mode, that's just a regular selection, as the target for
            // a create perhaps.
            if (currentPackageNode != null) {
               typeTreeModel.addTreePaths(paths, byLayer, currentPackageNode, true);

            }
         }
         TreePath[] newPaths = paths.toArray(new TreePath[paths.size()]);
         if (!Arrays.equals(newPaths, selectionModel.selectionPaths))
            selectionModel.selectionPaths = newPaths;
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
            model := typeTreeModel.rootTypeTreeModel;

            rootTreeNode =: setCellRenderer(typeTreeModel.getCellRenderer());
         }
      }

      object scrollLayerTree extends JScrollPane {
         viewportView = layerTree;

         object layerTree extends BaseTypeTree {
            byLayer = true;
            model := typeTreeModel.rootLayerTreeModel;
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

         class LayerToggle extends TextBevelToggle {
            Layer layer;
            JComponent prev;

            icon := layer.dynamic ? GlobalResources.layerDynIcon.icon : GlobalResources.layerIcon.icon;

            text := layer.layerName;
            location := SwingUtil.point((prev != null ? prev.location.x + prev.size.width : 0) + xpad, centerY);

            selected =: selected ? selectedLayer = layer : null;
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

         Layer selectedLayer :=: editorModel.currentLayer;
         selectedLayer =: validateSelected();
         JComponent lastComponent = null; // allToggle;

         object buttonGroup extends ButtonGroup {
            //buttons = Arrays.asList(new AbstractButton[]{allToggle});
            buttons = Arrays.asList(new AbstractButton[]{});
         }

         void removeTabs() {
            // Leave the allToggle but remove the rest from bottom to top
            JComponent lastComp = null; // allToggle;
            int stop = 0;
            if (lastComp != null) {
               while (getComponent(stop++) != lastComp)
                  ;
            }
            for (int i = getComponentCount()-1; i >= stop; i--) {
               AbstractButton button = (AbstractButton) getComponent(i);
               buttonGroup.remove(button);
               remove(i);

               // Remove bindings from the toggles and unregister them from the instance list
               DynUtil.dispose(button);
            }
         }

         void validateTabs() {
            removeTabs();

            JComponent lastComp = null; // allToggle;

            if (editorModel != null && editorModel.typeLayers != null) {
               for (int i = 0; i < editorModel.typeLayers.size(); i++) {
                   LayerToggle tog = new LayerToggle();
                   tog.prev = lastComp;
                   tog.layer = editorModel.typeLayers.get(i);
                   if (editorModel.currentLayer == tog.layer)
                      tog.selected = true;
                   add(tog);
                   buttonGroup.add(tog);
                   lastComp = tog;
               }
               lastComponent = lastComp;
            }
            else
               lastComponent = null; // allToggle
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
               JToggleButton button = (JToggleButton) getComponent(i);
               Layer buttonLayer;
               if (button instanceof LayerToggle) {
                  buttonLayer = ((LayerToggle) button).layer;
                  button.selected = buttonLayer == selectedLayer;
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

   object formView extends FormView {
      location := SwingUtil.point(editorX, editorY);
      size := SwingUtil.dimension(editorWidth, editorHeight);
      editorModel := EditorPanel.this.editorModel;
      viewVisible = true;
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
         visible := editorModel.createMode;
         location := SwingUtil.point(editStart, ypad);
         size := SwingUtil.dimension(statusPanel.size.width - editStart, statusPanelHeight);
         editorModel := EditorPanel.this.editorModel;

         opComplete =: editFieldPanel.currentTypeTextField.requestFocus();
      }
   }

   createTypeModeName =: statusPanel.createPanel.createTypeChoice.selectedItem;

   // When viewType changes, update the visible status of each view
   viewType =: validateViewType();

   void validateViewType() {
      formView.viewVisible = viewType == ViewType.FormViewType;
      codeView.viewVisible = viewType == ViewType.CodeViewType;
   }
}
