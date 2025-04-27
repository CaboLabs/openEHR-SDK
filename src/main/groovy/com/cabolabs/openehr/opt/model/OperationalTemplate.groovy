package com.cabolabs.openehr.opt.model

// to use the rm_attributes_not_in_opt
import com.cabolabs.openehr.rm_1_0_2.Model
import com.cabolabs.openehr.opt.model.primitive.*

@groovy.util.logging.Slf4j
class OperationalTemplate {

   String uid
   String templateId
   String concept
   boolean isControlled = false
   boolean isCompleted = false // true when complete() is called

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

   // Map<String, List<ObjectNode>>
   Map nodes = [:] // TemplatePath -> lista de nodos alternativos con la misma path


   /*
    * gets a node by template path
    */
   List<Constraint> getNodes(String path)
   {
      this.nodes[path]
   }

   // This is to deprecate getNodesByTemplateDataPath()
   Constraint getNodeByDataPath(String dataPath)
   {
      this.nodes.values().flatten().find { it.templateDataPath == templateDataPath }
   }

   // this considers the paths without the alternative index in them
   boolean existsNodeByTemplatePath(String templatePath)
   {
      this.nodes.values().flatten().find { it.templatePath == templatePath } != null
   }

   // this considers the paths with the alternative index in them
   boolean existsNodeByTemplateDataPath(String templateDataPath)
   {
      this.nodes.values().flatten().find { it.templateDataPath == templateDataPath } != null
   }

   List<Constraint> getNodesByTemplatePath(String templatePath)
   {
      this.nodes.values().flatten().findAll { it.templatePath == templatePath }
   }

   // FIXME: this should return 0..1 nodes not a list
   List<Constraint> getNodesByTemplateDataPath(String templateDataPath)
   {
      // findAll returns MapEntry
      // .values() return java.util.LinkedHashMap$LinkedValues not List
      // that contains many lists, .flatten() makes just one flat list and it's ArrayList

      this.nodes.values().flatten().findAll { it.templateDataPath == templateDataPath }
   }

   // NOTE: looking the code by archetype Id at the top level is not enough since we
   // could have multiple occurrences of the same archetype with different constraints
   // for the same nodeId: the text could be different for different instances of constraints
   // for the same archetype. But the codes at the current archetype root should be right.
   // TODO: So we might need to remove this method from here since the correct search is bottom up
   // to the archetype root, not top-down from the template root...
   def getTerm(String archetypeId, String nodeId)
   {
      getFromOntology(archetypeId, nodeId, 'text')
   }

   def getDescription(String archetypeId, String nodeId)
   {
      getFromOntology(archetypeId, nodeId, 'description')
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
      // FIXME: this only finds the first occurrence of the archetypeId, if there are two object constraints
      // for the same archetypeID this won't find the second.
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
    * FIXME: the same archetype could be used many times in the same OPT, so
    * there could be many roots for the same archetypeId, and those could have
    * different constraints like occurs 0..1 and 0..0
    */
   ObjectNode findRoot(String archetypeId)
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

   /*
    * Adds attributes to the OPT that are defined in the RM but not in the OPT.
    * Some could be optional in the OPT, like COMPO.context will be in the OPT if
    * there is a constraint for other_context.
    */
   def complete()
   {
      completeRecursive(this.definition)
      this.isCompleted = true
   }

   // Processes a single node alternative type
   private completeNodeSingle(AttributeNode atnc, String attr, String type)
   {
      def aom_type, obnc
      if (Model.primitive_types.contains(type))
      {
         aom_type = 'C_PRIMITIVE_OBJECT'

         // NOTE: this injected P.O.N. should have an item CPrimitive constraint, it's required by the AOM
         obnc = new PrimitiveObjectNode()

         def primitive_type = 'com.cabolabs.openehr.opt.model.primitive.C'+ type // e.g. CString
         obnc.item = Class.forName(primitive_type).newInstance() // NOTE: we can't add any constraints to the CPrimitive, so any value is allowed
      }
      else
      {
         aom_type = 'C_COMPLEX_OBJECT'
         obnc = new ObjectNode()
      }

      // NOTE: atnc.parent = obn

      // avoid // on root paths
      def path_sep = "/"
      if (atnc.parent.path == "/") path_sep = ""

      obnc.owner            = this // this template
      obnc.rmTypeName       = type
      obnc.type             = aom_type
      obnc.templatePath     = atnc.parent.templatePath     + path_sep + attr // same paths as the attr since this has no nodeId
      obnc.path             = atnc.parent.path             + path_sep + attr
      obnc.dataPath         = atnc.parent.dataPath         + path_sep + attr
      obnc.templateDataPath = atnc.parent.templateDataPath + path_sep + attr
      obnc.parent           = atnc
      obnc.occurrences      = new IntervalInt( // TODO: check the RM to see the default RM occurrences for this object
         upperIncluded:  true,
         lowerIncluded:  true,
         upperUnbounded: false,
         lowerUnbounded: false,
         lower: 0,
         upper: 1
      )

      // TODO: default_values

      // Add dummy text and description for the new nodes
      obnc.text = obnc.parent.parent.text +'.'+ obnc.parent.rmAttributeName
      obnc.description = obnc.parent.parent.description +'.'+ obnc.parent.rmAttributeName

      atnc.children << obnc


      // Add nodes to the OPT
      // supports many alternative nodes with the same path
      // TEST: should the key be templatePath or path?
      if (!this.nodes[obnc.templatePath]) this.nodes[obnc.templatePath] = []
      this.nodes[obnc.templatePath] << obnc


      // This while assigns the current generated node to all it's ascedant nodes,
      // in their flat list, so when getting any of those nodes, the new injected
      // node will be there and can be retrieved by it's path.
      def parent_obn = atnc.parent
      while (parent_obn)
      {
         if (!parent_obn.nodes[obnc.path]) parent_obn.nodes[obnc.path] = []
         parent_obn.nodes[obnc.path] << obnc

         parent_obn = parent_obn?.parent?.parent
      }
   }

   private completeNodeAlternatives(AttributeNode atnc, String attr, List types)
   {
      types.each { type ->

         completeNodeSingle(atnc, attr, type)
      }
   }

   private completeAttribute(ObjectNode obn, String attr, Object type_or_types)
   {
      // avoid // on root paths
      def path_sep = "/"
      if (obn.path == "/") path_sep = ""

      def atnc = new AttributeNode(
         rmAttributeName:  attr,
         type:             'C_SINGLE_ATTRIBUTE',
         parent:           obn,
         path:             obn.path             + path_sep + attr,
         dataPath:         obn.dataPath         + path_sep + attr,
         templatePath:     obn.templatePath     + path_sep + attr,
         templateDataPath: obn.templateDataPath + path_sep + attr,
         existence:        new IntervalInt( // TODO: check the RM to see the RM existence for this attribute
            upperIncluded:  true,
            lowerIncluded:  true,
            upperUnbounded: false,
            lowerUnbounded: false,
            lower: 0,
            upper: 1
         )
      )

      if (type_or_types instanceof List)
      {
         completeNodeAlternatives(atnc, attr, type_or_types)
      }
      else
      {
         completeNodeSingle(atnc, attr, type_or_types)
      }

      obn.attributes << atnc
   }

   private completeRecursive(ObjectNode obn)
   {
      // attr name -> type
      Map rm_attrs = Model.rm_attributes_not_in_opt[obn.rmTypeName]

      def path_sep, aom_type, atnc, obnc, parent_obn, primitive_type

      rm_attrs.each { attr, type ->

         // avoid if the attr is already on the OPT
         // for instance, a null_flavour could be in the OPT
         if (!obn.attributes.find{ it.rmAttributeName == attr })
         {
            // TODO: support that type could be a list of possible types (inheritance structure only with concrete types), I guess here we should pick one or just add all the alternative types.

            completeAttribute(obn, attr, type) // type can be a list

            /*
            if (Model.primitive_types.contains(type))
            {
               aom_type = 'C_PRIMITIVE_OBJECT'

               // NOTE: this injected P.O.N. should have an item CPrimitive constraint, it's required by the AOM
               obnc = new PrimitiveObjectNode()

               primitive_type = 'com.cabolabs.openehr.opt.model.primitive.C'+ type // e.g. CString
               obnc.item = Class.forName(primitive_type).newInstance() // NOTE: we can't add any constraints to the CPrimitive, so any value is allowed
            }
            else
            {
               aom_type = 'C_COMPLEX_OBJECT'
               obnc = new ObjectNode()
            }

            // avoid // on root paths
            path_sep = "/"
            if (obn.path == "/") path_sep = ""

            atnc = new AttributeNode(
               rmAttributeName:  attr,
               type:             'C_SINGLE_ATTRIBUTE',
               parent:           obn,
               path:             obn.path +path_sep+ attr,
               dataPath:         obn.dataPath +path_sep+ attr,
               templatePath:     obn.templatePath +path_sep+ attr,
               templateDataPath: obn.templateDataPath +path_sep+ attr,
               existence:        new IntervalInt( // TODO: check the RM to see the RM existence for this attribute
                  upperIncluded:  true,
                  lowerIncluded:  true,
                  upperUnbounded: false,
                  lowerUnbounded: false,
                  lower: 0,
                  upper: 1
               )
            )


            completeNodeSingle(atnc, attr, type)
            */


            /*
            obnc.owner            = this // this template
            obnc.rmTypeName       = type
            obnc.type             = aom_type
            obnc.templatePath     = obn.templatePath +path_sep+ attr // same paths as the attr since this has no nodeId
            obnc.path             = obn.path +path_sep+ attr
            obnc.dataPath         = obn.dataPath +path_sep+ attr
            obnc.templateDataPath = obn.templateDataPath +path_sep+ attr
            obnc.parent           = atnc
            obnc.occurrences      = new IntervalInt( // TODO: check the RM to see the default RM occurrences for this object
               upperIncluded:  true,
               lowerIncluded:  true,
               upperUnbounded: false,
               lowerUnbounded: false,
               lower: 0,
               upper: 1
            )

            // TODO: default_values

            // Add dummy text and description for the new nodes
            obnc.text = obnc.parent.parent.text +'.'+ obnc.parent.rmAttributeName
            obnc.description = obnc.parent.parent.description +'.'+ obnc.parent.rmAttributeName

            atnc.children << obnc
            */


            // Add nodes to the OPT
            // supports many alternative nodes with the same path
            // TEST: should the key be templatePath or path?
            // if (!this.nodes[obnc.templatePath]) this.nodes[obnc.templatePath] = []
            // this.nodes[obnc.templatePath] << obnc

//               obn.attributes << atnc

            // NOTE: the code below seted the node to the parent but not to the archetype root
            //       and all parent nodes, like the OPT parser does with the setFlatNodes(),
            //       which generated an incosistent behavior.
            //
            // Add nodes to the current ObjectNode

            //if (!obn.nodes[obnc.templatePath]) obn.nodes[obnc.templatePath] = []
            //obn.nodes[obnc.templatePath] << obnc

            // The key for these nodes should be the archetype path not the template path
            //if (!obn.nodes[obnc.path]) obn.nodes[obnc.path] = []
            //obn.nodes[obnc.path] << obnc

/*
            // This while assigns the current generated node to all it's ascedant nodes,
            // in their flat list, so when getting any of those nodes, the new injected
            // node will be there and can be retrieved by it's path.
            parent_obn = obn
            while (parent_obn)
            {
               if (!parent_obn.nodes[obnc.path]) parent_obn.nodes[obnc.path] = []
               parent_obn.nodes[obnc.path] << obnc

               parent_obn = parent_obn?.parent?.parent
            }
*/

            // TODO: info log
            //println "adding new node ${obnc.templatePath} to node ${obn.templatePath}"
         }
      }

      // recursion
      obn.attributes.each { atn ->
         completeRecursive(atn)
      }
   }
   private completeRecursive(AttributeNode atn)
   {
      atn.children.each { obn ->
         completeRecursive(obn)
      }
   }

}
