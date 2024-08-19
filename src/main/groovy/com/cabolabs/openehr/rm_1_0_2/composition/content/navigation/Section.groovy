package com.cabolabs.openehr.rm_1_0_2.composition.content.navigation

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable
import com.cabolabs.openehr.rm_1_0_2.composition.content.ContentItem

class Section extends ContentItem {

   List<ContentItem> items

   @Override
   void fillPathable(Pathable parent, String parentAttribute)
   {
      this.path = ((parent.path != '/') ? '/' : '') + parentAttribute.replaceAll(/\[\d+\]/, '')
      this.dataPath = ((parent.dataPath != '/') ? '/' : '') + parentAttribute
      this.parent = parent

      this.items.eachWithIndex { content_item, i ->
         content_item.fillPathable(this, "items[$i]")
      }
   }

   // getter with initializer
   List<ContentItem> getItems()
   {
      if (items == null) items = []
      items
   }
}
