package com.cabolabs.openehr.rm_1_0_2.composition

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.rm_1_0_2.common.generic.PartyProxy
import com.cabolabs.openehr.rm_1_0_2.composition.content.ContentItem
import com.cabolabs.openehr.rm_1_0_2.data_types.text.CodePhrase
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvCodedText

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
class Composition extends Locatable {
   
   CodePhrase language
   CodePhrase territory
   DvCodedText category
   PartyProxy composer
   List<ContentItem> content
   EventContext context
   
}
