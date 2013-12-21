package com.cabolabs.openehr.opt.model

class OperationalTemplate {

   String uid
   String templateId
   String concept
   boolean isControlled = false
   
   // language is CODE_PHRASE, will be coded as terminology_id::code_string
   // http://en.wikipedia.org/wiki/List_of_ISO_639-1_codes
   String language
   
   
   // description
   String purpose // purpose language will be the same as the template language, so is not storeds
   List otherDetails = [] // List<Term>
   Term originalAuthor
   
   // Plays the role of a CComplexObject of aom
   ObjectNode definition
   
   // Added to simplify path management
   // Paths will be calculated by a parser
   List paths = []
}