import sc.layer.CodeType;
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
               clickCount =: enableCreateMode("Layer");
            }
            object classItem extends JMenuItem {
               text = "Class";
               clickCount =: enableCreateMode("Class");
            }
            object objectItem extends JMenuItem {
               text = "Object";
               clickCount =: enableCreateMode("Object");
            }
            object propertyItem extends JMenuItem {
               text = "Property";
               clickCount =: enableCreateMode("Property");
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
         object showAllItem extends JCheckBoxMenuItem {
            text = "Show All Code Types";
            selected = true;
            selected =: selected ? editorModel.changeCodeTypes(CodeType.allSet) : null;
         }
         object modelItem extends JCheckBoxMenuItem {
            text = "Domain Model";
            selected =: selected ? editorModel.changeCodeTypes(EnumSet.of(CodeType.Model)) : null;
         }
         object programItem extends JCheckBoxMenuItem {
            text = "Application Code";
            selected =: selected ? editorModel.changeCodeTypes(EnumSet.of(CodeType.Application)) : null;
         }
         object uiItem extends JCheckBoxMenuItem {
            text = "UI Code";
            selected =: selected ? editorModel.changeCodeTypes(EnumSet.of(CodeType.UI)) : null;
         }
         object persistItem extends JCheckBoxMenuItem {
            text = "Persist Code";
            selected =: selected ? editorModel.changeCodeTypes(EnumSet.of(CodeType.Persist)) : null;
         }
         object styleItem extends JCheckBoxMenuItem {
            text = "Style Settings";
            selected =: selected ? editorModel.changeCodeTypes(EnumSet.of(CodeType.Style)) : null;
         }
         object adminItem extends JCheckBoxMenuItem {
            text = "Admin Settings";
            selected =: selected ? editorModel.changeCodeTypes(EnumSet.of(CodeType.Admin)) : null;
         }
         object frameworkItem extends JCheckBoxMenuItem {
            text = "Framework Code";
            selected =: selected ? editorModel.changeCodeTypes(EnumSet.of(CodeType.Framework)) : null;
         }
         object deployItem extends JCheckBoxMenuItem {
            text = "Admin Settings";
            selected =: selected ? editorModel.changeCodeTypes(EnumSet.of(CodeType.Deploy)) : null;
         }
         object showAllGroup extends ButtonGroup {
            buttons = {showAllItem, modelItem, programItem, uiItem, persistItem, styleItem, adminItem, frameworkItem, deployItem};
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

   void showErrorDialog(String message, String title) {
      JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
   }

   int showOptionDialog(String message, String title, Object[] options, Object defaultTitle) {
      return JOptionPane.showOptionDialog(EditorFrame.this, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
          options,  //the titles of buttons
          defaultTitle); //default button title
   }

}
