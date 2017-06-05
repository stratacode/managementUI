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

   int newInstSelected := editorModel.newInstSelected;
   newInstSelected =: updateInstances();

   abstract List<IElementEditor> getChildViews();

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

   // We could revalidate the form when only the instance changes but it should be faster to just update the
   // instance in each form editor when switching between instances of the same type.
   void updateInstances() {
      if (childViews != null) {
         for (int i = 0; i < childViews.size(); i++) {
            IElementEditor childView = childViews.get(i);
            if (childView instanceof FormEditor) {
               FormEditor childForm = (FormEditor) childView;
               Object newInst = getObjectForListElement(i);
               if (childForm.instance != newInst) {
                  childForm.instance = newInst;
                  childForm.updateChildInsts();
               }
            }
         }
      }
   }
}
