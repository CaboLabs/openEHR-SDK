package com.cabolabs.openehr.opt.model

import com.cabolabs.openehr.opt.model.datatypes.Duration

class IntervalDuration {

   boolean lowerIncluded
   boolean upperIncluded
   boolean lowerUnbounded
   boolean upperUnbounded
   Duration lower
   Duration upper

   boolean has(Duration i)
   {
      def ll = (lowerUnbounded ? Long.MIN_VALUE : (lowerIncluded ? lower : lower+1))
      def hh = (upperUnbounded ? Long.MAX_VALUE : (upperIncluded ? upper : upper-1))

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
