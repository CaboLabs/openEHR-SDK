package com.cabolabs.openehr.opt.model.datatypes

class Duration implements Comparable<Duration> {

   String value

   Long seconds()
   {
      // Period can parse date but not time! but cant retrieve seconds
      // Duration can parse from days to seconds but cant parse months or years
      def d, p

      if (this.value.contains("T"))
      {
         def parts = this.value.split('T')
         if (parts[0].contains('Y') || parts[0].contains('M'))
         {
            // parse time part as duration
            d = java.time.Duration.parse('PT'+ parts[1])

            // parse date part as period
            p = java.time.Period.parse(parts[0])

            // this is wrong but is an approximation
            // months of 30 days, years of 365 days
            def date_dur_in_seocnds = p.years * (365 * 86400) + p.months * (30 * 86400) + p.days * 86400

            return date_dur_in_seocnds + d.seconds
         }
         else // contains only days, and time
         {
            d = java.time.Duration.parse(this.value)
            return d.seconds //get(java.time.temporal.ChronoUnit.SECONDS)
         }
      }
      else // no time in duration
      {
         // parse date part as period
         p = java.time.Period.parse(this.value)

         // this is wrong but is an approximation
         // months of 30 days, years of 365 days
         def date_dur_in_seocnds = p.years * (365 * 86400) + p.months * (30 * 86400) + p.days * 86400

         return date_dur_in_seocnds
      }

      // only works in Java 8
      

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
}
