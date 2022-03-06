package com.cabolabs.openehr.dto_1_0_2.common.change_control

import com.cabolabs.openehr.rm_1_0_2.common.change_control.Version
import com.cabolabs.openehr.rm_1_0_2.support.identification.HierObjectId
import com.cabolabs.openehr.rm_1_0_2.support.identification.ObjectRef
import com.cabolabs.openehr.rm_1_0_2.common.generic.AuditDetails


/**
 * Repreesnts the Contributon DTO used in the openEHR API. The Contribution has a versions attribute which is a list of OBJECT_REF,
 * the DTO has the list of VERSION. Also, in the DTO the uid is optional.
 */
class ContributionDto {

   HierObjectId uid
   List<Version> versions
   AuditDetails audit
}