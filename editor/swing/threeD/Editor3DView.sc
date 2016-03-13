
import sc.layer.LayeredSystem;
import sc.layer.Layer;
import sc.dyn.DynUtil;
import sc.util.StringUtil;
import sc.util.FileUtil;


import sc.lang.java.TypeDeclaration;
import sc.lang.java.JavaSemanticNode;
import sc.lang.java.BodyTypeDeclaration;
import sc.lang.java.ModelUtil;
import sc.lang.java.JavaModel;
import sc.lang.sc.PropertyAssignment;
import sc.parser.ParseUtil;

import sc.util.ArrayList;
import java.util.Set;

import javax.swing.SwingUtilities;

@Component
class Editor3DView extends CGroup {
   LayeredSystem system = LayeredSystem.getCurrent();

   List<LayerTemplate> layerTemplates = new ArrayList<LayerTemplate>();

   EditorModel model;

   // The editor model sends a "default event" after it is rebuilt so we know to rebuild our dependent stuff
   model =: invalidateModels();

   // The Java model sends a default event when it changes as well so we know to rebuild
   JavaModel currentJavaModel := model.currentJavaModel;
   currentJavaModel =: invalidateModels();

   boolean doMergedView = true;

   double xpad = 5, ypad = 5, cellPadX = 10, cellPadY = 10;
   double layerPageWidth = 400, layerPageHeight = 150;
   double topGutter = 10;
   double screenTop = layerPageHeight - headerLabelHeight - topGutter - ypad;
   double spaceBetweenLayers = 70;
   double labelHeight = 20;
   double headerLabelHeight = 26;
   double typeWidth := (layerPageWidth - 2*xpad - (numCols-1) * cellPadX) / numCols;
   int numTypes := model.inheritedTypes.size();
   int numCols := numTypes <= 1 ? 1 : numTypes <= 4 ? 2 : numTypes <= 9 ? 3 : 4;
   visible =: invalidateModels();
   //int numCols = 1;

   final static double MIN_GRID_HEIGHT = 20;

   x = -1.0;
   y = 0.0;

   // Recenter along the z dimension as the number of layers changes.
   // Space between the layers * number of spaces total (n - 1)  divided by 2 to center it.  Then need to scale
   // this into model coordinates since scale is applied before translate.
   z := -spaceBetweenLayers * ((layerTemplates.size()-1) / 2.0) * (2.0 / layerPageWidth);

   scaleX = scaleY = scaleZ = 2.0 / layerPageWidth;

   x =: markChanged();
   y =: markChanged();
   z =: markChanged();

   class MLabel extends CLabel {
      double labelPad := labelHeight * 0.20;
      borderTop := labelPad;
      borderBottom := labelPad;
      borderLeft := labelPad;
   }

   /*
   object axisGroup extends CGroup {
      z = 2;
      object color extends CColor {
         r = 1.0;
         g = 1.0;
         b = 0.0;
      }
      object axis extends CAxis {
      }
   }
   */

   object connectingLines extends CGroup {
      visible = false;
      object stipple extends CLineStipple {
      }
      object lines extends CShape {
         shape = Shapes.GL_LINES;
         r = 0.85;
         g = 0.25;
         b = 0.25;
      }
   }

   /* These get cloned one for each layer we display */
   class LayerTemplate extends CGroup {
      Layer layer;
      List<Object> layerTypes;
      ArrayList<LayerTemplate.TypeTemplate> typeTemplates = new ArrayList<LayerTemplate.TypeTemplate>();

      boolean mergeLayers = false;

      layerTypes =: rebuild();

      object background extends CRect {
         //object stipple extends CPolygonStipple {}

         bkg {
            r = 0.08;
            g := layer.dynamic ? 0.28 : 0.45;
            b := layer.dynamic ? 0.13 : 0.55;
         }

         x = 0.0;
         y := -layerPageHeight;
         z = 0;
         width := layerPageWidth;
         height := 2*layerPageHeight;

/*
         border {
            r = 0.15;
            g = 0.15;
            b = 0.15;
         }
*/
      }

      object layerName extends MLabel {
         text := "Layer: " + layer.layerName + (layer.packagePrefix.length() > 0 ? " (package: " + layer.packagePrefix +
               (layer.defaultModifier == null ? "" : ", " + layer.defaultModifier) + ")" : "");
         x := 0;
         y := layerPageHeight - headerLabelHeight;
         width := layerPageWidth;
         height := headerLabelHeight;

         z = OVERLAY_SPACE;
      }

      void rebuild() {
         int ix = 0;

         for (int i = 0; i < typeTemplates.size(); i++) {
            LayerTemplate.TypeTemplate child = typeTemplates.get(i);
            removeChild(child);
            typeTemplates.remove(i);
            i--;
         }

         if (layerTypes == null)
            return;

         for (Object type:layerTypes) {
            LayerTemplate.TypeTemplate tt = new LayerTemplate.TypeTemplate();
            tt.type = type;
            Object rootType = type;
            if (type instanceof TypeDeclaration) {
               rootType = ((TypeDeclaration) type).getModifiedByRoot();
            }

            typeTemplates.add(tt);
            tt.typeIndex = model.inheritedTypes.indexOf(rootType);
            if (tt.typeIndex == -1)
               System.err.println("*** Can't find type index!");
            addChild(tt);
         }
      }

      public String buildTypeNameText(Object type) {
         String typeName;
         return ModelUtil.elementToString(type, true);
      }

      class TypeTemplate extends CGroup {
         CLabel lastChild := propTemplates.size() == 0 ? null : (CLabel) propTemplates.get(propTemplates.size()-1);

         int numLabels := propTemplates.size();

         List<PropertyTemplate> propTemplates = new ArrayList<PropertyTemplate>();

         double width := typeWidth;
         double height := (numLabels + 1) * (labelHeight + ypad) + 2*ypad;

         int typeIndex;
         int row := typeIndex / numCols;
         int col := typeIndex % numCols;

         x := xpad + col * (typeWidth + cellPadX);

         //TypeTemplate tt := row == 0 ? null : LayerTemplate.this.typeTemplates.getNoError((row-1) * numCols + col);
         //y := tt == null ? screenTop - height : tt.y - height - cellPadY;

         Object type;

         type =: rebuild();

         z = OVERLAY_SPACE;

         object background extends CRect {
            width := TypeTemplate.this.width;
            height := TypeTemplate.this.height;
            bkg {
               r = g = b = 0.95;
            }
            border {
               //r = g = b = 0.15;
            }

            y = 0;
         }
         object typeName extends MLabel {
            text := buildTypeNameText(type);
            height := labelHeight + labelHeight*0.25;
            width := TypeTemplate.this.width;
            y := TypeTemplate.this.height - labelHeight - ypad;

            z = OVERLAY_SPACE;

            rect {
               bkg {
                 r = g = b = 0.85;
               }
               border {
                  //r = g = b = 0.15;
               }
            }
         }

         void rebuild() {
            for (int i = 0; i < propTemplates.size(); i++) {
               PropertyTemplate child = propTemplates.get(i);
               removeChild(child);
               propTemplates.remove(i);
               i--;
            }

            if (type == null)
               return;

            Object[] props = !mergeLayers ? model.getPropertiesForType(type) : ModelUtil.getDeclaredMergedProperties(type, null, true);
            int ix = 0;
            if (props != null) {
               for (Object p:props) {
                  if (model.filteredProperty(type, p, !mergeLayers))
                     continue;

                  Object member = p;
                  if (p instanceof PropertyAssignment)
                     member = ((PropertyAssignment) p).getAssignedProperty();


                  if (!ModelUtil.hasModifier(member, "static"))
                     continue;

                  StaticPropertyTemplate spt = new StaticPropertyTemplate();
                  spt.property = p;
                  spt.propIndex = ix;
                  addChild(spt);
                  propTemplates.add(spt);
                  ix++;
               }
               for (Object p:props) {
                  if (model.filteredProperty(type, p, !mergeLayers))
                     continue;

                  Object member = p;
                  if (p instanceof PropertyAssignment)
                     member = ((PropertyAssignment) p).getAssignedProperty();

                  // For now, only StrataCode members
                  if (p instanceof java.lang.reflect.Member)
                     continue;

                  if (ModelUtil.hasModifier(member, "static"))
                     continue;

                  PropertyTemplate pt = new PropertyTemplate();
                  pt.property = p;
                  pt.propIndex = ix;
                  addChild(pt);
                  propTemplates.add(pt);
                  ix++;
               }
            }
            // TODO: add methods
         }

         class PropertyTemplate extends MLabel {
            Object property;
            text := property == null ? "null" : toPropertyString(property);
            width := typeWidth - 2*xpad;
            height := labelHeight;
            int propIndex;
            x := xpad;
            PropertyTemplate prev := propIndex == 0 ? null : propTemplates.get(propIndex - 1);
            y := propIndex == 0 ? TypeTemplate.this.height - 2*(labelHeight + ypad) - ypad : prev.y - prev.height - ypad;

            z = OVERLAY_SPACE*2;
         }

         class StaticPropertyTemplate extends PropertyTemplate {

         }

         class MethodTemplate extends MLabel {
            Object method;
            text := ModelUtil.getMethodName(method);
         }

         private String toPropertyString(Object p) {
            String ret = ModelUtil.elementToString(p, true);
            if (mergeLayers && p instanceof PropertyAssignment && !ModelUtil.isReverseBinding(p))
               ret = ModelUtil.getClassName(((PropertyAssignment) p).getTypeDeclaration()) + " " + ret;
            return ret;
            /*
            if (p instanceof JavaSemanticNode)
               ret = ((JavaSemanticNode) p).toLanguageString().trim();
            else
               ret = ModelUtil.getPropertyName(p);

            StringBuilder sb = new StringBuilder();
            int retlen = ret.length();
            if (retlen > maxw) {
               sb.append(ret.subSequence(0, maxw/2-5));
               sb.append(" ... ");
               sb.append(ret.subSequence(retlen-maxw/2-5, retlen));
            }
            else {
               sb.append(ret);
            }
            return sb.toString();
            */
         }

      }
   }

   class MergedLayerTemplate extends LayerTemplate {
      mergeLayers = true;
      layerName {
         text := "Merged Layers";
      }

      background {
         bkg {
            // Need to use := to override the binding in LayerTemplate
            g := 0.20;
            r := 0.25;
            b := 0.20;
         }
      }
   }

   static int maxw = 52;

   class LayerSeparator extends CTranslate {
      z := -spaceBetweenLayers;
      y := 190;
      x := -190;
   }

   void init() {
      if (model != null && model.typeNames != null)
         rebuildModels();
      else
         modelsValid = true;
   }

   boolean modelsValid = false;
   void invalidateModels() {
      if (modelsValid) {
         modelsValid = false;

         SwingUtilities.invokeLater(new Runnable() {
            public void run() {
               rebuildModels();
               }});

      }
   }

   void rebuildModels() {
      if (modelsValid)
         return;
      modelsValid = true;

      if (!visible)
         return;

      model.rebuildModel();

      List<IRenderNode> children = getChildren();
      if (children != null) {
         /* First remove any old ones */
         for (int i = 0; i < children.size(); i++) {
            IRenderNode child;
            if ((child = children.get(i)) instanceof LayerTemplate || child instanceof LayerSeparator) {
               children.remove(i);
               i--;
            }
         }
         //layerTemplates = new ArrayList<LayerTemplate>();
         layerTemplates.clear();
      }

      if (model.typeNames == null || model.typeNames.length == 0) {
         connectingLines.lines.verts = new float[0][];
         return;
      }

      doMergedView = model.filteredTypeLayers.size() > 1;

      int numRows = (model.inheritedTypes.size() + numCols-1) / numCols;
      double[][] gridHeights = new double[numRows][numCols];
      double[][] gridYs = new double[numRows][numCols];

      for (int i = 0; i < numRows; i++)
         for (int j = 0; j < numCols; j++)
            gridHeights[i][j] = MIN_GRID_HEIGHT;

      for (int i = 0; i < model.filteredTypeLayers.size(); i++) {
         Layer layer = model.filteredTypeLayers.get(i);
         LayerTemplate t = new LayerTemplate();
         t.layer = layer;
         t.layerTypes = model.typesPerLayer.get(i);

         layerTemplates.add(t);
         addChild(t);
         t.init(getCanvas());

         addChild(new LayerSeparator());

         for (LayerTemplate.TypeTemplate tt:t.typeTemplates) {
            int row = tt.typeIndex / numCols;
            int col = tt.typeIndex % numCols;
            if (gridHeights[row][col] < tt.height)
               gridHeights[row][col] = tt.height;
         }
      }

      if (doMergedView) {
         LayerTemplate t = new MergedLayerTemplate();
         t.layer = model.filteredTypeLayers.get(model.filteredTypeLayers.size()-1);
         t.layerTypes = model.inheritedTypes;

         layerTemplates.add(t);
         addChild(t);
         t.init(getCanvas());

         for (LayerTemplate.TypeTemplate tt:t.typeTemplates) {
            int row = tt.typeIndex / numCols;
            int col = tt.typeIndex % numCols;
            if (gridHeights[row][col] < tt.height)
               gridHeights[row][col] = tt.height;
         }
      }

      if (numRows > 0) {

         // Convert the first row from height to y coordinates
         for (int j = 0; j < numCols; j++)
            gridYs[0][j] = screenTop - gridHeights[0][j];

         // Convert heights to positions for each of the subsequent rows
         for (int i = 1; i < numRows; i++)
            for (int j = 0; j < numCols; j++)
               gridYs[i][j] = gridYs[i-1][j] - gridHeights[i][j] - cellPadY;

         // Now assign y coords for the TypeTemplates
         for (int i = 0; i < layerTemplates.size(); i++) {
            LayerTemplate t = layerTemplates.get(i);
            Layer layer = t.layer;

            for (LayerTemplate.TypeTemplate tt:t.typeTemplates) {
               int row = tt.typeIndex / numCols;
               int col = tt.typeIndex % numCols;
               // Positition this guy at the top of the cell
               tt.y = gridYs[row][col] + (gridHeights[row][col] - tt.height);
            }
         }
      }

      buildLines();

   }

   private LayerTemplate.TypeTemplate getTypeTemplate(Object type) {
      Layer typeLayer = null;
      if (type instanceof TypeDeclaration) {
         TypeDeclaration td = (TypeDeclaration) type;
         typeLayer = td.getLayer();
      }

      LayerTemplate lt = layerTemplates.get(model.filteredTypeLayers.indexOf(typeLayer));

      return lt.typeTemplates.get(lt.layerTypes.indexOf(type));
   }

   private void buildLines() {
      ArrayList<float[]> lineVerts = new ArrayList<float[]>();

      int typeIx = 0;
      for (Object type:model.inheritedTypes) {
         if (type instanceof TypeDeclaration) {
            BodyTypeDeclaration prevTD = (BodyTypeDeclaration) type;
            BodyTypeDeclaration nextTD = prevTD.getModifiedType();
            int nextLayerPos;
            Layer nextLayer;
            LayerTemplate.TypeTemplate prevTT, nextTT;
            while (nextTD != null) {
               Layer prevLayer = prevTD.getLayer();
               nextLayer = nextTD.getLayer();
               if (model.currentLayer != null && model.currentLayer.layerPosition < nextLayer.layerPosition)
                  break;

               int prevLayerPos = model.filteredTypeLayers.indexOf(prevLayer);
               // skipping this layer?
               if (prevLayerPos == -1)
                  break;
               nextLayerPos = model.filteredTypeLayers.indexOf(nextLayer);
               if (nextLayerPos == -1)
                  break;
               prevTT = getTypeTemplate(prevTD);
               nextTT = getTypeTemplate(nextTD);

               connectTypesWithLines(lineVerts, prevTT, nextTT, prevLayerPos, nextLayerPos);

               BodyTypeDeclaration nextNext = nextTD.getModifiedType();

               prevTD = nextTD;
               nextTD = nextNext;
            }

            // On the first one, connect the merged view with the previous one.
            if (doMergedView) {
               nextTD = (BodyTypeDeclaration) type;
               nextLayer = nextTD.getLayer();
               nextLayerPos = model.filteredTypeLayers.indexOf(nextLayer);
               if (nextLayerPos == -1)
                  break;
               nextTT = getTypeTemplate(nextTD);

               int lastLayerPos = layerTemplates.size()-1;
               LayerTemplate lastLT = layerTemplates.get(lastLayerPos);

               LayerTemplate.TypeTemplate lastTT = lastLT.typeTemplates.get(typeIx);

               connectTypesWithLines(lineVerts, nextTT, lastTT, nextLayerPos, lastLayerPos);
            }
         }
         typeIx++;
      }

      connectingLines.lines.verts = lineVerts.toArray(new float[lineVerts.size()][]);
   }


   private void connectTypesWithLines(List<float[]> lineVerts, LayerTemplate.TypeTemplate prevTT, LayerTemplate.TypeTemplate nextTT, int prevLayerPos, int nextLayerPos) {
      // One
      float[] pt = new float[3];
      pt[0] = (float) prevTT.x;
      pt[1] = (float) prevTT.y;
      pt[2] = (float) spaceBetweenLayers * prevLayerPos;
      lineVerts.add(pt);

      pt = new float[3];
      pt[0] = (float) nextTT.x;
      pt[1] = (float) nextTT.y;
      pt[2] = (float) spaceBetweenLayers * nextLayerPos;
      lineVerts.add(pt);

      // Two
      pt = new float[3];
      pt[0] = (float) (prevTT.x + prevTT.width);
      pt[1] = (float) prevTT.y;
      pt[2] = (float) (spaceBetweenLayers * prevLayerPos);
      lineVerts.add(pt);

      pt = new float[3];
      pt[0] = (float) (nextTT.x + nextTT.width);
      pt[1] = (float) nextTT.y;
      pt[2] = (float) (spaceBetweenLayers * nextLayerPos);
      lineVerts.add(pt);

      // Three
      pt = new float[3];
      pt[0] = (float) prevTT.x;
      pt[1] = (float) (prevTT.y + prevTT.height);
      pt[2] = (float) (spaceBetweenLayers * prevLayerPos);
      lineVerts.add(pt);

      pt = new float[3];
      pt[0] = (float) nextTT.x;
      pt[1] = (float) (nextTT.y + nextTT.height);
      pt[2] = (float) (spaceBetweenLayers * nextLayerPos);
      lineVerts.add(pt);

      // Four
      pt = new float[3];
      pt[0] = (float) (prevTT.x + prevTT.width);
      pt[1] = (float) (prevTT.y + prevTT.height);
      pt[2] = (float) (spaceBetweenLayers * prevLayerPos);
      lineVerts.add(pt);

      pt = new float[3];
      pt[0] = (float) (nextTT.x + nextTT.width);
      pt[1] = (float) (nextTT.y + nextTT.height);
      pt[2] = (float) (spaceBetweenLayers * nextLayerPos);
      lineVerts.add(pt);


   }

}
