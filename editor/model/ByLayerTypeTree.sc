import sc.lang.ILanguageModel;
import sc.lang.java.ModelUtil;
import sc.lang.java.BodyTypeDeclaration;
import sc.lang.java.ITypeDeclaration;

class ByLayerTypeTree extends TypeTree {
   ByLayerTypeTree() {
      byLayer = true;
   }

   ByLayerTypeTree(TypeTreeModel model) {
      super(model);
      byLayer = true;
   }


   String getRootName() {
      String rootName;
      if (treeModel.createMode) {
         if (treeModel.propertyMode)
            rootName = "Select Property Type by Layer";
         else if (treeModel.addLayerMode)
            rootName = "Select Layer to Include by File";
         else if (treeModel.createLayerMode)
            rootName = "Select Extends Layers by File";
         else
            rootName = "Select Extends Type by Layer";
      }
      else
         rootName = "Application Types by Layer";
      return rootName;
   }

   String getIdPrefix() {
       return "L";
   }
}