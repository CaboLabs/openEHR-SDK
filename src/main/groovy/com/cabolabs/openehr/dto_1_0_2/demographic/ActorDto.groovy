package com.cabolabs.openehr.dto_1_0_2.demographic

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
abstract class ActorDto extends PartyDto {

   Set roles = [] // Role in the DTO, this is PARTY_REF in the RM
   List languages = [] // DvText
}
