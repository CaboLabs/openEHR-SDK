package com.cabolabs.openehr.opt.model

abstract class Constraint {

   // Calculated path of this node during parsing
   String templatePath // absolute path inside the template
   String path // relative path to the root archetype node
}
