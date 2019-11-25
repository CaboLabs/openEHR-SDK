package com.cabolabs.openehr.rm_1_0_2.data_types.encapsulated

import com.cabolabs.openehr.rm_1_0_2.data_types.basic.DataValue
import com.cabolabs.openehr.rm_1_0_2.data_types.text.CodePhrase

class DvMultimedia extends DvEncapsulated {

   String alternate_text
   DvURI uri
   byte[] data
   CodePhrase media_type
   CodePhrase compression_algorithm
   byte[] integrity_check
   CodePhrase integrity_check_algorithm
   int size
   DvMultimedia thumbnail

}
