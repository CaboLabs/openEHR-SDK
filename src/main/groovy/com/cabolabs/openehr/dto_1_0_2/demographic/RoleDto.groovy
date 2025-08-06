package com.cabolabs.openehr.dto_1_0_2.demographic

import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.DvInterval
import com.cabolabs.openehr.rm_1_0_2.demographic.Capability

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
class RoleDto extends PartyDto {

   DvInterval time_validity      // DvDate
   ActorDto performer            // direct association with ActorDto instead of PartyRef
   List<Capability> capabilities // Capability
}
