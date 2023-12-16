package com.cabolabs.openehr.query.saqm.condition.datatypes

class SAQMConditionDvText extends SAQMConditionSimple {

   // 'value' is the RM attribute of DvText over which we want to define a criteria/condition,
   // all the attributes below represent many kinds of conditions over the 'value'

   // list of strings
   List valueValue

   // Used to support variables instead of given values for the condition, so we
   // can do /a/b/c = $VAR instead of /a/b/c = 'given ref value'.
   //
   // This doesn't need to be a List for this criteria, we need to check if a
   // variable with the same name is a list in another criteria to avoid collision
   // then we need to set a mapping name to avoid two variables with the same name
   // in two different criterias.
   String  valueVariable


   // TODO: make a enum class for all operators and a config for the operators
   //       each dv criteria can use
   String  valueOperator // eq, contains_like
   boolean valueNegation = false
}