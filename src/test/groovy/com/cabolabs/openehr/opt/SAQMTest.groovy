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

import com.cabolabs.openehr.opt.parser.*

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


   void testQuery3()
   {
      def q = new SAQMQuery(
         qualifiedName: 'com.cabolabs::blood_pressure_and_event_date_filter',
         name: 'query 2',
         description: 'test query 2',
         where: new SAQMConditionAnd(
            c1: new SAQMConditionDvQuantity(
               archetypeId: 'openEHR-EHR-OBSERVATION.pulse.v2',
               path: '/data[at0002]/events[at0003]/data[at0001]/items[at0004]/value',
               rmTypeName: 'DV_QUANTITY',

               magnitudeValue: [70],
               magnitudeOperator: 'eq',

               unitsValue: '/min',
               unitsOperator: 'eq'
            ),
            c2: new SAQMConditionAnd(
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
      )

      def s = new SAQMJsonSerializer(true)

      println s.serialize(q)

      analyzeQuery(q)

      assert true
   }


   private def analyzeQuery(SAQMQuery q)
   {
      def parser = new OperationalTemplateParser()
      def path = PS +"opts"+ PS + 'com.cabolabs.openehr_opt.namespaces.default' + PS +"Monitoreo_de_signos.opt"
      def optFile = new File(getClass().getResource(path).toURI())
      def text = optFile.getText()
      def opt = parser.parse(text)
      opt.complete()



      // This is the algorithm to detect parent-child conditions and add the extra hierarchy set condition for those

      def simpleConditions = q.getSimpleConditions()
      def size = simpleConditions.size()


      def cond1, cond2, obn, parentNodes, parentObn, condParent, condChild
      for (int i = 0; i < size; i++)
      {
         cond1 = simpleConditions[i]
         for (int j = i+1; j < size; j++)
         {
            cond2 = simpleConditions[j]

            if (cond1.archetypeId == cond2.archetypeId)
            {
               obn = opt.findRoot(cond1.archetypeId)

               // query paths always point to data values, so the parent object is always a locatable or pathable
               println cond1.path +" "+ cond2.path

               // we only need one matching node to get the parent
               // TODO: if nodes is empty, then the path in the query is incorrect in terms of the referenced template/archetype

               // check if parent of path1 contains path2
               if (cond1.path.size() < cond2.path.size())
               {
                  condParent = cond1
                  condChild = cond2
                  parentNodes = obn.getNodes(cond1.path)
               }
               else
               {
                  condParent = cond2
                  condChild = cond1
                  parentNodes = obn.getNodes(cond2.path)
               }

               parentObn = parentNodes[0].parent.parent
               if (parentObn.nodes.any{ it.key == condChild.path })
               {
                  println "Add condition hierarchy set for "+ condParent.path +" and "+ condChild.path
               }
            }
         }
      }


   }

}
