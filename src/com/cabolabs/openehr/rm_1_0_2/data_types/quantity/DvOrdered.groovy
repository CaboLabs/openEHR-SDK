package com.cabolabs.openehr.rm_1_0_2.data_types.quantity

import com.cabolabs.openehr.rm_1_0_2.data_types.basic.DataValue
import com.cabolabs.openehr.opt.model.datatypes.CodePhrase


class DvOrdered extends DataValue {

   CodePhrase normal_status
   DvInterval normal_range // DvInterval<DvOrdered>
   List other_reference_ranges // List<ReferenceRange<DvOrdered>>
}
