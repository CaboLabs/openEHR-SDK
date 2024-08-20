package com.cabolabs.openehr.rm_1_0_2.common.change_control

import com.cabolabs.openehr.rm_1_0_2.support.identification.ObjectVersionId
import com.cabolabs.openehr.rm_1_0_2.support.identification.ObjectRef
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvCodedText
import com.cabolabs.openehr.rm_1_0_2.common.generic.Attestation
import com.cabolabs.openehr.rm_1_0_2.common.generic.AuditDetails

import com.cabolabs.openehr.rm_1_0_2.support.identification.HierObjectId

class OriginalVersion extends Version {

   List<Attestation> attestations = []


   HierObjectId owner_id()
   {
      // TODO
   }

   Boolean is_branch()
   {
      // TODO
   }

   String canonical_form()
   {
      // TODO
   }

   /**
    * True if this Version was created from more than just the preceding (checked out) version.
    */
   Boolean is_merged()
   {
      false
   }

   // getter with initializer
   List<Attestation> getAttestations()
   {
      if (attestations == null) attestations = []
      attestations
   }
}