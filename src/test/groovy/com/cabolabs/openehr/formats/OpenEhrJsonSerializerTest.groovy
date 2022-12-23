package com.cabolabs.openehr.formats

import groovy.util.GroovyTestCase
import com.cabolabs.openehr.formats.OpenEhrJsonSerializer
import com.cabolabs.openehr.dto_1_0_2.ehr.EhrDto
import com.cabolabs.openehr.rm_1_0_2.support.identification.HierObjectId
import com.cabolabs.openehr.rm_1_0_2.support.identification.PartyRef
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.DvQuantity
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvText
import com.cabolabs.openehr.rm_1_0_2.ehr.EhrStatus
import com.cabolabs.openehr.rm_1_0_2.support.identification.ArchetypeId
import com.cabolabs.openehr.rm_1_0_2.support.identification.TemplateId
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.Element
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemTree
import com.cabolabs.openehr.rm_1_0_2.common.generic.PartySelf
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Archetyped

import groovy.json.JsonSlurper
import com.cabolabs.openehr.opt.instance_validation.JsonInstanceValidation

class OpenEhrJsonSerializerTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")

   void testEhrDtoSerialize()
   {
      def dto_ehr = new EhrDto(
         ehr_id: new HierObjectId(value: '7d44b88c-4199-4bad-97dc-d78268e01398'),
         system_id: new HierObjectId(value: '9624982A-9F42-41A5-9318-AE13D5F5031F'),
         time_created: new DvDateTime(value: '2015-01-20T19:30:22.765+01:00'),
         ehr_status: new EhrStatus(
            name: new DvText(
               value: 'status'
            ),
            archetype_node_id: 'openEHR-EHR-EHR_STATUS.generic.v1',
            archetype_details: new Archetyped(
               archetype_id: new ArchetypeId(
                  value: 'openEHR-EHR-EHR_STATUS.generic.v1'
               ),
               template_id: new TemplateId(
                  value: 'generic.en.v1'
               ),
               rm_version: '1.0.2'
            ),
            is_queryable: true,
            is_modifiable: true,
            subject: new PartySelf(
               external_ref: new PartyRef(
                  type: 'PERSON',
                  namespace: 'com.cabolabs.ehr',
                  id: new HierObjectId(
                     value: 'f27e75f0-39cf-4101-a430-936c293d73f5'
                  )
               )
            ),
            other_details: new ItemTree(
               name: new DvText(
                  value: 'tree'
               ),
               archetype_node_id: 'at0001',
               items: [
                  new Element(
                     name: new DvText(
                        value: 'element'
                     ),
                     archetype_node_id: 'at0002',
                     value: new DvQuantity(
                        magnitude: 123,
                        units: 'cm'
                     )
                  )
               ]
            )
         )
      )

      def serializer = new OpenEhrJsonSerializer()
      def ehr_string = serializer.serialize(dto_ehr)

      def slurper = new JsonSlurper()
      def json_map = slurper.parseText(ehr_string)

      def validator = new JsonInstanceValidation('api', '1.0.2')
      def errors = validator.validate(json_map)

      assert !errors
   }

}