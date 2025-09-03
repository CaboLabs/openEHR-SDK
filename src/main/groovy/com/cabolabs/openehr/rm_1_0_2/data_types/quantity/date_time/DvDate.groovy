package com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time

import java.text.SimpleDateFormat
import java.time.LocalDate

class DvDate extends DvTemporal {

   String value

   // Prevents groovy.lang.GroovyRuntimeException: Could not find named-arg compatible constructor with the second constructor
   DvDate()
   {

   }

   DvDate(Date date)
   {
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd"); // ISO date format
      this.value = formatter.format(date)
   }

   DvDate(LocalDate date)
   {
      this.value = date.toString() // ISO date format
   }


   @Override
   public Number getMagnitude() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public int compareTo(Object o) {
      // TODO Auto-generated method stub
      return 0;
   }

   LocalDate toLocalDate()
   {
      return LocalDate.parse(this.value)
   }
}
