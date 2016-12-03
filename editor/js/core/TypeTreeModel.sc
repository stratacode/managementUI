import sc.layer.CodeType;
import sc.layer.CodeFunction;

import sc.obj.Sync;
import sc.obj.SyncMode;

// The Javascript implementation of the type/layer tree in the UI.  This code should really be in coreui so it is shared. 
// The swingui code when written uses TreePath as part of its core and needs to be refactored to remove this
// swing dependency.  Fortunately we can also cleanly separate the classes until it's worth the time to refactor and bring them
// back in sync.
TypeTreeModel {
   // The swing version does a rebuild on the first refresh call but the web version doesn't
   // need to do that.
   rebuildFirstTime = false;

   public final static String PKG_INDEX_PREFIX = "<pkg>:";

   boolean nodeExists(String typeName) {
      return typeTree.rootTreeIndex.get(typeName) != null;
   }
}
