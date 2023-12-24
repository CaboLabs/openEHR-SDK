package com.cabolabs.openehr.query.saqm.condition.datatypes

import com.cabolabs.openehr.query.saqm.condition.SAQMConditionSimple

class SAQMConditionDvDateTime extends SAQMConditionSimple {

   List    valueValue
   List    valueVariable  // is a list because 'range' needs two variables
   String  valueOperator
   boolean valueNegation = false

   // Support for functions
   List    age_in_yearsValue
   List    age_in_yearsVariable
   String  age_in_yearsOperator
   boolean age_in_yearsNegation = false

   List    age_in_monthsValue
   List    age_in_monthsVariable
   String  age_in_monthsOperator
   boolean age_in_monthsNegation = false

   /**
    * Metadata that defines the types of criteria supported to search
    * by conditions over DV_DATE_TIME.
    * @return
    */
   static List criteriaSpec(String archetypeId, String path, boolean returnCodes = true)
   {
      return [
         [
            value: [
               eq:  'value', // operators eq,lt,gt,... can be applied to attribute magnitude and the reference value is a single value
               lt:  'value',
               gt:  'value',
               neq: 'value',
               le:  'value',
               ge:  'value',
               between: 'range' // operator between can be applied to attribute magnitude and the reference value is a list of 2 values: min, max
            ]
         ],
         [
            age_in_years: [
               eq:  'value',
               lt:  'value',
               gt:  'value',
               neq: 'value',
               le:  'value',
               ge:  'value',
               between: 'range'
            ]
         ],
         [
            age_in_months: [
               eq:  'value',
               lt:  'value',
               gt:  'value',
               neq: 'value',
               le:  'value',
               ge:  'value',
               between: 'range'
            ]
         ]
      ]
   }

   static Map attributes()
   {
      return [
         'value': Date.class
      ]
   }

   static Map functions()
   {
      return [
         'age_in_years': Integer.class, // the type is the arg type for the function that has one arg
         'age_in_months': Integer.class
      ]
   }

   boolean containsFunction()
   {
      return age_in_yearsOperator != null || age_in_monthsOperator != null
   }

   String evaluateFunction(String function)
   {
      def time_attr, value, operator, negation
      if (function == 'age_in_years')
      {
         time_attr = 'years'

         value = age_in_yearsValue // TODO: need to support variables?
         operator = age_in_yearsOperator
         negation = age_in_yearsNegation
      }
      else if (function == 'age_in_months')
      {
         time_attr = 'months'

         value = age_in_monthsValue
         operator = age_in_monthsOperator
         negation = age_in_monthsNegation
      }
      else
      {
         throw new Exception("function $function not supported")
      }


      //def criteria_spec = specs[this.spec]
      //def criteriaValueType = criteria_spec[function][operator]
      def criteriaValueType = ((operator == 'between') ? 'range' : 'value')

      // age_in_years criteriaValueType is value or range
      if (criteriaValueType == 'value')
      {
         // function logic, calculates the limit age of the date with the value in years to compare with the attr 'value' in the query
         def now = new Date()
         def criteria_value
         use(TimeCategory) {
            criteria_value = now - value[0]."$time_attr"
         }

         return (negation ? 'NOT ' : '') + criteria_value.asSQLValue(operator) +' '+ QueryUtils.sqlOperator(operator) +' '+ this.alias +'.value '
      }
      else if (criteriaValueType == 'range')
      {
         value.sort()

         def now = new Date()
         def criteria_value_low, criteria_value_high
         use(TimeCategory) {
            criteria_value_low  = now - value[0]."$time_attr"
            criteria_value_high = now - value[1]."$time_attr" // high is really the lower value since value[1] is greater but is -
         }

         return this.alias +'.value '+ (negation ? 'NOT ' : '') +'BETWEEN '+ criteria_value_high.asSQLValue(operator) +' AND '+ criteria_value_low.asSQLValue(operator)
      }
   }

   Map variables()
   {
      Map variables = [:]

      if (this.valueVariable) variables.value = this.valueVariable

      // NOTE: these are functions not fields
      if (this.age_in_yearsVariable) variables.age_in_years = this.age_in_yearsVariable
      if (this.age_in_monthsVariable) variables.age_in_months = this.age_in_monthsVariable

      return variables
   }

   // NOTE: the value is already casted to the correct type in DataCriteria.put_variables()
   void put_variable(String attr, Object value)
   {
      if (attr == 'value')
      {
         // NOTE: values for multiple variables are set individually and are single values,
         //       we should accumulate values here.
         if (this.valueValue == null) this.valueValue = []

         this.valueValue << value
      }

      println "put variable "+ attr +" "+ value

      if (attr == 'age_in_years')
      {
         if (this.age_in_yearsValue == null) this.age_in_yearsValue = []

         this.age_in_yearsValue << value
      }

      if (attr == 'age_in_months')
      {
         if (this.age_in_monthsValue == null) this.age_in_monthsValue = []

         this.age_in_monthsValue << value
      }
   }
}