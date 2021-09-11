package com.cabolabs.openehr.opt.instance_generator

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
}