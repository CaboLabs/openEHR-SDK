package com.cabolabs.openehr.dto_1_0_2.demographic

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.DvInterval
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemStructure
import com.cabolabs.openehr.rm_1_0_2.demographic.PartyRelationship

class PartyRelationshipDto extends Locatable {

   ItemStructure details
   DvInterval time_validity // DvDate
   PartyDto source
   PartyDto target

   @Override
   void fillPathable(Pathable parent, String parentAttribute)
   {
      this.path = ((parent.path != '/') ? '/' : '') + parentAttribute.replaceAll(/\[\d+\]/, '')
      this.dataPath = ((parent.dataPath != '/') ? '/' : '') + parentAttribute
      this.parent = parent

      this.details.fillPathable(this, "details")
   }

   PartyRelationship toPartyRelationship()
   {
      def pr = new PartyRelationship()

      // party relationship
      pr.details = this.details
      pr.time_validity = this.time_validity
      pr.source = this.source?.toPartyRef()
      pr.target = this.target?.toPartyRef()

      // locatable
      pr.uid = this.uid
      pr.archetype_node_id = this.archetype_node_id
      pr.name = this.name

      pr.archetype_details = this.archetype_details
      // pr.feederAudit = this.feederAudit
      // pr.links = this.links.collect { it } // shallow copy

      return pr
   }
}