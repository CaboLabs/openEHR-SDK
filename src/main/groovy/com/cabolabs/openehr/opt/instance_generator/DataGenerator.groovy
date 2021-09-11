package com.cabolabs.openehr.opt.instance_generator

import com.cabolabs.openehr.opt.model.IntervalDuration

class DataGenerator {
   
   // Generates DV_DURATION.value expression
   static String duration_value_from_pattern(String pattern)
   {
      Random random = new Random()
      def gen = ""
      def is_time = false
      def util_date = new Date()
      pattern.each { c ->

         switch (c)
         {
            case 'P':
               gen += c
            break
            case 'T':
               gen += c
               is_time = true
            break
            case 'Y':
               gen += (1900 + util_date.getYear()) + 'Y'
            break
            case 'M':
               if (is_time)
                  gen += random.nextInt(60) + 'M'
               else
                  gen += (util_date.getMonth()+1) + 'M'
            break
            case 'D':
               gen += util_date.getDate() + 'D'
            break
            case 'H':
               gen += random.nextInt(24) + c
            break
            case 'S':
               gen += random.nextInt(60) + c
            break
         }
      }

      return gen
   }

   // returns a duration expression that is in the provided interval
   static String duration_in_interval(IntervalDuration ivd)
   {
      Long l_limit_seconds = 0 // avoid negative durations
      Long u_limit_seconds  = Long.MAX_VALUE

      if (!ivd.lowerUnbounded)
      {
         l_limit_seconds = ivd.lower.seconds()
      }

      if (!ivd.upperUnbounded)
      {
         u_limit_seconds = ivd.upper.seconds()
      }

      // if the lower is not included, take the next second
      if (!ivd.lowerIncluded) l_limit_seconds++

      // random generates including the lower and excluding the upper
      def rnd = new java.util.concurrent.ThreadLocalRandom()
      Long value = rnd.nextLong(l_limit_seconds, u_limit_seconds)

      // NOTE: java duration only supports time
      return java.time.Duration.ofSeconds(value).toString()
   }
}