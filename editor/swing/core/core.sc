package sc.editor;

public editor.swing.core extends editor.modelImpl, editor.coreui, swing.style, swing.rtextarea, swing.autocomplete, gui.util.swing {
   codeType = sc.layer.CodeType.Application;
   codeFunction = sc.layer.CodeFunction.UI;

   liveDynamicTypes = layeredSystem.options.editEditor;

/*
   void init() {
      // Split this layer and it's sublayers out into a new process using the default 'java' runtime
      addProcess(sc.layer.ProcessDefinition.create("Desktop"));
   }
*/
}
