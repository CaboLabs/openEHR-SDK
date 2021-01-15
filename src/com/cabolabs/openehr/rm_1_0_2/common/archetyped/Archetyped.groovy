package com.cabolabs.openehr.rm_1_0_2.common.archetyped

import com.cabolabs.openehr.rm_1_0_2.support.identification.ArchetypeId
import com.cabolabs.openehr.rm_1_0_2.support.identification.TemplateId

class Archetyped {

   ArchetypeId archetype_id
   TemplateId template_id
   String rm_version = '1.0.2' // hardcoded because this is 1.0.2 impl
}
