CreateMode {
   abstract CreateSubPanel createSubPanel(CreatePanel panel);

   Instance { 
      CreateInstance createSubPanel(CreatePanel panel) {
         return new CreateInstance(panel, this);
      }
   }
   Property {
      CreateProperty createSubPanel(CreatePanel panel) {
         return new CreateProperty(panel, this);
      }
   }
   Class {
      CreateType createSubPanel(CreatePanel panel) {
         return new CreateType(panel, this);
      }
   }
   Object {
      CreateType createSubPanel(CreatePanel panel) {
         return new CreateType(panel, this);
      }
   }
   Layer {
      CreateLayer createSubPanel(CreatePanel panel) {
         return new CreateLayer(panel, this);
      }
   }
}