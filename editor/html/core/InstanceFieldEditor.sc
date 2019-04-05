InstanceFieldEditor {
   void updateWrapper(InstanceWrapper newWrapper) {
      Object inst;
      if (newWrapper == null)
         inst = null;
      else
         inst = newWrapper.instance;
      updateInstance(inst);
   }

   void updateInstance(Object inst) {
      if (inst == instance)
         return;

      instance = inst;

      if (instance != oldInstance) {
         updateInstancesForChildren();
         updateListeners(true);
         oldInstance = instance;
      }
   }

   void updateInstancesForChildren() {
      Object[] children = DynUtil.getObjChildren(repeatWrapper, null, true);
      if (children == null)
         return;

      for (Object child:children) {
         if (child instanceof Element) {
            Element pchild = (Element) child;

            IElementEditor childView = (IElementEditor) pchild.repeatVar;

            if (childView instanceof InstanceFieldEditor) {
               InstanceFieldEditor childCView = (InstanceFieldEditor) childView;
               Object childType = childCView.type;
               if (childType != null && ModelUtil.isObjectType(childType) && !ModelUtil.hasModifier(childType, "static")) {
                  Object childInst = instance == null ? null : DynUtil.getPropertyValue(instance, ModelUtil.getTypeName(childType));
                  childCView.updateInstance(childInst);
               }
            }
         }
      }
   }
}