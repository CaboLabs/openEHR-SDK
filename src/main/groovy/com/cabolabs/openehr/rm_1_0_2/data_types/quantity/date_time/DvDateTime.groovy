package com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time

import java.text.SimpleDateFormat
import java.text.ParseException

class DvDateTime extends DvTemporal {

   String value

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

   Date toDate()
   {
      def formats = ["yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                     "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                     "yyyy-MM-dd'T'HH:mm:ss.SSS",

                     "yyyy-MM-dd'T'HH:mm:ss,SSS'Z'", // seconds fraction alternative with comma
                     "yyyy-MM-dd'T'HH:mm:ss,SSSX",
                     "yyyy-MM-dd'T'HH:mm:ss,SSS",

                     "yyyy-MM-dd'T'HH:mm:ss'Z'",
                     "yyyy-MM-dd'T'HH:mm:ssX",
                     "yyyy-MM-dd'T'HH:mm:ss",

                     "yyyy-MM-dd'T'HH:mm'Z'",
                     "yyyy-MM-dd'T'HH:mmX",
                     "yyyy-MM-dd'T'HH:mm",

                     "yyyy-MM-dd'T'HH'Z'",
                     "yyyy-MM-dd'T'HHX",
                     "yyyy-MM-dd'T'HH",

                     "yyyyMMdd'T'HHmmss.SSS'Z'",         // basic formats ISO-8601
                     "yyyyMMdd'T'HHmmss.SSSX",
                     "yyyyMMdd'T'HHmmss.SSS",

                     "yyyyMMdd'T'HHmmss,SSS'Z'",         // basic formats ISO-8601, seconds fraction alternatives with comma
                     "yyyyMMdd'T'HHmmss,SSSX",
                     "yyyyMMdd'T'HHmmss,SSS",

                     "yyyyMMdd'T'HHmmss'Z'",
                     "yyyyMMdd'T'HHmmssX",
                     "yyyyMMdd'T'HHmmss",

                     "yyyyMMdd'T'HHmm'Z'",
                     "yyyyMMdd'T'HHmmX",
                     "yyyyMMdd'T'HHmm",

                     "yyyyMMdd'T'HH'Z'",
                     "yyyyMMdd'T'HHX",
                     "yyyyMMdd'T'HH"
                     ]

      def date
      for (String format : formats)
      {
         try
         {
            SimpleDateFormat sdf = new SimpleDateFormat(format)
            sdf.setLenient(false) // avoids heuristic parsing, enabling just exact parsing
            date = sdf.parse(value) // returns the date but we'll not use it

            return date
         }
         catch (ParseException e) {}
      }

      throw new Exception("Can't parse date value: ${value}")
   }
}
