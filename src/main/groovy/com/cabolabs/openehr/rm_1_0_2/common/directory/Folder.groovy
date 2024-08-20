package com.cabolabs.openehr.rm_1_0_2.common.directory

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable
import com.cabolabs.openehr.rm_1_0_2.support.identification.ObjectRef

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
class Folder extends Locatable {

   List<ObjectRef> items = []
   List<Folder> folders = [] // children, with inherited parent will point to the parent folder as back link

   @Override
   void fillPathable(Pathable parent, String parentAttribute)
   {
      this.path = ((parent && parent.path != '/') ? '/' : '') + parentAttribute.replaceAll(/\(\d+\)/, '')
      this.dataPath = ((parent && parent.dataPath != '/') ? '/' : '') + parentAttribute
      this.parent = parent

      this.folders.eachWithIndex { folder, i ->
         folder.fillPathable(this, "folders($i)")
      }
   }

   // getter with initializer
   List<Folder> getFolders()
   {
      if (folders == null) folders = []
      folders
   }

   // getter with initializer
   List<ObjectRef> getItems()
   {
      if (items == null) items = []
      items
   }
}