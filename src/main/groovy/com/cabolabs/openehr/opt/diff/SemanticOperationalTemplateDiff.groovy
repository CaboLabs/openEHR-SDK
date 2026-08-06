package com.cabolabs.openehr.opt.diff

import com.cabolabs.openehr.opt.model.OperationalTemplate

class SemanticOperationalTemplateDiff {
   OperationalTemplate compared // OPT 1
   OperationalTemplate to       // OPT 2

   List<FieldChange> templateMetadataChanges = [] // templateId, concept, language, purpose, isControlled

   SemanticNodeDiff root

   // flat list of every breaking (or possibly-breaking) constraint change found anywhere in the
   // tree, see BreakingChange for what 'certain' means
   List<BreakingChange> breakingChanges = []
}
