package com.cabolabs.openehr.opt.model.domain

import com.cabolabs.openehr.opt.model.datatypes.CodePhrase

class CDvOrdinalItem {

   int value
   CodePhrase symbol // in the model this is DV_CODED_TEXT but the value is not used, so CodePhrase is enough

   String toString()
   {
      return "CDvOrdinalItem (${value}) "+ symbol.terminologyId +"::"+ symbol.codeString
   }
}
