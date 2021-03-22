package com.cabolabs.openehr.rm_1_0_2.data_types.quantity

class DvCount extends DvAmount {

   Integer magnitude

   DvAmount negative()
   {
      // TODO: init magnitude status, accuracy, etc if needed
      return new DvCount(magnitude: -this.magnitude)
   }

   DvAmount plus(DvAmount e)
   {
      if (!e instanceof DvCount) throw new Exception("Can't add "+ e.getClass() + " to DvCount")

      return new DvCount(magnitude: this.magnitude + e.magnitude)
   }

   DvAmount minus(DvAmount e)
   {
      if (!e instanceof DvCount) throw new Exception("Can't substract "+ e.getClass() + " to DvCount")

      return new DvCount(magnitude: this.magnitude - e.magnitude)
   }

   Number getMagnitude()
   {
      return this.magnitude
   }

   int compareTo(Object o)
   {
      if (!(o instanceof DvCount)) throw new Exception("Can't compare "+ e.getClas() +" to DvCount")

      return this.magnitude <=> o.magnitude
   }
}
