package com.cabolabs.openehr.rm_1_0_2.common.generic

import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.DvInterval
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvCodedText
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvText

class Participation {
   
   DvText function
   DvInterval time
   DvCodedText mode
   PartyProxy performer
}
