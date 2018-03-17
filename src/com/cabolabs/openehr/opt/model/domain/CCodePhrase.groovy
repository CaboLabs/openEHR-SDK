package com.cabolabs.openehr.opt.model.domain

import com.cabolabs.openehr.opt.model.ObjectNode

@groovy.util.logging.Log4j
class CCodePhrase extends ObjectNode {

   // CODE LIST CONSTRAINT

   // List<String>
   List codeList = []
   String terminologyIdName
   String terminologyIdVersion // optional


   // REFERENCE SET URI CONSTRAINT

   // TODO: this can be a list on the OPT but since
   // the Template Designer doesnt allow more than one,
   // we support just one value.
   String terminologyRef
}
