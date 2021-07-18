package com.cabolabs.openehr.rm_1_0_2.data_types.encapsulated

import com.cabolabs.openehr.rm_1_0_2.data_types.basic.DataValue
import com.cabolabs.openehr.rm_1_0_2.data_types.text.CodePhrase
import com.cabolabs.openehr.rm_1_0_2.data_types.uri.DvUri

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
class DvMultimedia extends DvEncapsulated {

   String alternate_text
   DvUri uri
   byte[] data
   CodePhrase media_type
   CodePhrase compression_algorithm
   byte[] integrity_check
   CodePhrase integrity_check_algorithm
   int size
   DvMultimedia thumbnail

   // TODO: add a method to get the decoded data (it is stored encoded)

}
