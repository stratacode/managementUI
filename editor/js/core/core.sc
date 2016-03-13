package sc.editor;

@sc.js.JSSettings(prefixAlias="sce_", jsModuleFile="js/sceditor.js")
// To have modelImpl in the list or not?  Since it excludes itself now in the client runtime it should be ok in the list.  If not,
// there's no declared dependency on the remote methods we are calling from js.core.  At least in the IDE, editor.js.core ends up 
// before modelImpl and we do not resolve that method based on how inactiveLayers does the lookup now.
public editor.js.core extends html.core, js.layer, jetty.schtml, gui.util.html, editor.modelImpl, editor.coreui {
  codeType = sc.layer.CodeType.Application;
  codeFunction = sc.layer.CodeFunction.UI;

  // Set this to true for refresh when changing the editor files
  liveDynamicTypes = layeredSystem.options.editEditor;
}
