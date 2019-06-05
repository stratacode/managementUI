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
            rootName = "Select property type by layer";
         else if (treeModel.addLayerMode)
            rootName = "Select layer to include by file";
         else if (treeModel.createLayerMode)
            rootName = "Select extends layers by file";
         else if (treeModel.currentCreateMode == CreateMode.Instance)
            rootName = "Select new instance type by layer";
         else
            rootName = "Select type by layer";
      }
      else
         rootName = "Application Types by Layer";
      return rootName;
   }

   String getIdPrefix() {
       return "L";
   }
}
