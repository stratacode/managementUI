editor.packageFilter extends modelImpl {
   // This layer only configures components - it does not add classes.
   // It helps identify these types of layers and helps sorting of layers
   // in the IDE. It will closely follow modelImpl keeping it in front of
   // platform specific customizations.
   configLayer = true;
}
