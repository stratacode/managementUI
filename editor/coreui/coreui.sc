package sc.editor;

/** 
 * The view model layers for the editor, generic for all UIs.
 *  extends modelImpl, the server-only layer in the client/server
 *  setup so the remote apis are visible. extends model so when
 *  modelImpl is excluded, runtimes still inherit those types. 
*/
@sc.js.JSSettings(jsModuleFile="js/sceditor.js")
public editor.coreui extends modelImpl, model {
   // This is one of those layers which could go either way...
   //defaultSyncMode = sc.obj.SyncMode.Automatic;
   codeType = CodeType.UI;

   liveDynamicTypes = layeredSystem.options.editEditor;
   hidden = !layeredSystem.options.editEditor; 
}
