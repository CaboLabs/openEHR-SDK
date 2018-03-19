package com.cabolabs.openehr.opt.model.domain

import com.cabolabs.openehr.opt.model.datatypes.CodePhrase

@groovy.util.logging.Log4j
class CDvOrdinalItem {

   int value
   CodePhrase symbol // in the model this is DV_CODED_TEXT but the value is not used, so CodePhrase is enough
}
