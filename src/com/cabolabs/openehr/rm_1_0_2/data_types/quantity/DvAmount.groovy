package com.cabolabs.openehr.rm_1_0_2.data_types.quantity

abstract class DvAmount extends DvQuantified {

   float accuracy
   boolean accuracy_is_percent

   // operators

   abstract DvAmount negative();
   abstract DvAmount plus(DvAmount e);
   abstract DvAmount minus(DvAmount e);
}
