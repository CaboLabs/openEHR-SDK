package com.cabolabs.openehr.opt.diff

// helps on the result of the OPT diff algorithm
class NodeDiff {

   String templateDataPath
   String compareResult // same, added, removed
   
   // attribute name => node diffs
   Map attributeDiffs = [:]
}
