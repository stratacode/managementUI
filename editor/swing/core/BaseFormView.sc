BaseFormView extends JScrollPane implements EditorPanelStyle {
   int numCols = 1;
   int nestWidth = 10;


   visible :=: viewVisible;

   viewportView = contentPanel;

   void invalidateForm() {
      contentPanel.invalidateForm();
   }

   object contentPanel extends JPanel {
      void invalidateForm() {
      }
   }

   TypeView {

      int columnWidth := (int) ((BaseFormView.this.size.width - scrollBorder - 2 * borderSize) / numCols - xpad);
      int startY := ypad + borderTop;

      int nestLevel = 0;

      int scrollBorder = 25; // space for the scroll bar should it be needed

      int borderSize = 2;
      int borderTop = 25;
      int borderBottom = 0;


      ElementView implements IElementView {
         IElementView prev;
         int row, col;

         Object propertyType := propC == null ? null : ModelUtil.getVariableTypeDeclaration(propC);
         String propertyTypeName := propC == null ? "<no type>" : String.valueOf(propertyType);

         propertySuffix := (ModelUtil.isArray(propertyType) || propertyType instanceof List ? "[]" : "");
         
         String propertyOperator := propertyOperator(instance, propC);

         boolean propertyInherited := propC != null && ModelUtil.getLayerForMember(null, propC) != classViewLayer;

         String propertyOperator(Object instance, Object val) {
            return val == null ? "" : (instance != null ? "" : ModelUtil.getOperator(val) != null ? " " + ModelUtil.getOperator(val) : " =");
         }

         @Bindable
         int x := columnWidth * col + xpad,
             y := prev == null ? ypad + startY : prev.y + prev.height,
             width := columnWidth - (nestWidth + 2*xpad) * nestLevel, height;

      }
   }

}
