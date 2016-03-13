package sc.editor;

import sc.lang.java.ModelUtil;
import sc.lang.java.JavaModel;
import sc.lang.java.IVariableInitializer;

@sc.js.JSSettings(jsModuleFile="js/sceditor.js")
public editor.coreui extends model, gui.util.core {
   // This is one of those layers which could go either way...
   //defaultSyncMode = sc.obj.SyncMode.Automatic;
   codeType = sc.layer.CodeType.Application;
   codeFunction = sc.layer.CodeFunction.UI;

   liveDynamicTypes = layeredSystem.options.editEditor;
}
