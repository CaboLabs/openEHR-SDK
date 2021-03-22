package com.cabolabs.openehr.rm_1_0_2.common.generic

import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvText
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvCodedText
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime

class AuditDetails {

   String system_id
   DvDateTime time_committed
   DvCodedText change_type
   DvText description
   PartyProxy committer
}