package com.cabolabs.openehr.rm_1_0_2.common.generic

import com.cabolabs.openehr.rm_1_0_2.data_types.encapsulated.DvMultimedia
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvText
import com.cabolabs.openehr.rm_1_0_2.data_types.uri.DvEhrUri

class Attestation extends AuditDetails {

   DvMultimedia attested_view
   String proof
   Set<DvEhrUri> items
   DvText reason
   Boolean is_pending
}
