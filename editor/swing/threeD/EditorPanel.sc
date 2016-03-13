EditorPanel {
   ViewType {
      ThreeDViewType {
      }
   }
   
   editorToolBar {
      object threeDViewButton extends ToolBarToggleButton {
        toolTipText = "Show 3D view of the selected types";
        selected =: selected ? viewType = ViewType.ThreeDViewType : null;
        icon = new ImageIcon(EditorPanel.class.getResource("images/3dview.png"), "3D View");
      }

      buttonGroup {
         buttons = {formViewButton, codeViewButton, threeDViewButton};
      }
   }

   void validateViewType() {
      canvas.visible = viewType == ViewType.ThreeDViewType;
      super.validateViewType();
   }

   object canvas extends CCanvas {
      location := SwingUtil.point(editorX, editorY);
      size := SwingUtil.dimension(editorWidth, editorHeight);
      visible = false;

      backgroundRed = 0.9f;
      backgroundGreen = 0.9f;
      backgroundBlue = 0.9f;

      object editor3DView extends Editor3DView {
         model = editorModel;
         visible := EditorPanel.this.canvas.visible;
      }
   }
}
