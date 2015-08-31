package com.cabolabs.openehr.opt.model

import java.util.Map;

import groovy.util.slurpersupport.GPathResult

class ObjectNode {

   // For root and resolved slots
   String archetypeId
   
   // AOM type
   String type
   
   // atNNNN
   String nodeId
   
   String rmTypeName
   
   // List<AttributeNode>
   List attributes = []
   
   // List<CodedTerm>
   List termDefinitions = []
   
   Interval occurrences
   
   // Calculated path of this node during parsing
   String templatePath // absolute path inside the template
   String path // relative path to the root archetype node
   
   // TODO: this can be a list on the OPT but since
   // the Template Designer doesnt allow more than one,
   // we support just one value.
   String terminologyRef
   
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
   GPathResult xmlNode
   
   /*
    * gets a node by archetype path
    */
   ObjectNode getNode(String path)
   {
      return this.nodes[path]
   }
}
