package com.cabolabs.openehr.opt

import groovy.util.GroovyTestCase

import com.cabolabs.openehr.opt.parser.*
import com.cabolabs.openehr.opt.ui_generator.OptUiGenerator
import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.opt.model.primitive.*
import com.cabolabs.openehr.opt.model.domain.*
import com.cabolabs.openehr.opt.manager.*
import com.cabolabs.openehr.opt.instance_generator.*
import com.cabolabs.openehr.terminology.TerminologyParser
import com.cabolabs.openehr.opt.instance_validation.XmlValidation
import com.cabolabs.openehr.opt.instance_generator.*
import com.cabolabs.openehr.opt.serializer.*
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.Element

import com.cabolabs.openehr.rm_1_0_2.data_types.text.*
import com.cabolabs.openehr.rm_1_0_2.support.identification.TerminologyId
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.*

import com.cabolabs.testing.TestUtils

import com.cabolabs.openehr.query.saqm.*
import com.cabolabs.openehr.query.saqm.condition.*
import com.cabolabs.openehr.query.saqm.condition.datatypes.*

import com.cabolabs.openehr.formats.SAQMJsonSerializer

class SAQMTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")

   void testQuery1()
   {
      def q = new SAQMQuery(
         qualifiedName: 'com.cabolabs::blood_pressure_filter',
         name: 'query 1',
         description: 'test query 1',
         where: new SAQMConditionDvQuantity(
            archetypeId: 'openEHR-EHR-OBSERVATION.blood_pressure.v2',
            path: '/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value',
            rmTypeName: 'DV_QUANTITY',

            magnitudeValue: [110],
            magnitudeOperator: 'lt',

            unitsValue: 'mm[Hg]',
            unitsOperator: 'eq'
         )
      )

      def s = new SAQMJsonSerializer(true)

      println s.serialize(q)

      assert true
   }


   void testQuery2()
   {
      def q = new SAQMQuery(
         qualifiedName: 'com.cabolabs::blood_pressure_and_event_date_filter',
         name: 'query 2',
         description: 'test query 2',
         where: new SAQMConditionAnd(
            c1: new SAQMConditionDvQuantity(
               archetypeId: 'openEHR-EHR-OBSERVATION.blood_pressure.v2',
               path: '/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value',
               rmTypeName: 'DV_QUANTITY',

               magnitudeValue: [110],
               magnitudeOperator: 'lt',

               unitsValue: 'mm[Hg]',
               unitsOperator: 'eq'
            ),
            c2: new SAQMConditionDvDateTime(
               archetypeId: 'openEHR-EHR-OBSERVATION.blood_pressure.v2',
               path: '/data[at0001]/events[at0006]/time',
               rmTypeName: 'DV_DATE_TIME',

               valueValue: ['2023-12-19 01:10:00'],
               valueOperator: 'eq'
            )
         )
      )

      def s = new SAQMJsonSerializer(true)

      println s.serialize(q)

      assert true
   }

}
