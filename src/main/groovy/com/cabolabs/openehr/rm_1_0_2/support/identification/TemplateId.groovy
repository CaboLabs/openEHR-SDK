package com.cabolabs.openehr.rm_1_0_2.support.identification

class TemplateId extends ObjectId {

   // Prevents groovy.lang.GroovyRuntimeException: Could not find named-arg compatible constructor with the second constructor
   TemplateId()
   {

   }

   TemplateId(String value)
   {
      this.value = value
   }
}
