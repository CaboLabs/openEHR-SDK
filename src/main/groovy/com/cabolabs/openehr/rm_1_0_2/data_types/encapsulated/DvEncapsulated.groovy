package com.cabolabs.openehr.rm_1_0_2.data_types.encapsulated

import com.cabolabs.openehr.rm_1_0_2.data_types.basic.DataValue
import com.cabolabs.openehr.rm_1_0_2.data_types.text.CodePhrase

abstract class DvEncapsulated extends DataValue {

   CodePhrase charset
   CodePhrase language
   int size
}
