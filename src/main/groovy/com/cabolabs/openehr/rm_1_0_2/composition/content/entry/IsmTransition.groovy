package com.cabolabs.openehr.rm_1_0_2.composition.content.entry

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvCodedText

class IsmTransition extends Pathable {
   
   DvCodedText current_state
   DvCodedText transition
   DvCodedText careflow_step
}
