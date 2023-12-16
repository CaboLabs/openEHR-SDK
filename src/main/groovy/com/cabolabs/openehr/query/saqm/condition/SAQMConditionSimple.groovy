package com.cabolabs.openehr.query.saqm.condition

class SAQMConditionSimple extends SAQMCondition {

   String archetypeId
   String path
   String rmTypeName
   boolean allowAnyArchetypeVersion = false
   boolean isExists = false
}