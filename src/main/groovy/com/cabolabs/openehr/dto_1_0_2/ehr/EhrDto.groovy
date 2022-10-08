/**
 * 
 */
package com.cabolabs.openehr.dto_1_0_2.ehr

import com.cabolabs.openehr.rm_1_0_2.ehr.EhrStatus
import com.cabolabs.openehr.rm_1_0_2.common.directory.Folder
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import com.cabolabs.openehr.rm_1_0_2.support.identification.HierObjectId
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime
import com.cabolabs.openehr.dto_1_0_2.common.change_control.ContributionDto


/**
 * @author pablo.pazos@cabolabs.com
 *
 */
class EhrDto {

   HierObjectId system_id
   HierObjectId ehr_id
   DvDateTime time_created
   EhrStatus ehr_status
   // TODO: ehr_access
   List<Composition> compositions
   Folder directory
   List<ContributionDto> contributions
}
