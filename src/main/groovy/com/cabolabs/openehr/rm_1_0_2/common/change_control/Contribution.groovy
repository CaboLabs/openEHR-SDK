package com.cabolabs.openehr.rm_1_0_2.common.change_control

import com.cabolabs.openehr.rm_1_0_2.support.identification.HierObjectId
import com.cabolabs.openehr.rm_1_0_2.support.identification.ObjectRef
import com.cabolabs.openehr.rm_1_0_2.common.generic.AuditDetails

class Contribution {

   HierObjectId uid
   HashSet<ObjectRef> versions
   AuditDetails audit
}