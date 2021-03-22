package com.cabolabs.openehr.rm_1_0_2.data_types.quantity

class DvProportion extends DvAmount {

   Float numerator
   Float denominator
   Integer type
   Integer precision = -1

   @Override
   int compareTo(Object o)
   {
      // TODO
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
