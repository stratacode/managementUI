import sc.lang.TemplateLanguage;
import sc.lang.java.ModelUtil;

ValueFieldInput {
   enteredText =: validateField();

   void validateField() {
      if (inputValue.trim().length() == 0) {
         errorText = "";
         return;
      }
      String err = ModelUtil.validateElement(TemplateLanguage.getTemplateLanguage().expression, inputValue, false);
      if (err != null) {
          errorText = err;
      }
      else
          errorText = "";
   }
}
