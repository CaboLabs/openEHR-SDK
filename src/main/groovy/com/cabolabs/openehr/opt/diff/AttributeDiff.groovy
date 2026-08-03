package com.cabolabs.openehr.opt.diff

// diff of a matched pair of AttributeNode (or a whole-subtree added/removed attribute)
class AttributeDiff {
   String rmAttributeName
   String status // same, added, removed, modified

   List<FieldChange> fieldChanges = [] // cardinality.*, existence

   List<SemanticNodeDiff> children = []
}
