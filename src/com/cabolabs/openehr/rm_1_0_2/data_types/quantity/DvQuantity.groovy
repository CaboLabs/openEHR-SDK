package com.cabolabs.openehr.rm_1_0_2.data_types.quantity

class DvQuantity extends DvAmount {

   Double magnitude
   Integer precision // could be null
   String units


   DvAmount negative()
   {
      // TODO: init magnitude status, accuracy, etc if needed
      return new DvQuantity(
         magnitude: -this.magnitude,
         units: this.units,
         precision: this.precision)
   }

   DvAmount plus(DvAmount e)
   {
      if (!e instanceof DvCount) throw new Exception("Can't add "+ e.getClass() + " to DvQuantity")
      if (e.units != this.units) throw new Exception("Can't add quantities with different units "+ this.units +" / "+ e.units)

      return new DvQuantity(
         magnitude: this.magnitude + e.magnitude,
         units: this.units,
         precision: this.precision)
   }

   DvAmount minus(DvAmount e)
   {
      if (!e instanceof DvCount) throw new Exception("Can't substract "+ e.getClass() + " to DvQuantity")
      if (e.units != this.units) throw new Exception("Can't substract quantities with different units "+ this.units +" / "+ e.units)

      return new DvQuantity(
         magnitude: this.magnitude - e.magnitude,
         units: this.units,
         precision: this.precision)
   }

   Number getMagnitude()
   {
      return this.magnitude
   }

   int compareTo(Object o)
   {
      if (!(o instanceof DvQuantity)) throw new Exception("Can't compare "+ e.getClas() +" to DvQuantity")
      if (this.units != o.units) throw new Exception("Can't compare quantities with different units")

      return this.magnitude <=> o.magnitude
   }
}
