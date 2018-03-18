package com.cabolabs.openehr.opt.model

class IntervalInt {

   boolean lowerIncluded
   boolean upperIncluded
   boolean lowerUnbounded
   boolean upperUnbounded
   Integer lower
   Integer upper

   boolean has(Integer i)
   {
      def ll = (lowerUnbounded ? Integer.MIN_VALUE : (lowerIncluded ? lower : lower+1))
      def hh = (upperUnbounded ? Integer.MAX_VALUE : (upperIncluded ? upper : upper-1))

      return ll <= i && i <= hh
   }
}
