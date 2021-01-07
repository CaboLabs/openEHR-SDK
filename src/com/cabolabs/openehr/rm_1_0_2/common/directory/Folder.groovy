package com.cabolabs.openehr.rm_1_0_2.common.directory

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.rm_1_0_2.support.identification.ObjectRef

class Folder extends Locatable {

   List<ObjectRef> items
   List<Folder> folders // children, with inherited parent will point to the parent folder as back link
}