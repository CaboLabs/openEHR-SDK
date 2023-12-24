package com.cabolabs.openehr.query.saqm.condition.datatypes

import com.cabolabs.openehr.query.saqm.condition.SAQMConditionSimple

class SAQMConditionDvQuantity extends SAQMConditionSimple {

   List    magnitudeValue
   List    magnitudeVariable // holds the variable name if there is no reference value
   String  magnitudeOperator
   boolean magnitudeNegation = false

   String  unitsValue
   String  unitsVariable
   String  unitsOperator
   boolean unitsNegation = false

   /**
    * Metadata that defines the types of criteria supported to search
    * by conditions over DV_QUANTITY.
    * @return
    */
   static List criteriaSpec(String archetypeId, String path, boolean returnCodes = true)
   {
      def spec = [
        [
          magnitude: [
            eq:  'value', // operators eq,lt,gt,... can be applied to attribute magnitude and the reference value is a single value
            lt:  'value',
            gt:  'value',
            neq: 'value',
            le:  'value',
            ge:  'value',
            between: 'range' // operator between can be applied to attribute magnitude and the reference value is a list of 2 values: min, max
          ],
          units: [
            eq: 'value'
          ]
        ]
      ]

      /* here we dont have the OptManager or Holder
      if (returnCodes)
      {
         //println archetypeId +" "+ path
         def optMan = OptManager.getInstance()
         def units = [:]
         def namespace = Holders.config.app.opt_repo_namespace

         // if the OPT doesn't define constraints for the quantity, the constraint will be
         // an ObjectNode not a CDvQuantity, and ObjectNode doesn't have a .list property.
         // In the case this has no further constraints, the type is C_COMPLEX_OBJECT.

         // There could be multiple constraints on the same path as alternatives
         // TODO: test if we need to display more than one criteria builder if there are alternative constraints
         def constraints = optMan.getNodes(archetypeId, path, namespace)
         if (constraints)
         {
            def constraint = constraints.find{ it.type == 'C_DV_QUANTITY' }
            if (constraint)
            {
               constraint.list.each { c_qty_item ->

                  // keep it as map to keep the same structure as the DV_CODED_TEXT
                  units[c_qty_item.units] = c_qty_item.units // mm[Hg] -> mm[Hg]
               }

               if (units.size() > 0) spec[0].units.units = units
            }
         }
      }
      */

      return spec
   }

   static Map attributes()
   {
       // TODO: we could return the types too so when variables for the attributes are defined,
       //       the types for the variable values can be checked against the attribute types
      return [
         'magnitude': Double.class,
         'units': String.class
      ]
   }

   static Map functions()
   {
      return [:]
   }

   // Map<attr, var name> if a variable is defined for the attribute.
   Map variables()
   {
      Map variables = [:]

      if (this.magnitudeVariable) variables.magnitude = this.magnitudeVariable
      if (this.unitsVariable) variables.units = this.unitsVariable

      return variables
   }

   // NOTE: the value is already casted to the correct type in DataCriteria.put_variables()
   void put_variable(String attr, Object value)
   {
      if (attr == 'magnitude')
      {
         // NOTE: values for multiple variables are set individually and are single
         //       values, we should accumulate values here.
         if (this.magnitudeValue == null) this.magnitudeValue = []

         this.magnitudeValue << value
      }
      else if (attr == 'units')
      {
         this.unitsValue = value
      }
   }

   // String toString()
   // {
   //    return this.getClass().getSimpleName() +": "+ this.magnitudeOperator +" "+ this.magnitudeValue.toString() +" "+ this.unitsOperator +" "+ this.unitsValue
   // }

   boolean containsFunction()
   {
      return false
   }
}