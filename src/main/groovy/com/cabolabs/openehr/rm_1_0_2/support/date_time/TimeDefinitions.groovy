package com.cabolabs.openehr.rm_1_0_2.support.date_time

class TimeDefinitions {

   static final int   secondsInMinute    = 60
   static final int   minutesInHour      = 60
   static final int   hoursInDay         = 24

   // note this is considering a non-leap year 365/12 =~ 30.42
   // a more accurate value is averageDaysInyear from NASA below 365.2425/12 = 30.436875
   static final BigDecimal nominalDaysInMonth = 30.436875
   static final int   maxDaysInMonth     = 31
   static final int   daysInYear         = 365
   static final int   daysInLeapYear     = 366
   static final int   maxDaysInYear      = 366

   // not from 1.0.2. it's from 1.2.0 but is needed to transform year durations to days and then to seconds
   // note this is the accurate calendar year not using the 365.24 value from the 1.2.0 specs
   // spec value: https://specifications.openehr.org/releases/BASE/Release-1.2.0/foundation_types.html#_time_definitions_class
   // more accurate value: https://www.grc.nasa.gov/www/k-12/Numbers/Math/Mathematical_Thinking/calendar_calculations.htm
   static final BigDecimal averageDaysInYear  = 365.2425 // correct for Gregorian calendar
}