package com.cabolabs.openehr.opt.instance_generator

import com.cabolabs.openehr.opt.model.*
import java.time.LocalDate
import java.time.temporal.WeekFields
import com.cabolabs.openehr.rm_1_0_2.support.identification.PartyRef
import com.cabolabs.openehr.rm_1_0_2.support.identification.HierObjectId
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.DvInterval
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDate

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
            case 'W':
               LocalDate date = LocalDate.now();
               WeekFields weekFields = WeekFields.of(Locale.getDefault())
               gen += date.get(weekFields.weekOfWeekBasedYear()) + 'W'
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
      Long u_limit_seconds = Long.MAX_VALUE

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
      // the if below is to prevent the case where both limits are the same, and nextLong throws an exception
      Long value = (l_limit_seconds < u_limit_seconds ? rnd.nextLong(l_limit_seconds, u_limit_seconds) : l_limit_seconds)

      // NOTE: this only outputs time
      return java.time.Duration.ofSeconds(value).toString()
   }

   // generates a magnitude for DV_COUNT.magnitude constraint CInteger.range
   static Integer int_in_range(IntervalInt range)
   {
      Integer value

      Integer lo = ((range.lowerUnbounded) ? 0 : range.lower)
      Integer hi = ((range.upperUnbounded) ? 100 : range.upper)

      if (!range.lowerIncluded) lo++
      if (!range.upperIncluded) hi--

      // random between lo .. hi
      // the +1 is because the upper is exclusive
      if (lo < hi)
         value = new Random().nextInt(hi - lo + 1) + lo
      else
         value = lo

      return value
   }

   static Double double_in_range(IntervalDouble range)
   {
      Double value

      Double lo = ((range.lowerUnbounded) ? 0 : range.lower)
      Double hi = ((range.upperUnbounded) ? 100 : range.upper)

      if (!range.lowerIncluded) lo += 0.1
      if (!range.upperIncluded) hi -= 0.1

      if (lo < hi)
         value = new Random().nextDouble() * (hi - lo) + lo // random between lo .. hi
      else
         value = lo

      return value
   }

   static PartyRef random_party_ref(String demographicType = "PERSON")
   {
      new PartyRef(
         namespace: 'DEMOGRAPHIC',
         type: demographicType,
         id: new HierObjectId(
            value: java.util.UUID.randomUUID().toString()
         )
      )
   }

   // This is used for demographics .time_validity intervals
   static DvInterval date_interval()
   {
      new DvInterval(
         lower_included: true,
         lower_unbounded: false,
         upper_included: false,
         upper_unbounded: true,
         lower: new DvDate(
            value: new Date().toOpenEHRDate()
         )
      )
   }
}