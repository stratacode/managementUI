// This is an empty layer, used by clients as an "entry point" to include the set of layers
// for the editor.  
public editor.swing.main extends editor.swing.style, editor.swing.rtext {

   hidden = !layeredSystem.options.editEditor;  
   liveDynamicTypes = layeredSystem.options.editEditor; 

   codeType = sc.layer.CodeType.Declarative;
   codeFunction = sc.layer.CodeFunction.Program;
}
