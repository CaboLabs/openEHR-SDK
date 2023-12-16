package com.cabolabs.openehr.query.saqm

import com.cabolabs.openehr.query.OpenEhrQuery

class SAQMQuery extends OpenEhrQuery {

   String uid

   String name
   String description

   List projections
   List conditions

   boolean isCount = false
}