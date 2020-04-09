package com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time

import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.DvAmount

class DvDuration extends DvAmount {

   String value

   @Override
   int compareTo(Object o)
   {
      // TODO: compare duration expressions, need to parse!
      return 0
   }

   @Override
   Number getMagnitude()
   {
      // TODO: calculate the magnitude in seconds
      return 0
   }

   @Override
   DvAmount negative()
   {
      return null
   }

   @Override
   DvAmount plus(DvAmount e)
   {
      return null
   }

   @Override
   DvAmount minus(DvAmount e)
   {
      return null
   }


}
