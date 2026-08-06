package com.cabolabs.openehr.opt.diff

// diff of a matched pair of AttributeNode (or a whole-subtree added/removed attribute)
class AttributeDiff {
   String rmAttributeName
   String status // same, added, removed, modified

   // true if this attribute or any descendant carries a 'certain' BreakingChange (see BreakingChange)
   boolean breaking = false

   List<FieldChange> fieldChanges = [] // cardinality.*, existence

   List<SemanticNodeDiff> children = []
}
