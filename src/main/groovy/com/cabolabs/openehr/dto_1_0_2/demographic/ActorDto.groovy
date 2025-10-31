package com.cabolabs.openehr.dto_1_0_2.demographic

import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvText
import com.cabolabs.openehr.rm_1_0_2.demographic.Actor

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
abstract class ActorDto extends PartyDto {

   Set<RoleDto> roles = [] // RoleDto in the DTO, this is PARTY_REF in the RM
   List<DvText> languages = [] // DvText

   /**
    * Converts this DTO to its corresponding RM class. Note the resulting object
    * shares references to the same fields as this object, if you modify the source
    * object, that will modify the references.
    */
   Actor toActor()
   {
      Actor actor = createActor() // implemented in subclasses
      populateCommonFields(actor) // copy common actor fields
      return actor
   }

   // NOTE: this makes the source and target share references to fields, so it's not a deep copy
   protected void populateCommonFields(Actor actor)
   {
      // party
      actor.details           = this.details
      actor.contacts          = this.contacts
      actor.identities        = this.identities
      // NOTE: PartyDto doesn't have relationships, but the RM does.

      // actor
      actor.languages         = this.languages

      this.roles.each { roleDto ->
         actor.roles.add(roleDto.toPartyRef())
      }

      // locatable
      actor.uid               = this.uid
      actor.name              = this.name
      actor.archetype_node_id = this.archetype_node_id
      actor.archetype_details = this.archetype_details

   }

   protected abstract Actor createActor()
}
