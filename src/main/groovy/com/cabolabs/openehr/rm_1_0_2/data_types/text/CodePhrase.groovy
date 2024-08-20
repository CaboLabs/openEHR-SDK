package com.cabolabs.openehr.rm_1_0_2.data_types.text

import com.cabolabs.openehr.rm_1_0_2.support.identification.TerminologyId

class CodePhrase {

   TerminologyId terminologyId // terminology_id
   String codeString // code_string

   // Prevents groovy.lang.GroovyRuntimeException: Could not find named-arg compatible constructor with the second constructor
   CodePhrase()
   {

   }

   CodePhrase(TerminologyId terminologyId, String codeString)
   {
      this.terminologyId = terminologyId
      this.codeString = codeString
   }
}
