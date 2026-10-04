package com.cabolabs.openehr.opt.model.domain

import com.cabolabs.openehr.opt.model.IntervalBigDecimal
import com.cabolabs.openehr.opt.model.IntervalInt

class CQuantityItem {

   IntervalBigDecimal magnitude // can be null
   IntervalInt precision // can be null
   String units

   // Optional, written by the template designer since the data types spec added them to the units of a quantity (the
   // constraint model should have them too). The code system of the units, e.g. http://unitsofmeasure.org, and the
   // name to show for them. Null when the OPT doesn't have them.
   String unitsSystem
   String unitsDisplayName

   String toString()
   {
      return "CQuantityItem "+ magnitude +" "+ units
   }
}
