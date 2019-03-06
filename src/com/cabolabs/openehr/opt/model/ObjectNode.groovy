package com.cabolabs.openehr.opt.model

import java.util.Map
import groovy.util.slurpersupport.GPathResult

@groovy.util.logging.Log4j
class ObjectNode extends Constraint {

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

   // resolved values needed for structure only data when the API is not available, like serializing this to JSON
   String text
   String description

   // Plain structure of subnodes of this ObjectNode
   Map nodes = [:] // path -> Constraint (node) para pedir restricciones

   // TODO: constraints by type
   //
   // e.g. C_DV_QUANTITY has:
   // - property
   // - list *
   //   - magnitude
   //     - lower
   //     - upper
   //   - units

   /*
    * gets a node by archetype path
    */
   Constraint getNode(String path)
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
