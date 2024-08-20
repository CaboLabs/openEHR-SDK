package com.cabolabs.openehr.rm_1_0_2.composition.content.entry

import com.cabolabs.openehr.rm_1_0_2.common.generic.Participation
import com.cabolabs.openehr.rm_1_0_2.common.generic.PartyProxy
import com.cabolabs.openehr.rm_1_0_2.common.generic.PartySelf
import com.cabolabs.openehr.rm_1_0_2.composition.content.ContentItem
import com.cabolabs.openehr.rm_1_0_2.data_types.text.CodePhrase
import com.cabolabs.openehr.rm_1_0_2.support.identification.ObjectRef

abstract class Entry extends ContentItem {

   CodePhrase language
   CodePhrase encoding
   PartyProxy subject
   PartyProxy provider
   List<Participation> otherParticipations = []
   ObjectRef workflowId

   Boolean subject_is_self()
   {
      this.subject instanceof PartySelf
   }

   // getter with initializer
   List<Participation> getOtherParticipations()
   {
      if (otherParticipations == null) otherParticipations = []
      otherParticipations
   }
}
