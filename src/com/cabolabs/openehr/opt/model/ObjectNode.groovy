package com.cabolabs.openehr.opt.model

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
   String path
   
   // TODO: this can be a list on the OPT but since
   // the Template Designer doesnt allow more than one,
   // we support just one value.
   String terminologyRef
   
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
}
