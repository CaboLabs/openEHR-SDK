package com.cabolabs.openehr.opt.model

@groovy.util.logging.Log4j
class OperationalTemplate {

   String uid
   String templateId
   String concept
   boolean isControlled = false

   // language is CODE_PHRASE, will be coded as terminology_id::code_string
   // http://en.wikipedia.org/wiki/List_of_ISO_639-1_codes
   String language // ISO_639-1::en


   // description
   String purpose // purpose language will be the same as the template language, so is not storeds
   List otherDetails = [] // List<Term>
   Term originalAuthor

   // Plays the role of a CComplexObject of aom
   ObjectNode definition // has attributes category, context and content of the COMPOSITION

   // Added to simplify path management
   // Paths will be calculated by a parser
   List paths = []

   Map nodes = [:] // TemplatePath -> ObjectNode (node) para pedir restricciones

   /*
    * gets a node by template path
    */
   Constraint getNode(String path)
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

   def getLangCode()
   {
      this.language.split('::')[1]
   }
   def getLangTerminology()
   {
      this.language.split('::')[0]
   }

   private String getFromOntology(String archetypeId, String code, String part)
   {
      assert part == "text" || part == "description", "part should be text or description and is ${part}"

      // Recursive search for the archetype root by archetypeId
      def root = findRoot(archetypeId)

      if (!root)
      {
         //log.info( "root not found for "+ archetypeId +" "+ code )
         return
      }
      else
      {
         //log.info( "root found "+ root.toString() )
      }

      //println root.termDefinitions
      //root.termDefinitions.each{ println it.code }

      // Get the term from the ontology of the root archetype
      //println "root term definition codes: "+ root.termDefinitions.code

      def codedTerm = root.termDefinitions.find{ it.code == code }

      if (!codedTerm)
      {
         //log.info( "codedTerm not found "+ archetypeId +" "+ code )
         return
      }
      else
      {
         //log.info( "codedTerm found "+ codedTerm.code +" "+ codedTerm.term.text )
      }

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
      //log.info( "findRootRecursive aid="+ archetypeId +" o.aid="+ obj.archetypeId )
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

         root = findRootRecursive(obj, archetypeId)
         if (root) return true // any break, each does not break
      }
      return root
   }

   public List<ObjectNode> getReferencedArchetypes()
   {
      return getReferencedArchetypesRecursive(this.definition)
   }

   private List<ObjectNode> getReferencedArchetypesRecursive(ObjectNode obj)
   {
      List<ObjectNode> ret = []

      if (obj.path == '/' || obj.type == 'C_ARCHETYPE_ROOT')
      {
         ret << obj
      }

      obj.attributes.each { attr ->
         ret.addAll( getReferencedArchetypesRecursive(attr) )
      }

      return ret
   }

   private List<ObjectNode> getReferencedArchetypesRecursive(AttributeNode attr)
   {
      List<ObjectNode> ret = []

      attr.children.each { obj ->
         ret.addAll( getReferencedArchetypesRecursive(obj) )
      }

      return ret
   }
}
