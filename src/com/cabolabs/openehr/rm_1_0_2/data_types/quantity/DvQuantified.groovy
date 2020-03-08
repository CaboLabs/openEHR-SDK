package com.cabolabs.openehr.rm_1_0_2.data_types.quantity

import java.lang.Number

abstract class DvQuantified extends DvOrdered {

   String magnitude_status

   // TODO: there are methods not implemented here yet

   abstract Number getMagnitude();
}
