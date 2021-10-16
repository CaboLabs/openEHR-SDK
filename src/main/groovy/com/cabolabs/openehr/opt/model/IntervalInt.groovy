package com.cabolabs.openehr.opt.model

import groovy.transform.AutoClone

@AutoClone
class IntervalInt {

   Boolean lowerIncluded
   Boolean upperIncluded
   Boolean lowerUnbounded
   Boolean upperUnbounded
   Integer lower
   Integer upper

   boolean has(Integer i)
   {
      def ll = (lowerUnbounded ? Integer.MIN_VALUE : (lowerIncluded ? lower : lower+1))
      def hh = (upperUnbounded ? Integer.MAX_VALUE : (upperIncluded ? upper : upper-1))

      return ll <= i && i <= hh
   }

   String toString()
   {
      String s = ""

      s += (lowerUnbounded ? '*' : lower.toString())

      s += '..'

      s += (upperUnbounded ? '*' : upper.toString())

      return s
   }

   // true if the interval is 0..*
   boolean anyAllowed()
   {
      (lower != null && lower == 0 && upperUnbounded)
   }
}
