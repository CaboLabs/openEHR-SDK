package com.cabolabs.openehr.opt.diff

// a constraint change that can invalidate data or queries built against the 'compared' (old) OPT
// when validated/run against the 'to' (new) OPT.
//
// certain = true:  the change alone makes some previously-valid data invalid
//                   (narrowed occurrences/existence/cardinality, a range or a value removed from a
//                   constraint the old OPT had, or a new mandatory node/attribute added)
// certain = false: the change only *might* be breaking and needs an external check, that can be automated:
//                   - a constraint over a value that the old OPT didn't constrain, so the RM rules
//                     applied (any value of its type, or the values of an RM enumeration), it breaks
//                     only the data that has values outside of the new constraint (constraint_added,
//                     or a range_narrowed from an unconstrained value)
//                   - node/attribute removed - fine for stored data, but any stored query
//                     referencing that path is now invalid against the new OPT
class BreakingChange {
   String templatePath
   String category   // occurrences_narrowed, existence_narrowed, cardinality_narrowed,
                      // range_narrowed, constraint_added, value_removed, node_added_mandatory,
                      // attribute_added_mandatory, node_removed, attribute_removed
   String field
   def oldValue
   def newValue
   String reason
   boolean certain
}
