import sc.layer.Layer;
import sc.dyn.DynUtil;
import sc.type.CTypeUtil;

import sc.bind.Bind;
import sc.bind.AbstractListener;
import sc.bind.IListener;

import java.util.Iterator;

class FormView extends BaseView {
   boolean instanceMode = false;

   int newInstSelected := editorModel.newInstSelected;
   newInstSelected =: updateInstances();

   int instanceModeChanged := editorModel.instanceModeChanged;
   instanceModeChanged =: rebuildForm();

   List<Layer> currentLayers := editorModel.ctx.currentLayers;
   currentLayers =: updateFormProperties();

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

   InstanceWrapper getWrapperForListElement(int ix) {
      InstanceWrapper currentObj = null;
      if (instanceMode) {
      // TODO: if this is a nested type, we should find the sub-object of the parent type
         if (editorModel.selectedInstances != null && editorModel.selectedInstances.size() > ix) {
            currentObj = editorModel.selectedInstances.get(ix);
            return currentObj;
         }
      }
      return null;
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
               InstanceWrapper newWrapper = getWrapperForListElement(i);
               if (childForm.instance != newInst || childForm.wrapper != newWrapper) {
                  // There's a pending event to refresh the instances so our instances may not be valid. Need them to be valid so that we can find the selected instance in the UI.
                  if (!editorModel.refreshInstancesValid)
                     Bind.refreshBinding(childForm, "instancesOfType");
                  if (newInst != null) {
                     Object dt = DynUtil.getType(newInst);
                     if (dt != childForm.type && !ModelUtil.sameTypes(dt, childForm.type)) {
                        System.out.println("*** type and instance are not in sync");
                        return;
                     }
                  }
                  childForm.instance = newInst;
                  childForm.wrapper = newWrapper;
                  childForm.operatorChanged();
                  childForm.updateChildInsts();
               }
            }
         }
      }
   }

   void invalidateModel() {
      if (childViews != null) {
         for (IElementEditor child:childViews)
            child.invalidateEditor();
      }
   }
   
   void validateEditorTree() {
      if (childViews != null) {
         for (IElementEditor child:childViews)
            child.validateEditorTree();
      }
   }

   void validateSize() {}

   // Changing the current layers will invalidate the model but since the type does not change, FormView
   // propagates this event down to the children to update the properties.
   void updateFormProperties() {
      if (childViews != null) {
         for (IElementEditor child:childViews) {
            if (child instanceof TypeEditor)
               ((TypeEditor) child).updateProperties();
         }
      }
   }

   boolean validateTreeScheduled = false;

   // Called when a child element's size has changed, or in general to refresh the lists and sizes of all tree components
   void scheduleValidateTree() {
      if (validateTreeScheduled)
         return;
      validateTreeScheduled = true;
      DynUtil.invokeLater(new Runnable() {
         public void run() {
            validateTreeScheduled = false;
            validateEditorTree();
         }
      }, 0);
   }

   boolean validateSizeScheduled = false;

   void scheduleValidateSize() {
      if (validateSizeScheduled || validateTreeScheduled)
         return;
      validateSizeScheduled = true;
         DynUtil.invokeLater(new Runnable() {
            public void run() {
               validateSizeScheduled = false;
               validateSize();
            }
         }, 0);

   }

   boolean rebuildFormScheduled = false;

   // Called when a child element's size has changed, or in general to refresh the lists and sizes of all tree components
   void scheduleRebuildForm() {
      if (rebuildFormScheduled)
         return;
      rebuildFormScheduled = true;
         DynUtil.invokeLater(new Runnable() {
            public void run() {
               rebuildFormScheduled = false;
               rebuildForm();
            }
         }, 0);
   }

   abstract void rebuildForm();
}
