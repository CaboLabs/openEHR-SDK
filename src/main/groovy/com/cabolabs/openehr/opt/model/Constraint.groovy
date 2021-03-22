package com.cabolabs.openehr.opt.model

abstract class Constraint {

   // Calculated path of this node during parsing
   String templatePath // absolute path inside the template
   String path // relative path to the root archetype node
   String templateDataPath
   String dataPath // relative to the root archetype, as it is on data, e.g. PATHABLES will not have node_id as they might have on the OPT and archetype
}
