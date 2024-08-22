package com.cabolabs.openehr.opt.model

import java.util.Map
import groovy.util.slurpersupport.GPathResult

@groovy.util.logging.Slf4j
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
   // Map<String, List<ObjectNode>>
   Map nodes = [:] // path -> list of alternative constraints with the same path

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
   List<Constraint> getNodes(String path)
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

   // returns the constraints for the attribute with name
   AttributeNode getAttr(String name)
   {
      this.attributes.find { it.rmAttributeName == name }
   }

   // Returns the own archetypeId if this is a root node, or gets the nearest root node archetypeId from an ancestor.
   String getOwnerArchetypeId()
   {
      def ancestor = getOwnerArchetypeRoot()
      return ancestor?.archetypeId
   }

   ObjectNode getOwnerArchetypeRoot()
   {
      if (this.archetypeId) return this

      def ancestor = this.parent.parent // parent is an attribute, parent.parent is an object
      while (!ancestor.archetypeId)
      {
         ancestor = ancestor.parent.parent
      }

      return ancestor
   }
}
