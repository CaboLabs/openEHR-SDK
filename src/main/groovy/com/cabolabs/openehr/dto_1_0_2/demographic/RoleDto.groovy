package com.cabolabs.openehr.dto_1_0_2.demographic

import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.DvInterval
import com.cabolabs.openehr.rm_1_0_2.demographic.Capability
import com.cabolabs.openehr.rm_1_0_2.support.identification.PartyRef

/**
 * @author pablo.pazos@cabolabs.com
 * NOTE: this is actually the same as Role. It was a test to check if performer could be ActorDto but we couldn't do it because we didn't have the full actor at the moment of creating the RoleDto via the REST API.
 */
class RoleDto extends PartyDto {

   DvInterval time_validity      // DvDate
   PartyRef performer            // Reference to the actor performer
   List<Capability> capabilities // Capability

   PartyRef toPartyRef()
   {
      PartyRef pr = new PartyRef()
      pr.namespace = 'demographic'
      pr.type = 'ROLE'
      pr.id = this.id.clone()
      return pr
   }
}
