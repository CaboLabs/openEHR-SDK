package com.cabolabs.openehr.opt.diff

import com.cabolabs.openehr.opt.model.ObjectNode

// helps on the result of the OPT diff algorithm
class NodeDiff {

   String templateDataPath
   String compareResult // same, added, removed

   ObjectNode optNode // reference to the OPT node associated to this diff
   
   // attribute name => node diffs
   Map attributeDiffs = [:]
}
