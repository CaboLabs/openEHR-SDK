package com.cabolabs.openehr.query.saqm

import com.cabolabs.openehr.query.OpenEhrQuery
import com.cabolabs.openehr.query.saqm.condition.SAQMCondition

class SAQMQuery extends OpenEhrQuery {

   String uid

   String name
   String description

   List projections
   SAQMCondition where

   boolean isCount = false
}