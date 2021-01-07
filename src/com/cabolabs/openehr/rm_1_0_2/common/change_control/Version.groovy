package com.cabolabs.openehr.rm_1_0_2.common.change_control

import com.cabolabs.openehr.rm_1_0_2.support.identification.ObjectVersionId
import com.cabolabs.openehr.rm_1_0_2.support.identification.ObjectRef
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvCodedText
import com.cabolabs.openehr.rm_1_0_2.common.generic.AuditDetails
import com.cabolabs.openehr.rm_1_0_2.support.identification.HierObjectId
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable

abstract class Version {

   ObjectVersionId uid
   ObjectVersionId preceding_version_id

   Locatable data

   DvCodedText lifecycle_state

   AuditDetails commit_audit
   ObjectRef contribution
   String signature

   //abstract List<ObjectVersionId> other_input_version_uids() // ???

   abstract HierObjectId owner_id()
   abstract Boolean is_branch()
   abstract String canonical_form()

}