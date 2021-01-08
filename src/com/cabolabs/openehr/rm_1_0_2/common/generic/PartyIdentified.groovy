package com.cabolabs.openehr.rm_1_0_2.common.generic

import com.cabolabs.openehr.rm_1_0_2.data_types.basic.DvIdentifier

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
class PartyIdentified extends PartyProxy {
   
   String name
   List<DvIdentifier> identifiers
}
