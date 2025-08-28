package com.cabolabs.openehr.dto_1_0_2.demographic

import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvText

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
abstract class ActorDto extends PartyDto {

   Set<RoleDto> roles = [] // RoleDto in the DTO, this is PARTY_REF in the RM
   List<DvText> languages = [] // DvText
}
