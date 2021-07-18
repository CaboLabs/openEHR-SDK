package com.cabolabs.openehr.opt.model

class IntervalFloat {

   Boolean lowerIncluded
   Boolean upperIncluded
   Boolean lowerUnbounded
   Boolean upperUnbounded
   Float lower
   Float upper

   boolean has(float i)
   {
      def ll = (lowerUnbounded ? -Float.MAX_VALUE : lower)
      def hh = (upperUnbounded ? Float.MAX_VALUE : upper)

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
}
