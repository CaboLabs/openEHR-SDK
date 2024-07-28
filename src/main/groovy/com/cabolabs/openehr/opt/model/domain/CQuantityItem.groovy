package com.cabolabs.openehr.opt.model.domain

import com.cabolabs.openehr.opt.model.IntervalDouble
import com.cabolabs.openehr.opt.model.IntervalInt

@groovy.util.logging.Log4j2
class CQuantityItem {

   IntervalDouble magnitude // can be null
   IntervalInt precision // can be null
   String units
}
