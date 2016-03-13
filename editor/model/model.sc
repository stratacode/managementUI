package sc.editor;

import java.util.Map;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Set;


import sc.layer.SrcEntry;
import sc.layer.Layer;
import sc.layer.LayeredSystem;

import sc.layer.CodeType;
import sc.layer.CodeFunction;

import sc.type.CTypeUtil;

import sc.util.LinkedHashMap;
import sc.util.StringUtil;

import sc.dyn.DynUtil;

import sc.bind.Bind;
import sc.bind.IListener;
import sc.bind.AbstractListener;

import sc.lang.java.JavaModel;
import sc.lang.template.Template;
import sc.lang.java.VariableDefinition;
import sc.lang.sc.PropertyAssignment;
import sc.lang.java.IVariableInitializer;
import sc.lang.java.TypeDeclaration;
import sc.lang.java.ClientTypeDeclaration;
import sc.lang.EditorContext;

@sc.obj.Sync(syncMode=sc.obj.SyncMode.Automatic)
public editor.model extends util, gui.util.core, sys.layeredSystem {
   //defaultSyncMode = sc.obj.SyncMode.Automatic;
   codeType = sc.layer.CodeType.Declarative;
   codeFunction = sc.layer.CodeFunction.Model;

   // This layer depends on both JS and Java but we do not want extending layers to pick up 
   // these dependencies
   exportRuntime = false;
   inheritRuntime = false;

   liveDynamicTypes = layeredSystem.options.editEditor;

   void init() {
      // When the editor is enabled, by default we turn on dynamic types
      layeredSystem.options.liveDynamicTypes = true;
   }
}
