package com.cabolabs.openehr.opt.model

import java.util.Map;

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
   
   Map nodes = [:] // TemplatePath -> ObjectNode (node) para pedir restricciones
   
   ObjectNode getNode(String path)
   {
      return this.nodes[path]
   }
   
   def getTerm(String archetypeId, String nodeId)
   {
      return getFromOntology(archetypeId, nodeId, 'text')
   }
   
   def getDescription(String archetypeId, String nodeId)
   {
      return getFromOntology(archetypeId, nodeId, 'description')
   }
   
   private String getFromOntology(String archetypeId, String code, String part)
   {
      assert part == "text" || part == "description", "part should be text or description and is ${part}"
      
      // Recursive search for the archetype root by archetypeId
      def root = findRoot(archetypeId)
      
      if (!root)
      {
         println "root not found for "+ archetypeId +" "+ code
         return
      }
      else
         println "root found "+ root.toString()
         
      //println root.termDefinitions
      //root.termDefinitions.each{ println it.code }
         
      // Get the term from the ontology of the root archetype
      def codedTerm = root.termDefinitions.find{ it.code == code }
      
      if (!codedTerm)
      {
         println "codedTerm not found "+ archetypeId +" "+ code
         return
      }
      else
         println "codedTerm found "+ codedTerm.code +" "+ codedTerm.term.text
      
      return codedTerm.term."${part}"
   }
   
   /**
    * Find root object node by archetypeId.
    */
   private ObjectNode findRoot(String archetypeId)
   {
      return findRootRecursive(this.definition, archetypeId)
   }
   private ObjectNode findRootRecursive(ObjectNode obj, String archetypeId)
   {
      println "findRootRecursive aid="+ archetypeId +" o.aid="+ obj.archetypeId
      if (obj.archetypeId == archetypeId) return obj // Stop recursion
      
      // can use any or find:
      // http://stackoverflow.com/questions/3049790/break-from-groovy-each-closure
      def root
      obj.attributes.any { attr ->
         root = findRootRecursive(attr, archetypeId)
         if (root) return true // any break, each does not break
      }
      return root
   }
   private ObjectNode findRootRecursive(AttributeNode attr, String archetypeId)
   {
      def root
      attr.children.any { obj ->
         println "each obj.aid="+ obj.archetypeId
         root = findRootRecursive(obj, archetypeId)
         if (root) return true // any break, each does not break
      }
      return root
   }
}