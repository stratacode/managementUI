package sc.editor;

// the main layer to run the browser based program editor.   Keep modelImpl in front of jsui even though they are not directly dependent.  
// modelImpl is the server part of the model and jsui does not want to depend on the server, so it runs on both the client and server in sync. 
editor.js.main extends editor.modelImpl, editor.coreui, editor.js.core {
}
