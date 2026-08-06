package com.cabolabs.openehr.opt.diff

import com.cabolabs.openehr.opt.model.ObjectNode

// diff of a matched pair of ObjectNode (or a whole-subtree added/removed node).
// Fields needed to render a tree UI node (templatePath, nodeId, rmTypeName, type, name, status)
// are denormalized here so a renderer never needs to dereference node1/node2.
class SemanticNodeDiff {
   String templatePath
   String nodeId
   String rmTypeName
   String type // AOM type
   String name // resolved text
   String status // same, added, removed, modified

   // true if this node or any descendant carries a 'certain' BreakingChange (see BreakingChange)
   boolean breaking = false

   List<FieldChange> fieldChanges = []
   List<ListChange> listChanges = []

   // rmAttributeName -> AttributeDiff
   Map<String, AttributeDiff> attributes = [:]

   // kept for advanced/debug use, not required to render the tree
   ObjectNode node1
   ObjectNode node2
}
