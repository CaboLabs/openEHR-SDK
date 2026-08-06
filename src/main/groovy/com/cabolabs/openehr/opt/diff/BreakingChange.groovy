package com.cabolabs.openehr.opt.diff

// a constraint change that can invalidate data or queries built against the 'compared' (old) OPT
// when validated/run against the 'to' (new) OPT.
//
// certain = true:  the change alone makes some previously-valid data invalid
//                   (narrowed occurrences/existence/cardinality/range, a value removed from a
//                   coded/ordinal/list constraint, or a new mandatory node/attribute added)
// certain = false: the change only *might* be breaking and needs an external check
//                   (node/attribute removed - fine for stored data, but any stored query
//                   referencing that path is now invalid against the new OPT)
class BreakingChange {
   String templatePath
   String category   // occurrences_narrowed, existence_narrowed, cardinality_narrowed,
                      // range_narrowed, value_removed, node_added_mandatory,
                      // attribute_added_mandatory, node_removed, attribute_removed
   String field
   def oldValue
   def newValue
   String reason
   boolean certain
}
