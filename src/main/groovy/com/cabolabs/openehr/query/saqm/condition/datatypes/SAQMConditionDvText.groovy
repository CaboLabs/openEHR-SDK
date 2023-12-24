package com.cabolabs.openehr.query.saqm.condition.datatypes

import com.cabolabs.openehr.query.saqm.condition.SAQMConditionSimple

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

   /**
    * Metadata that defines the types of criteria supported to search
    * by conditions over DV_QUANTITY.
    * @return
    */
   static List criteriaSpec(String archetypeId, String path, boolean returnCodes = true)
   {
      return [
        [
          value: [
            //contains:  'value', // ilike %value%
            contains_like: 'list', // a like %value0% OR a like %value1% OR ...
            eq:  'value'
          ]
        ]
      ]
   }

   static Map functions()
   {
      return [:]
   }

   static Map attributes()
   {
      return [
         'value': String.class
      ]
   }


   // Map<attr, var name> if a variable is defined for the attribute.
   Map variables()
   {
      Map variables = [:]

      if (this.valueVariable) variables.value = this.valueVariable

      return variables
   }

   void put_variable(String attr, Object value)
   {
      if (attr == 'value')
      {
         // the value is a list?
         if (this.valueOperator == 'contains_like')
         {
            value = value.split(',').collect{ it.trim() }
            this.valueValue = value
         }
         else
         {
            this.valueValue = [value] // single value in a list
         }
      }
   }

   boolean containsFunction()
   {
      return false
   }
}