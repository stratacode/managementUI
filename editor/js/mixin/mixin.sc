package sc.html.tag;

public editor.js.mixin extends editor.js.main, html.schtml {
   hidden = !layeredSystem.options.editEditor;  
   liveDynamicTypes = layeredSystem.options.editEditor;
   exportPackage = false; // since we are mixed in with app layers, don't use our package as the default package
}
