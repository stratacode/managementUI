import java.util.Arrays;

@sc.obj.Sync(syncMode=sc.obj.SyncMode.Automatic)
class EditorPanel {

   @sc.obj.Sync(syncMode=sc.obj.SyncMode.Automatic, includeSuper=true)
   object editorModel extends EditorModel {
   }

   @sc.obj.Sync(syncMode=sc.obj.SyncMode.Automatic, includeSuper=true)
   object typeTreeModel extends TypeTreeModel {
      editorModel := EditorPanel.this.editorModel;
   }

   enum ViewType {
       FormViewType, CodeViewType;
   }

   Object createTypeModeName;

   @sc.obj.Sync
   ViewType viewType = ViewType.FormViewType;

   String newTypeNameField :=: editorModel.createModeTypeName;  // Set on selection to a value to pre-populate the 'extends' form field
   String newLayerNameField;           // Set to populate the new layer field properties

   @sc.obj.Sync(syncMode=sc.obj.SyncMode.Disabled)
   boolean staleSelection = false;

   @sc.obj.Sync(syncMode=sc.obj.SyncMode.Automatic)
   class BaseTypeTree implements TypeTreeSelectionListener {
      // When a package is selected, stores the name of that package
      String currentPackageNode;

      boolean byLayer = false;

      String[] selectedTypeNames :=: editorModel.typeNames;
      ArrayList<TypeTreeModel.TreeEnt> selectedTreeEnts = new ArrayList<TypeTreeModel.TreeEnt>();

      public void clearSelection() {
         if (selectedTreeEnts != null) {
            for (TypeTreeModel.TreeEnt selEnt:selectedTreeEnts) 
               selEnt.selected = false;
            selectedTreeEnts.clear();
         }
      }

      int selectionChanged := editorModel.selectionChanged;
      selectionChanged =: updateListSelection();

      int updateSelectionCount = 0;
      int lastUpdateSelectionCount = 0;

      public void treeTypeAvailable(TypeTreeModel.TreeEnt treeEnt) {
         if (staleSelection) {
            for (TypeTreeModel.TreeEnt selEnt:selectedTreeEnts) 
               if (selEnt.cachedTypeDeclaration == null)
                 return;
            staleSelection = false;
            selectTreeEnts(selectedTreeEnts);
         }
      }

      public void selectTreeEnt(TypeTreeModel.TreeEnt treeEnt, boolean append) {
         if (!append)
            clearSelection();
         boolean createMode = editorModel.createMode;
         if (treeEnt.isSelectable() && ((createMode && !treeEnt.createModeSelected) || (!createMode && !treeEnt.selected))) {
            treeEnt.needsType = true;
            if (editorModel.createMode)
               treeEnt.createModeSelected = true;
            else
               treeEnt.selected = true;
            System.out.println("*** type name: " + treeEnt.typeName + " is selected");
            if (selectedTreeEnts == null) {
               System.err.println("*** Null treeEnts!");
               selectedTreeEnts = new ArrayList<TypeTreeModel.TreeEnt>();
            }
            selectedTreeEnts.add(treeEnt);
            if (treeEnt.cachedTypeDeclaration == null)
               staleSelection = true;
            else if (!staleSelection)
               selectTreeEnts(selectedTreeEnts);
         }
      }

      public void selectTreeEnts(ArrayList<TypeTreeModel.TreeEnt> treeEnts) {
         ArrayList<String> newTypeNames = new ArrayList<String>();

         boolean changeTypeNames = false;
         boolean firstType = true;
         String newPackageNode = null;
         boolean changePackageNode = false;
         for (TypeTreeModel.TreeEnt treeEnt:treeEnts) {
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
                  if (treeEnt.type == TypeTreeModel.EntType.LayerFile || treeEnt.type == TypeTreeModel.EntType.LayerDir) {
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
                     if (treeEnt.type != TypeTreeModel.EntType.Primitive && treeEnt.typeDeclaration != null) {
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
               if (!Arrays.equals(selectedTypeNames, newSelTypes)) {
                  selectedTypeNames = newSelTypes;
               }
               editorModel.selectionChanged++;
            }
         }
         lastUpdateSelectionCount = updateSelectionCount;
      }

      void updateListSelection() {
         typeTreeModel.rootTypeDirEnt.updateSelected();
         typeTreeModel.rootLayerDirEnt.updateSelected();
      }
   }

}
