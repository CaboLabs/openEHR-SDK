package com.cabolabs.openehr.opt.model

import com.cabolabs.openehr.opt.model.datatypes.Duration

class IntervalDuration {

   Boolean lowerIncluded
   Boolean upperIncluded
   Boolean lowerUnbounded
   Boolean upperUnbounded
   Duration lower
   Duration upper

   // FIXME: this is all wrong
   // 1. duration is a string than can be represented as a number of seconds
   // 2. if the range limits are not included. +1 or -1 is not the right way of comparing, it should be <= vs. < or >= vs. >
   // 3. the seconds should be compared, in that way this would be the same as comparing integers
   boolean has(Duration d)
   {
      def i = d.seconds()

      def ll = (lowerUnbounded ? Long.MIN_VALUE : (lowerIncluded ? lower.seconds() : lower.seconds()+1))
      def hh = (upperUnbounded ? Long.MAX_VALUE : (upperIncluded ? upper.seconds() : upper.seconds()-1))

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
}
