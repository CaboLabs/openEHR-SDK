package com.cabolabs.openehr.opt.model

class IntervalDouble {

   Boolean lowerIncluded
   Boolean upperIncluded
   Boolean lowerUnbounded
   Boolean upperUnbounded
   Double lower
   Double upper

   boolean has(Double i)
   {
      def ll = (lowerUnbounded ? -Double.MAX_VALUE : lower)
      def hh = (upperUnbounded ? Double.MAX_VALUE : upper)

      if (lowerIncluded)
      {
         if (upperIncluded)
         {
            return ll <= i && i <= hh
         }
         else
         {
            return ll <= i && i < hh
         }
      }
      else
      {
         if (upperIncluded)
         {
            return ll < i && i <= hh
         }
         else
         {
            return ll < i && i < hh
         }
      }
   }

   String toString()
   {
      String s = ""

      s += (lowerUnbounded ? '*' : lower.toString())

      s += '..'

      s += (upperUnbounded ? '*' : upper.toString())

      return s
   }

   // true if the interval is *..*
   boolean anyAllowed()
   {
      (lowerUnbounded && upperUnbounded)
   }
}
