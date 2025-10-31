package com.cabolabs.openehr.dto_1_0_2.demographic

import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemStructure
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable
import com.cabolabs.openehr.rm_1_0_2.demographic.PartyIdentity
import com.cabolabs.openehr.rm_1_0_2.demographic.Contact

import com.cabolabs.openehr.rm_1_0_2.support.identification.PartyRef

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
abstract class PartyDto extends Locatable {

   ItemStructure details
   List<Contact> contacts = [] // Contact
   List<PartyIdentity> identities = [] // PartyIdentity

   @Override
   void fillPathable(Pathable parent, String parentAttribute)
   {
      this.path = ((parent.path != '/') ? '/' : '') + parentAttribute.replaceAll(/\[\d+\]/, '')
      this.dataPath = ((parent.dataPath != '/') ? '/' : '') + parentAttribute
      this.parent = parent

      // TODO: parent fillPathable
      this.details.fillPathable(this, "details")
   }

   // Helps transforming DTO objects into RM objects
   PartyRef toPartyRef()
   {
      PartyRef pr = new PartyRef()
      pr.namespace = 'demographic' // TODO: this might be configurable
      pr.type = this.getRmType() // PartyRelationshipDto => PARTY_RELATIONSHIP
      pr.id = this.uid?.clone() // same OBJECT_VERSION_ID or HIER_OBJECT_ID
      return pr
   }
}
