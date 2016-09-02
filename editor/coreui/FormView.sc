import sc.layer.Layer;
import sc.dyn.DynUtil;
import sc.type.CTypeUtil;

import sc.bind.Bind;
import sc.bind.AbstractListener;
import sc.bind.IListener;

import java.util.Iterator;

class FormView extends BaseView {
   /** When making change to a type, do we go ahead and update all instances? */
   boolean updateInstances = true;
   
}
