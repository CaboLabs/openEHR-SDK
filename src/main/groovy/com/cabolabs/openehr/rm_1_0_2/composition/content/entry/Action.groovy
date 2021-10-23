package com.cabolabs.openehr.rm_1_0_2.composition.content.entry

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemStructure
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
class Action extends CareEntry {
   
   DvDateTime time
   ItemStructure description
   IsmTransition ism_transition
   InstructionDetails instruction_details

   @Override
   void fillPathable(Pathable parent, String parentAttribute)
   {
      this.path = ((parent.path != '/') ? '/' : '') + parentAttribute.replaceAll(/\[\d+\]/, '')
      this.dataPath = ((parent.dataPath != '/') ? '/' : '') + parentAttribute
      this.parent = parent

      this.description.fillPathable(this, 'description')
      if (this.ism_transition)
      {
         this.ism_transition.fillPathable(this, 'ism_transition')
      }
      if (this.instruction_details)
      {
         this.instruction_details.fillPathable(this, 'instruction_details')
      }
      if (this.protocol)
      {
         this.protocol.fillPathable(this, 'protocol')
      }
   }
}
