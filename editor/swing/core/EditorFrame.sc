import sc.layer.CodeType;
import sc.layer.CodeFunction;
import sc.bind.Bind;
import sc.dyn.DynUtil;

import java.util.EnumSet;

EditorFrame extends AppFrame implements EditorPanelStyle {
   EditorPanel editorPanel;

   size = SwingUtil.dimension(1024, 768);

   // This is named JMenuBar so that this object definition sets the property JMenuBar on EditorFrame.
   object JMenuBar extends javax.swing.JMenuBar implements ComponentStyle {
      size := SwingUtil.dimension(windowWidth, preferredSize.height);

      object fileMenu extends JMenu implements ComponentStyle {
         text = "File";
         object newItem extends JMenu implements ComponentStyle {
            text = "Add...";
            object layerItem extends JMenuItem {
               text = "Layer";
               clickCount =: doAddLayer();

            }
            object classItem extends JMenuItem {
               text = "Class";
               clickCount =: enableTypeCreateMode("Class");
            }
            object objectItem extends JMenuItem {
               text = "Object";
               clickCount =: enableTypeCreateMode("Object");
            }
            object propertyItem extends JMenuItem {
               text = "Property";
               clickCount =: enablePropCreateMode();
            }
         }
         object quitItem extends JMenuItem {
            text = "Quit";

            clickCount =: doExit();

            void doExit() {
               DynUtil.dispose(EditorFrame.this);

               // Displays any bindings not removed from this dispose command: TODO: need a way to notify application code that we are exiting - a system.exit?
               if (Bind.trace) {
                  Bind.printAllBindings();
               }

               System.exit(0);
            }
         }
      }

      object editMenu extends JMenu implements ComponentStyle {
         text = "Edit";
         String selectionName := editorModel.currentSelectionName;
         object deleteItem extends JMenuItem {
            text := "Delete" + selectionName;
            enabled := editorModel.editSelectionEnabled;
            clickCount =: doDeleteCurrentSelection();
         }
         object mergeItem extends JMenuItem {
            text = "Merge...";
            enabled = false;
         }

      }
      object viewMenu extends JMenu implements ComponentStyle {
         text = "View";
         object allItem extends JRadioButtonMenuItem {
            text = "Show All Types of Code";
            selected = true;
            selected =: selected ? editorModel.codeTypes = new ArrayList(CodeType.allSet) : null;
         }
         object applicationItem extends JRadioButtonMenuItem {
            text = "Hide Framework Code";
            selected =: selected ? editorModel.codeTypes = new ArrayList(EnumSet.of(CodeType.Application,CodeType.Declarative)) : null;
         }
         object declarativeItem extends JRadioButtonMenuItem {
            text = "Show Declarative Only";
            selected =: selected ? editorModel.codeTypes = new ArrayList(EnumSet.of(CodeType.Declarative)) : null;
         }
         object buttonGroup extends ButtonGroup {
            buttons = {declarativeItem, applicationItem, allItem};
         }
         object sep extends JSeparator {
         }
         object showAllItem extends JCheckBoxMenuItem {
            text = "Show All Functions";
            selected = true;
            selected =: selected ? editorModel.changeCodeFunctions(CodeFunction.allSet) : null;
         }
         object programItem extends JCheckBoxMenuItem {
            text = "Application Code";
            selected =: selected ? editorModel.changeCodeFunctions(EnumSet.of(CodeFunction.Program)) : null;
         }
         object uiItem extends JCheckBoxMenuItem {
            text = "UI Code";
            selected =: selected ? editorModel.changeCodeFunctions(EnumSet.of(CodeFunction.UI)) : null;
         }
         object styleItem extends JCheckBoxMenuItem {
            text = "Style Settings";
            selected =: selected ? editorModel.changeCodeFunctions(EnumSet.of(CodeFunction.Style)) : null;
         }
         object businessItem extends JCheckBoxMenuItem {
            text = "Domain Model";
            selected =: selected ? editorModel.changeCodeFunctions(EnumSet.of(CodeFunction.Model)) : null;
         }
         object adminItem extends JCheckBoxMenuItem {
            text = "Admin Settings";
            selected =: selected ? editorModel.changeCodeFunctions(EnumSet.of(CodeFunction.Admin)) : null;
         }
         object showAllGroup extends ButtonGroup {
            buttons = {showAllItem, programItem, uiItem, styleItem, businessItem, adminItem};
         }
         object sep2 extends JSeparator {
         }
         object debugBindingEnabled extends JCheckBoxMenuItem {
            text = "Trace Bindings";
            selected :=: editorModel.debugBindingEnabled;
         }
      }
   }

   object editorPanel extends EditorPanel {
      location := SwingUtil.point(0, 0);
      // The -40 seems needed cause AppFrame misreports its height?  Maybe the window border?
      size := SwingUtil.dimension(windowWidth, windowHeight-40-ypad);
   }

   void doAddLayer() {
       super.doAddLayer();

       CreatePanel cp = editorPanel.statusPanel.createPanel;
       cp.createTypeChoice.selectedItem = "Layer";
       cp.addLayerField.requestFocus();
   }

   void showErrorDialog(String message, String title) {
      JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
   }

   int showOptionDialog(String message, String title, Object[] options, Object defaultTitle) {
      return JOptionPane.showOptionDialog(EditorFrame.this, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
          options,  //the titles of buttons
          defaultTitle); //default button title
   }

   void enableTypeCreateMode(String type) {
      if (editorModel.currentPackage != null) {
         editorModel.createMode = true;
         editorPanel.createTypeModeName = type;
      }
      else {
         UIUtil.showErrorDialog(this, "Select a layer or type to choose the destination package before clicking 'Add " + type + "'", "No package selected");
      }
   }

   void enablePropCreateMode() {
      if (editorModel.currentType != null) {
         editorModel.createMode = true;
         editorPanel.createTypeModeName = "Property";
      }
      else {
         UIUtil.showErrorDialog(this, "Select a type for the property before clicking 'Add'", "No type selected");
      }
   }

}
