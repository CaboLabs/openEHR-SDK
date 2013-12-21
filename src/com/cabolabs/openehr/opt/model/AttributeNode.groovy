package com.cabolabs.openehr.opt.model

class AttributeNode {

   // AOM type
   String type
   String rmAttributeName
   
   // List<ObjectNode>
   List children = []
   
   Cardinality cardinality
   
   Interval existence
}