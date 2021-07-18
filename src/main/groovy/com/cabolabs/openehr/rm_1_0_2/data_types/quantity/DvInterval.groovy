package com.cabolabs.openehr.rm_1_0_2.data_types.quantity

import com.cabolabs.openehr.rm_1_0_2.data_types.basic.DataValue

class DvInterval extends DataValue { // implements assumed Interval<T>

   DvOrdered lower
   DvOrdered upper

   // inherited from assumed Interval<T>
   Boolean lower_unbounded
   Boolean upper_unbounded
   Boolean lower_included
   Boolean upper_included

   boolean has(DvOrdered e)
   {
      boolean isin = true

      // check min
      if (!lower_unbounded)
      {
         if (lower_included)
         {
            // lower should be <= e
            if (lower > e) isin = false // TODO: this needs DvOrdered to implement int compareTo(Object o)
         }
         else
         {
            // lower should be < e
            if (lower >= e) isin = false
         }
      }

      // fails lower check
      if (!isin) return false

      // check max
      if (!upper_unbounded)
      {
         if (upper_included)
         {
            // e should be <= upper
            if (e > upper) isin = false
         }
         else
         {
            // e should be < upper
            if (e >= upper) isin = false
         }
      }

      return isin
   }
}
