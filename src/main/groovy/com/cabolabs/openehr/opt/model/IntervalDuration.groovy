package com.cabolabs.openehr.opt.model

import com.cabolabs.openehr.opt.model.datatypes.Duration

class IntervalDuration {

   Boolean lowerIncluded
   Boolean upperIncluded
   Boolean lowerUnbounded
   Boolean upperUnbounded
   Duration lower
   Duration upper

   boolean has(Duration d)
   {
      def i = d.seconds()

      def ll = lowerUnbounded ? Long.MIN_VALUE : lower.seconds()
      def hh = upperUnbounded ? Long.MAX_VALUE : upper.seconds()

      boolean has = false

      if (lowerIncluded)
      {
         if (upperIncluded)
         {
            has = ll <= i && i <= hh
         }
         else
         {
            has = ll <= i && i < hh
         }
      }
      else
      {
         if (upperIncluded)
         {
            has = ll < i && i <= hh
         }
         else
         {
            has = ll < i && i < hh
         }
      }

      return has
   }

   String toString()
   {
      String s = ""

      s += (lowerUnbounded ? '*' : lower.toString())

      s += '..'

      s += (upperUnbounded ? '*' : upper.toString())

      return s
   }
}
