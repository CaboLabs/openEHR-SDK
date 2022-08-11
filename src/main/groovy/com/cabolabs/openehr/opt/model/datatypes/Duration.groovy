package com.cabolabs.openehr.opt.model.datatypes

import com.cabolabs.openehr.rm_1_0_2.support.date_time.TimeDefinitions

class Duration implements Comparable<Duration> {

   String value

   Duration(Map attrs)
   {
      if (attrs.value)
      {
         if (!Duration.isValid(attrs.value))
         {
            throw new Exception("Duration value '${attrs.value}' is not a valid duration expression")
         }

         this.value = attrs.value
      }
   }

   def setValue(String value)
   {
      if (!Duration.isValid(value))
      {
         throw new Exception("Duration value '${value}' is not a valid duration expression")
      }

      this.value = value
   }

   Long seconds()
   {
      // Period can parse date but not time! but cant retrieve seconds
      // Duration can parse from days to seconds but cant parse months or years
      def d, p
    
      def secondsInDay = TimeDefinitions.hoursInDay * TimeDefinitions.minutesInHour * TimeDefinitions.secondsInMinute
    
      if (this.value.contains("T"))
      {
         def parts = this.value.split('T')
         if (parts[0].contains('Y') || parts[0].contains('M'))
         {
            try
            {
               // parse time part as duration
               d = java.time.Duration.parse('PT'+ parts[1])
    
               // parse date part as period
               p = java.time.Period.parse(parts[0])
            }
            catch (Exception e)
            {
               throw new Exception("Value '${this.value}' cannot be parsed as a valid duration expression", e)
            }
    
            // this is wrong but is an approximation
            // months of 30 days, years of 365 days
            def date_dur_in_seocnds = p.years * TimeDefinitions.averageDaysInYear * secondsInDay +
                                      p.months * TimeDefinitions.nominalDaysInMonth * secondsInDay +
                                      p.days * secondsInDay
    
            return date_dur_in_seocnds + d.seconds
         }
         else // contains only days, and time
         {
            try
            {
               d = java.time.Duration.parse(this.value)
            }
            catch (Exception e)
            {
               throw new Exception("Value '${this.value}' cannot be parsed as a valid duration expression", e)
            }
            return d.seconds //get(java.time.temporal.ChronoUnit.SECONDS)
         }
      }
      else // no time in duration
      {
         try
         {
            // parse date part as period
            p = java.time.Period.parse(this.value)
         }
         catch (Exception e)
         {
            throw new Exception("Value '${this.value}' cannot be parsed as a valid duration expression", e)
         }
    
         // this is wrong but is an approximation
         // months of 30 days, years of 365 days
         def date_dur_in_seocnds = p.years * TimeDefinitions.averageDaysInYear * secondsInDay +
                                   p.months * TimeDefinitions.nominalDaysInMonth * secondsInDay +
                                   p.days * secondsInDay
    
         return date_dur_in_seocnds
      }
      

      // FIXME: Java Duration only allows from Days to Seconds, Years, Months and Weeks are not allowed
      // https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-
      // Solution: use PeriodDuration from ThreeTen project
      // http://www.threeten.org/threeten-extra/apidocs/org/threeten/extra/PeriodDuration.html#parse-java.lang.CharSequence-
      // or use in combination with JAva 8 Period
      // https://docs.oracle.com/javase/8/docs/api/java/time/Period.html#parse-java.lang.CharSequence-
   }

   Float minutes()
   {
      seconds() / 60
   }

   Float hours()
   {
      minutes() / 60
   }

   Float days()
   {
      hours() / 24
   }

   @Override
   int compareTo(Duration d)
   {
      seconds() <=> d.seconds()
   }

   String toString()
   {
      return this.value
   }

   // validates durationExpression is a valid ISO8601 duration
   // the code is similar to seconds but without doing the calculations or throwing exceptions
   static boolean isValid(String durationExpression)
   {
      def d, p
    
      if (durationExpression.contains("T"))
      {
         def parts = durationExpression.split('T')
         if (parts[0].contains('Y') || parts[0].contains('M'))
         {
            try
            {
               // parse time part as duration
               d = java.time.Duration.parse('PT'+ parts[1])
    
               // parse date part as period
               p = java.time.Period.parse(parts[0])
            }
            catch (Exception e)
            {
               return false
            }
         }
         else // contains only days, and time
         {
            try
            {
               d = java.time.Duration.parse(durationExpression)
            }
            catch (Exception e)
            {
               return false
            }
         }
      }
      else // no time in duration
      {
         try
         {
            // parse date part as period
            p = java.time.Period.parse(durationExpression)
         }
         catch (Exception e)
         {
            return false
         }
      }

      return true
   }
}
