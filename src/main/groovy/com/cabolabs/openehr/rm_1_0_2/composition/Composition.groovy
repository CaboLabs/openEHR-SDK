package com.cabolabs.openehr.rm_1_0_2.composition

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable
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
   List<ContentItem> content = []
   EventContext context

   @Override
   void fillPathable(Pathable parent, String parentAttribute)
   {
      this.path = '/'
      this.dataPath = '/'
      // parent should be null

      if (this.context)
      {
         this.context.fillPathable(this, 'context')
      }

      this.content.eachWithIndex { content_item, i ->
         content_item.fillPathable(this, "content[$i]")
      }
   }
}
