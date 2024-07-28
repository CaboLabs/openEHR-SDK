package com.cabolabs.openehr.opt.model

@groovy.util.logging.Log4j2
class AttributeNode extends Constraint {

   // AOM type
   String type
   String rmAttributeName

   ObjectNode parent

   // List<ObjectNode>
   List children = []

   Cardinality cardinality

   IntervalInt existence
}
