package sc.editor;

public editor.modelImpl extends model, sys.layeredSystem {
   codeType = sc.layer.CodeType.Application;
   codeFunction = sc.layer.CodeFunction.Program;

   liveDynamicTypes = layeredSystem.options.editEditor;

/*
   void init() {
      // Exclude the javascript runtime.  All layers which extend this layer explicitly will also be excluded, unless they explicitly include a layer which uses JS
      excludeRuntime("js");

      // The servlet stuff requires the default runtime
      addRuntime(null);
   }
*/
}
