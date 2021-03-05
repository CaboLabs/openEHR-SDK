package com.cabolabs.openehr.rm_1_0_2.composition.content.entry

import com.cabolabs.openehr.rm_1_0_2.data_types.encapsulated.DvParsable
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvText

class Instruction extends CareEntry {
   
   DvText narrative
   DvDateTime expiry_time
   DvParsable wf_definition
   List<Activity> activities = []
}
