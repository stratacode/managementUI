import java.util.Arrays;

import sc.obj.Sync;
import sc.obj.SyncMode;

// Comment before
@Sync(syncMode=SyncMode.Automatic)
// Comment after
class EditorPanel {

   @Sync(syncMode=SyncMode.Automatic, includeSuper=true)
   object editorModel extends EditorModel {
   }

   @Sync(syncMode=SyncMode.Automatic, includeSuper=true)
   object typeTreeModel extends TypeTreeModel {
      editorModel = EditorPanel.this.editorModel;
      viewType := EditorPanel.this.viewType;

      propertyMode := editorModel.createMode && editorModel.currentCreateMode == CreateMode.Property;
      createMode := editorModel.createMode;
      currentCreateMode := editorModel.currentCreateMode;
   }

   @Sync
   ViewType viewType = ViewType.DataViewType;

   String newTypeNameField :=: editorModel.createModeTypeName;  // Set on selection to a value to pre-populate the 'extends' form field
   String newLayerNameField;           // Set to populate the new layer field properties

   // TODO: statusPanel.createPanel is defined in each of the sub-layers but not in this layer so this will show as a syntax error sometimes in the IDE...
   // Should define an abstract implementation here that is overridden in each of the sub-layers
   newTypeNameField =: statusPanel.createPanel.newTypeSelected;
   newLayerNameField =: statusPanel.createPanel.newLayerSelected;

   @Sync(syncMode=SyncMode.Disabled)
   boolean staleSelection = false;

   @Sync(syncMode=SyncMode.Disabled)
   boolean typeTreeSelection;

   @Sync(syncMode=SyncMode.Disabled)
   boolean selectionChangeInProgress;

   @Sync(syncMode=SyncMode.Automatic)
   class BaseTypeTree implements TypeTreeSelectionListener {
      // When a package is selected, stores the name of that package
      String currentPackageNode;

      @Sync(syncMode=SyncMode.Disabled)
      TypeTree myTypeTree;

      @Sync(syncMode=SyncMode.Disabled)
      boolean byLayer = false;

      String[] selectedTypeNames :=: editorModel.typeNames;
      List<InstanceWrapper> selectedInstances :=: editorModel.selectedInstances;
      ArrayList<TypeTree.TreeEnt> selectedTreeNodes = new ArrayList<TypeTree.TreeEnt>();

      @Sync(syncMode=SyncMode.Disabled)
      String[] lastSelectedTypeNames;
      @Sync(syncMode=SyncMode.Disabled)
      List<InstanceWrapper> lastSelectedInstances;

      public void clearSelection() {
         if (selectedTreeNodes != null) {
            for (Object selEnt:selectedTreeNodes) {
               if (selEnt instanceof TypeTree.TreeEnt)
                  ((TypeTree.TreeEnt)selEnt).selected = false;
            }
            selectedTreeNodes.clear();
         }
      }

      @Sync(syncMode=SyncMode.Disabled)
      int updateSelectionCount = 0;
      @Sync(syncMode=SyncMode.Disabled)
      int lastUpdateSelectionCount = 0;

      @Sync(syncMode=SyncMode.Disabled)
      boolean updateListSelectionScheduled = false;

      // The updateListSelection needs to run after includeInstances has been set and the tree has been rebuilt or
      // else we won't find the newly added instance in the selection.
      void scheduleUpdateListSelection() {
         if (updateListSelectionScheduled)
            return;
         DynUtil.invokeLater(new Runnable() {
            public void run() {
               updateListSelectionScheduled = false;
               updateListSelection();
            }
         }, 0);
      }

      public void treeTypeAvailable(TypeTree.TreeEnt treeEnt) {
         if (staleSelection) {
            for (Object selNode:selectedTreeNodes) {
               if (selNode instanceof TypeTree.TreeEnt) {
                  TypeTree.TreeEnt selEnt = ((TypeTree.TreeEnt)selNode);
                  selEnt.selected = false;
                  if (selEnt.cachedTypeDeclaration == null)
                    return;
               }
            }
            staleSelection = false;
            selectTreeNodes(selectedTreeNodes);
         }
      }

      public void selectTreeEnt(TypeTree.TreeEnt treeEnt, boolean append) {
         if (!append)
            clearSelection();
         boolean createMode = editorModel.createMode;
         if (treeEnt.isSelectable() && ((createMode && !treeEnt.createModeSelected) || (!createMode && !treeEnt.selected))) {
            treeEnt.needsType = true;
            if (editorModel.createMode)
               treeEnt.createModeSelected = true;
            else
               treeEnt.selected = true;
            if (selectedTreeNodes == null) {
               System.err.println("*** Null treeEnts!");
               selectedTreeNodes = new ArrayList<TypeTree.TreeEnt>();
            }
            selectedTreeNodes.add(treeEnt);
            if (treeEnt.cachedTypeDeclaration == null)
               staleSelection = true;
            else if (!staleSelection)
               selectTreeNodes(selectedTreeNodes);
         }
      }

      public void selectTreeNodes(ArrayList<TypeTree.TreeEnt> treeNodes) {
         ArrayList<String> newTypeNames = new ArrayList<String>();
         ArrayList<InstanceWrapper> newInstances = new ArrayList<InstanceWrapper>();

         boolean changeTypeNames = false;
         boolean firstType = true;
         String newPackageNode = null;
         boolean changePackageNode = false;
         for (Object treeNode:treeNodes) {
            TypeTree.TreeEnt treeEnt = (TypeTree.TreeEnt) treeNode;
            switch (treeEnt.type) {
               case Package:
                  if (!typeTreeModel.createMode) {
                     newPackageNode = treeEnt.value;
                     changePackageNode = true;
                     editorModel.currentPackage = treeEnt.typeName;
                     editorModel.currentLayer = null;
                     changeTypeNames = true;
                  }
                  break;

               case LayerDir:
                  if (!typeTreeModel.createMode) {
                     newPackageNode = treeEnt.value;
                     changePackageNode = true;
                     editorModel.currentPackage = treeEnt.layer.packagePrefix;
                     editorModel.currentLayer = treeEnt.layer;
                     // The dir does not select the layer
                     //changeTypeNames = true;
                     //if (!newTypeNames.contains(treeEnt.typeName))
                     //   newTypeNames.add(treeEnt.typeName);
                  }
                  if (typeTreeModel.layerMode) {
                     newLayerNameField = treeEnt.layer.layerName;
                  }
                  break;

               case InactiveLayer:
                  newLayerNameField = treeEnt.srcTypeName;
                  break;

               case LayerGroup:
               /* layer groups do not have a package name so.
                  no easy way to register htem with the other view so
                  if (!typeTreeModel.createMode) {
                     changeTypeNames = true;
                     newPackageNode = treeEnt.value;
                     changePackageNode = true;
                  }
               */
                  break;

               case Comment:
                  break;

               default:
                  newPackageNode = null; // Replace currently selected package
                  changePackageNode = true;
                  String newTypeName;
                  if (treeEnt.type == TypeTree.EntType.LayerFile || treeEnt.type == TypeTree.EntType.LayerDir) {
                     if (treeEnt.layer != null) {
                        editorModel.currentPackage = treeEnt.layer.packagePrefix;
                        newTypeName = treeEnt.layer.layerName;
                     }
                     else {
                        editorModel.currentPackage = null;
                        newTypeName = treeEnt.srcTypeName;
                     }
                  }
                  else {
                     // Setting package to null also resets the property mode so leave it alone for primitives which do not have a package name
                     if (treeEnt.type != TypeTree.EntType.Primitive && treeEnt.typeDeclaration != null) {
                        editorModel.currentPackage = ModelUtil.getPackageName(treeEnt.typeDeclaration);
                     }
                     newTypeName = treeEnt.imported ? CTypeUtil.getClassName(treeEnt.typeName) : (editorModel.currentType != null && treeEnt.packageName != null && treeEnt.packageName.equals(CTypeUtil.getPackageName(DynUtil.getTypeName(editorModel.currentType, false))) ? treeEnt.value : treeEnt.typeName);
                  }
                  if (!typeTreeModel.createMode) {
                     changeTypeNames = true;
                     if (!newTypeNames.contains(treeEnt.typeName))
                        newTypeNames.add(treeEnt.typeName);
                     if (byLayer) {
                        if (lastUpdateSelectionCount == updateSelectionCount) {
                           // When you choose a type in the layer view, it also by default sets the current layer
                           editorModel.currentLayer = treeEnt.layer;
                        }
                        //viewTabsScroll.viewTabs.mergeToggle.selected = false;
                     }
                     else {
                        // If the current layer does not apply to the newly selected object, reset it to "all" so we get
                        // the latest one.
                        //Layer curLayer = viewTabsScroll.viewTabs.selectedLayer;
                        //if (curLayer != null && !ModelUtil.definedInLayer(treeEnt.getTypeDeclaration(), curLayer))

                        // Now the type tree always resets the layer to null instead of only when it was not in the current type
                        if (lastUpdateSelectionCount == updateSelectionCount) {
                           editorModel.currentLayer = ModelUtil.getLayerForType(null, treeEnt.getTypeDeclaration());
                        }
                        //viewTabsScroll.viewTabs.mergeToggle.selected = true;
                     }

                     // Make sure the instance gets updated since we set it the first time we are selected.
                     treeEnt.selected = true;
                     treeEnt.updateInstances();

                     if (treeEnt.instance != null) {
                        if (!newInstances.contains(treeEnt.instance))
                           newInstances.add(treeEnt.instance);
                     }
                  }
                  // In createMode only looking at the first path selected
                  else if (firstType){
                      newTypeNameField = newTypeName;
                  }
                  break;
            }

            if (firstType)
               firstType = false;
         }

         if (changePackageNode) {
            if (!DynUtil.equalObjects(currentPackageNode, newPackageNode)) {
               // This change is local to this list.
               currentPackageNode = newPackageNode;
            }
         }
         if (changeTypeNames) {
            if (!editorModel.createMode) {
               String[] newSelTypes = newTypeNames.toArray(new String[newTypeNames.size()]);
               boolean typeChanged = false;
               boolean instChanged = false;
               boolean instanceModeChanged = false;
               if (!Arrays.equals(selectedTypeNames, newSelTypes)) {
                  selectedTypeNames = newSelTypes;
                  typeChanged = true;
               }
               if (!typeChanged && !Arrays.equals(lastSelectedTypeNames, newSelTypes))
                  typeChanged = true;

               if (!DynUtil.equalObjects(editorModel.selectedInstances, newInstances)) {
                  int newSize = newInstances.size();
                  int oldSize = editorModel.selectedInstances == null ? 0 : editorModel.selectedInstances.size();
                  // When we go from having a current type to the same type with an instance or vice versa, need to rebuild the form view
                  if (newSize != oldSize && (newSize == 0 || oldSize == 0)) {
                     instanceModeChanged = true;
                  }
                  instChanged = true;
                  editorModel.selectedInstances = newInstances;
               }
               // Always need to update the list selection so we keep both type trees in view
               if (typeChanged || instChanged)
                  editorModel.selectionChanged++;
               // But if only the instance changed we also need to update the instance in the form
               if (instChanged && !typeChanged) {
                  if (instanceModeChanged)
                     editorModel.instanceModeChanged++;
                  else
                     editorModel.newInstSelected++;
               }
            }
         }
         lastUpdateSelectionCount = updateSelectionCount;
      }

      void updateListSelection() {
         if (typeTreeModel == null || typeTreeModel.typeTree == null || 
             typeTreeModel.typeTree.rootDirEnt == null || 
             typeTreeModel.byLayerTypeTree == null || 
             typeTreeModel.byLayerTypeTree.rootDirEnt == null)
            return;

         lastSelectedTypeNames = selectedTypeNames;
         lastSelectedInstances = selectedInstances == null ? null : new ArrayList<InstanceWrapper>(selectedInstances);

         boolean needsRefresh = typeTreeModel.typeTree.rootDirEnt.updateSelected(false);
         if (typeTreeModel.byLayerTypeTree.rootDirEnt.updateSelected(false))
            needsRefresh = true;
         if (needsRefresh)
            typeTreeModel.refresh();
      }
   }

   void ensureViewType(ViewType type) {
      if (viewType != type)
         viewType = type;
   }
}
