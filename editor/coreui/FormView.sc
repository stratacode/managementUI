import sc.layer.Layer;
import sc.dyn.DynUtil;
import sc.type.CTypeUtil;

import sc.bind.Bind;
import sc.bind.AbstractListener;
import sc.bind.IListener;

import java.util.Iterator;

class FormView extends BaseView {
   /** When making change to a type, do we go ahead and update all instances? */
   boolean instanceMode = false;

   List<IElementEditor> childViews;

   Object getObjectForListElement(int ix) {
      Object currentObj = null;
      if (instanceMode) {
         // TODO: if this is a nested type, we should find the sub-object of the parent type
         if (editorModel.selectedInstances != null && editorModel.selectedInstances.size() > ix) {
            currentObj = editorModel.selectedInstances.get(ix);
            if (currentObj instanceof InstanceWrapper)
               currentObj = ((InstanceWrapper) currentObj).instance;
         }
      }
      return currentObj;
   }

}
