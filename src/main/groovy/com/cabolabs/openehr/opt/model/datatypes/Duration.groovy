package com.cabolabs.openehr.opt.model.datatypes

class Duration implements Comparable<Duration> {

   String value

   Long seconds()
   {
      // only works in Java 8
      java.time.Duration d = java.time.Duration.parse(value)
      d.get(java.time.temporal.ChronoUnit.SECONDS)

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
