package com.cabolabs.openehr.rm_1_0_2.data_types.text

import com.cabolabs.openehr.rm_1_0_2.data_types.basic.DataValue

class DvText extends DataValue {

   String value
   List mappings = [] // TermMapping[]
   // TODO: hyperlink
   // TODO: formating
   // TODO: language
   // TODO: encoding

   // Prevents groovy.lang.GroovyRuntimeException: Could not find named-arg compatible constructor with the second constructor
   DvText()
   {

   }

   DvText(String value)
   {
      this.value = value
   }
}
