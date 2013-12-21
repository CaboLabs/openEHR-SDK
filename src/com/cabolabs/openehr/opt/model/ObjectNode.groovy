package com.cabolabs.openehr.opt.model

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
}