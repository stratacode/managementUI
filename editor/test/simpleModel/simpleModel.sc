package sc.editor;
// Because editor.model now depends on both client and server layers we need to make this
// server thing explicit here.  Or should there be a way for a layer not to propagate up it's
// client/server dependency?
public editor.test.simpleModel extends editor.modelImpl, example.simple {
/*
   void init() {
      // Exclude the javascript runtime.  All layers which extend this layer explicitly will also be excluded, unless they explicitly include a layer which uses JS
      excludeRuntime("js");

      // The servlet stuff requires the default runtime
      addRuntime(null);
   }
*/
}
