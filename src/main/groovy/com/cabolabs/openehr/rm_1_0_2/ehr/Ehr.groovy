/**
 * 
 */
package com.cabolabs.openehr.rm_1_0_2.ehr

import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime
import com.cabolabs.openehr.rm_1_0_2.support.identification.HierObjectId
import com.cabolabs.openehr.rm_1_0_2.support.identification.ObjectRef

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
class Ehr {

   HierObjectId system_id
   HierObjectId ehr_id
   DvDateTime time_created
   ObjectRef ehr_status
   ObjectRef ehr_access
   List<ObjectRef> compositions
   ObjectRef directory
   List<ObjectRef> contributions
}
