package com.cabolabs.openehr.rm_1_0_2.common.archetyped

import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvText
import com.cabolabs.openehr.rm_1_0_2.support.identification.UIDBasedId

abstract class Locatable extends Pathable {

   Archetyped archetype_details

   DvText name
   String archetype_node_id
   UIDBasedId uid
}
