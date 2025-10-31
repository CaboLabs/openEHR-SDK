package com.cabolabs.openehr.rm_1_0_2.common.archetyped

import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvText
import com.cabolabs.openehr.rm_1_0_2.support.identification.UIDBasedId

abstract class Locatable extends Pathable {

   // TODO: standardize fields to camelCase
   Archetyped archetype_details

   DvText name
   String archetype_node_id
   UIDBasedId uid

   boolean is_archetype_root()
   {
      !atchetype_node_id.startsWith('at')
   }

   /**
    * From / To:
    * Composition / COMPOSITION
    * EhrStatus / EHR_STATUS
    * PartyRelationship / PARTY_RELATIONSHIP
    * PartyRelationshipDto / PARTY_RELATIONSHIP
    */
   String getRmType()
   {
      (this.class.simpleName - 'Dto').replaceAll( /([A-Z])/, /_$1/ ).toUpperCase().replaceAll( /^_/, '' )
   }
}
