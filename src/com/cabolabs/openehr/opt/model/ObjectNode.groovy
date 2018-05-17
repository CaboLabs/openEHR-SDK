package com.cabolabs.openehr.opt.model

import java.util.Map
import groovy.util.slurpersupport.GPathResult

@groovy.util.logging.Log4j
class ObjectNode {

   OperationalTemplate owner

   // For root and resolved slots
   String archetypeId

   // AOM type
   String type

   // atNNNN
   String nodeId

   String rmTypeName

   // Null if the object is the root
   AttributeNode parent

   // List<AttributeNode>
   List attributes = []

   // List<CodedTerm>
   List termDefinitions = []

   IntervalInt occurrences

   // CONSTRAINT_REF
   //String reference // this is really just for C_CODE_PHRASE, moved there

   // Calculated path of this node during parsing
   String templatePath // absolute path inside the template
   String path // relative path to the root archetype node

   // This is now on CCodePhrase
   // TODO: this can be a list on the OPT but since
   // the Template Designer doesnt allow more than one,
   // we support just one value.
   //String terminologyRef

   // Plain structure of subnodes of this ObjectNode
   Map nodes = [:] // path -> ObjectNode (node) para pedir restricciones

   // TODO: constraints by type
   //
   // e.g. C_DV_QUANTITY has:
   // - property
   // - list *
   //   - magnitude
   //     - lower
   //     - upper
   //   - units

   // For now I just save the XML node to extract the constraints
   //GPathResult xmlNode

   /*
    * gets a node by archetype path
    */
   ObjectNode getNode(String path)
   {
      return this.nodes[path]
   }

   /*
    * get term
    */
   String getText(String code)
   {
      def codedTerm = this.termDefinitions.find{ it.code == code }

      if (!codedTerm)
      {
         log.info( "codedTerm not found "+ this.archetypeId +" "+ code )
         return
      }
      else
      {
         log.info( "codedTerm found "+ codedTerm.code +" "+ codedTerm.term.text )
      }

      return codedTerm.term.text
   }

   /*
    * get description
    */
   String getDescription(String code)
   {
      def codedTerm = this.termDefinitions.find{ it.code == code }

      if (!codedTerm)
      {
         log.info( "codedTerm not found "+ this.archetypeId +" "+ code )
         return
      }
      else
      {
         log.info( "codedTerm found "+ codedTerm.code +" "+ codedTerm.term.text )
      }

      return codedTerm.term.description
   }
}
