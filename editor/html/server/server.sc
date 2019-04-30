editor.html.server extends editor.html.main, editor.html.mixin {
   // Because this is also a mixin layer don't reset the app layer's package
   exportPackage = false;
}
