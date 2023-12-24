package com.cabolabs.openehr.formats

import java.text.SimpleDateFormat
import groovy.json.*

import com.cabolabs.openehr.query.saqm.*
import com.cabolabs.openehr.query.saqm.condition.*
import com.cabolabs.openehr.query.saqm.condition.datatypes.*

class SAQMJsonSerializer {

   boolean pretty

   public SAQMJsonSerializer()
   {
      this(false)
   }

   public SAQMJsonSerializer(boolean pretty)
   {
      this.pretty = pretty
   }

   // TODO: comply with the JSON date format
   //def dateFormatter = new SimpleDateFormat("yyyyMMdd'T'HHmmss,SSSZ")

   private String encode(Map m)
   {
      if (this.pretty)
      {
         return JsonOutput.prettyPrint(JsonOutput.toJson(m))
      }

      return JsonOutput.toJson(m)
   }

   String serialize(SAQMQuery query)
   {
      def out = this.toMap(query)
      this.encode(out)
   }

   Map toMap(SAQMQuery query)
   {
      return this.serializeSAQMQuery(query)
   }

   private Map serializeSAQMQuery(SAQMQuery query)
   {
      def out = [:]

      out._type = 'SAQMQuery'
      out.qualifiedName = query.qualifiedName
      out.version = query.version
      out.uid = query.uid
      out.name = query.name
      out.description = query.description
      out.projections = []

      query.projections.each {
         out.projections << serializeSAQMLProjection(it)
      }

      out.where = serializeSAQMLCondition(query.where)

      return out
   }

   private fillSAQMConditionSimple(SAQMConditionSimple cond, Map out)
   {
      out.archetypeId = cond.archetypeId
      out.path = cond.path
      out.rmTypeName = cond.rmTypeName

      // TODO: allowAnyArchetypeVersion
      // TODO: isExists
   }


   private Map serializeSAQMLCondition(SAQMConditionDvQuantity cond)
   {
      def out = [:]

      out._type = 'ConditionDvQuantity'

      fillSAQMConditionSimple(cond, out)

      out.magnitudeValue    = cond.magnitudeValue
      out.magnitudeVariable = cond.magnitudeVariable
      out.magnitudeOperator = cond.magnitudeOperator
      out.magnitudeNegation = cond.magnitudeNegation

      out.unitsValue    = cond.unitsValue
      out.unitsVariable = cond.unitsVariable
      out.unitsOperator = cond.unitsOperator
      out.unitsNegation = cond.unitsNegation

      return out
   }

   private Map serializeSAQMLCondition(SAQMConditionDvDateTime cond)
   {
      def out = [:]

      out._type = 'ConditionDvDateTime'

      fillSAQMConditionSimple(cond, out)

      out.valueValue    = cond.valueValue
      out.valueVariable = cond.valueVariable
      out.valueOperator = cond.valueOperator
      out.valueNegation = cond.valueNegation

      out.age_in_yearsValue    = cond.age_in_yearsValue
      out.age_in_yearsVariable = cond.age_in_yearsVariable
      out.age_in_yearsOperator = cond.age_in_yearsOperator
      out.age_in_yearsNegation = cond.age_in_yearsNegation

      out.age_in_monthsValue    = cond.age_in_monthsValue
      out.age_in_monthsVariable = cond.age_in_monthsVariable
      out.age_in_monthsOperator = cond.age_in_monthsOperator
      out.age_in_monthsNegation = cond.age_in_monthsNegation

      return out
   }

   private Map serializeSAQMLCondition(SAQMConditionAnd cond)
   {
      def out = [:]

      out._type = "AND"
      out.c1 = serializeSAQMLCondition(cond.c1)
      out.c2 = serializeSAQMLCondition(cond.c2)

      return out
   }
}