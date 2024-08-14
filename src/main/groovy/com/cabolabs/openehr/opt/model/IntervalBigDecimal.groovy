package com.cabolabs.openehr.opt.model

class IntervalBigDecimal {

   Boolean lowerIncluded
   Boolean upperIncluded
   Boolean lowerUnbounded
   Boolean upperUnbounded
   BigDecimal lower // FIXME: it would be better to use BigDecimal
   BigDecimal upper

   boolean has(BigDecimal i)
   {
      boolean hasLower = false

      if (lowerUnbounded)
      {
         hasLower = true
      }
      else
      {
         if (lowerIncluded)
         {
            hasLower = lower <= i
         }
         else
         {
            hasLower = lower < i
         }
      }

      boolean hasUpper = false

      if (upperUnbounded)
      {
         hasUpper = true
      }
      else
      {
         if (upperIncluded)
         {
            hasUpper = i <= upper
         }
         else
         {
            hasUpper = i < upper
         }
      }

      return hasLower && hasUpper
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
