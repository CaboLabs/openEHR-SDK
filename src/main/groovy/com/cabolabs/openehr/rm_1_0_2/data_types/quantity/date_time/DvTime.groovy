package com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time

import java.time.LocalTime

class DvTime extends DvTemporal {

   String value

   DvTime()
   {

   }

   DvTime(LocalTime time)
   {
      this.value = time.toString() // ISO time format
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

   LocalTime toLocalTime()
   {
      return LocalTime.parse(this.value)
   }
}
