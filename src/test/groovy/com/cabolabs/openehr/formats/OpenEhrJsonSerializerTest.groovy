package com.cabolabs.openehr.formats

import groovy.util.GroovyTestCase
import com.cabolabs.openehr.dto_1_0_2.ehr.EhrDto
import com.cabolabs.openehr.dto_1_0_2.demographic.*

import com.cabolabs.openehr.rm_1_0_2.ehr.EhrStatus
import com.cabolabs.openehr.rm_1_0_2.support.identification.*
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvText
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.DvQuantity
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.Element
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemTree
import com.cabolabs.openehr.rm_1_0_2.common.generic.PartySelf
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Archetyped

import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.*
import com.cabolabs.openehr.rm_1_0_2.demographic.*
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.*
import com.cabolabs.openehr.rm_1_0_2.data_types.basic.*

import com.cabolabs.openehr.validation.*

import com.cabolabs.openehr.opt.manager.*

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

   void testPartyRelationshipDto()
   {
      def rel = new PartyRelationshipDto(
         name: new DvText(
            value: 'relationship'
         ),
         archetype_node_id: 'openEHR-DEMOGRAPHIC-PARTY_RELATIONSHIP.test.v1',
         archetype_details: new Archetyped(
            archetype_id: new ArchetypeId(
               value: 'openEHR-DEMOGRAPHIC-PARTY_RELATIONSHIP.test.v1'
            ),
            template_id: new TemplateId(
               value: 'test_rel'
            ),
            rm_version: '1.0.2'
         ),
         details: new ItemTree(
            name: new DvText(
               value: 'tree'
            ),
            archetype_node_id: 'at0001', // << FIXME: change structure to comply with archetype
            items: [
               new Element(
                  name: new DvText(
                     value: 'identifier'
                  ),
                  archetype_node_id: 'at0002',
                  value: new DvIdentifier(
                     id: 'A123',
                     issuer: 'Hospital X',
                     type: 'test1',
                     assigner: 'Hospital X'
                  )
               )
            ]
         ),
         time_validity: new DvInterval(
            lower: new DvDate(
               value: '2020-01-01'
            ),
            lower_included: true,
            lower_unbounded: false,
            upper_included: false,
            upper_unbounded: true
         ),
         source: new PersonDto( // The RoleDto has the ActorDto directly, not a REF
            name: new DvText(
               value: 'generic person'
            ),
            uid: new HierObjectId(
               value: '40329c20-39a8-4c10-8282-6d9b66c37999' // this should be a version object id
            ),
            archetype_node_id: 'openEHR-DEMOGRAPHIC-PERSON.generic_person.v1',
            archetype_details: new Archetyped(
               archetype_id: new ArchetypeId(
                  value: 'openEHR-DEMOGRAPHIC-PERSON.generic_person.v1'
               ),
               template_id: new TemplateId(
                  value: 'generic_person'
               ),
               rm_version: '1.0.2'
            ),
            //
            // Won't have roles because it's contained in the role, though we could add some to check how the serialization behaves
            //
            languages: [
               new DvText("es")
            ],
            details: new ItemTree(
               name: new DvText(
                  value: 'tree'
               ),
               archetype_node_id: 'at0001', // << FIXME: change structure to comply with archetype
               items: [
                  new Element(
                     name: new DvText(
                        value: 'identifier'
                     ),
                     archetype_node_id: 'at0002',
                     value: new DvIdentifier(
                        id: 'PERSON1234',
                        issuer: 'Hospital X',
                        type: 'PERSON_ID'
                     )
                  )
               ]
            ),
            identities: [
               new PartyIdentity(
                  archetype_node_id: 'at0003',
                  name: new DvText(
                     value: 'Name'
                  ),
                  details: new ItemTree(
                     name: new DvText(
                        value: 'tree'
                     ),
                     archetype_node_id: 'at0004', // << FIXME: change structure to comply with archetype
                     items: [
                        new Element(
                           name: new DvText(
                              value: 'Full name'
                           ),
                           archetype_node_id: 'at0005',
                           value: new DvText("Pablo Pazos")
                        )
                     ]
                  )
               )
            ]
         ),
         target: new PersonDto( // The RoleDto has the ActorDto directly, not a REF
            name: new DvText(
               value: 'generic person'
            ),
            uid: new HierObjectId(
               value: '40329c20-39a8-4c10-8282-6d9b66c37444' // this should be a version object id
            ),
            archetype_node_id: 'openEHR-DEMOGRAPHIC-PERSON.generic_person.v1',
            archetype_details: new Archetyped(
               archetype_id: new ArchetypeId(
                  value: 'openEHR-DEMOGRAPHIC-PERSON.generic_person.v1'
               ),
               template_id: new TemplateId(
                  value: 'generic_person'
               ),
               rm_version: '1.0.2'
            ),
            //
            // Won't have roles because it's contained in the role, though we could add some to check how the serialization behaves
            //
            languages: [
               new DvText("es")
            ],
            details: new ItemTree(
               name: new DvText(
                  value: 'tree'
               ),
               archetype_node_id: 'at0001', // << FIXME: change structure to comply with archetype
               items: [
                  new Element(
                     name: new DvText(
                        value: 'identifier'
                     ),
                     archetype_node_id: 'at0002',
                     value: new DvIdentifier(
                        id: 'PERSON1234',
                        issuer: 'Hospital X',
                        type: 'PERSON_ID'
                     )
                  )
               ]
            ),
            identities: [
               new PartyIdentity(
                  archetype_node_id: 'at0003',
                  name: new DvText(
                     value: 'Name'
                  ),
                  details: new ItemTree(
                     name: new DvText(
                        value: 'tree'
                     ),
                     archetype_node_id: 'at0004', // << FIXME: change structure to comply with archetype
                     items: [
                        new Element(
                           name: new DvText(
                              value: 'Full name'
                           ),
                           archetype_node_id: 'at0005',
                           value: new DvText("Esther Kuriaky")
                        )
                     ]
                  )
               )
            ]
         )
      )

      def serializer = new OpenEhrJsonSerializer()
      def string = serializer.serialize(rel)
      println string

      def parser = new OpenEhrJsonParserQuick(false)
      parser.setSchemaFlavorAPI()
      def rel_out = parser.parseJson(string)

      assert rel_out.source instanceof PersonDto
      assert rel_out.target instanceof PersonDto
   }

   void testRoleSerialization()
   {
      def role = new Role(
         name: new DvText(
            value: 'generic role'
         ),
         uid: new HierObjectId(
            value: '40329c20-39a8-4c10-8282-6d9b66c372fd'
         ),
         archetype_node_id: 'openEHR-DEMOGRAPHIC-ROLE.generic_role_with_capabilities.v1',
         archetype_details: new Archetyped(
            archetype_id: new ArchetypeId(
               value: 'openEHR-DEMOGRAPHIC-ROLE.generic_role_with_capabilities.v1'
            ),
            template_id: new TemplateId(
               value: 'generic_role_complete'
            ),
            rm_version: '1.0.2'
         ),
         time_validity: new DvInterval(
            lower: new DvDate(
               value: '2020-01-01'
            ),
            lower_included: true,
            lower_unbounded: false,
            upper_included: false,
            upper_unbounded: true
         ),
         performer: new PartyRef(
            namespace: 'demographic',
            type: 'PERSON',
            id: new HierObjectId(
               value: '0884624d-a748-4510-a342-c93f98afd853'
            )
         ),
         details: new ItemTree(
            name: new DvText(
               value: 'tree'
            ),
            archetype_node_id: 'at0001', // << FIXME: change structure to comply with archetype
            items: [
               new Element(
                  name: new DvText(
                     value: 'identifier'
                  ),
                  archetype_node_id: 'at0002',
                  value: new DvIdentifier(
                     id: 'A123',
                     issuer: 'Hospital X',
                     type: 'test1',
                     assigner: 'Hospital X'
                  )
               )
            ]
         ),
         identities: [
            new PartyIdentity(
               name: new DvText(
                  value: 'identity'
               ),
               archetype_node_id: 'at0004',
               details: new ItemTree(
                  name: new DvText(
                     value: 'tree'
                  ),
                  archetype_node_id: 'at0005', // << change structure to comply with archetype
                  items: [
                     new Element(
                        name: new DvText(
                           value: 'name'
                        ),
                        archetype_node_id: 'at0006',
                        value: new DvText(
                           value: 'patient'
                        )
                     )
                  ]
               )
            )
         ],
         capabilities: [
            new Capability(
               name: new DvText(
                  value: 'capability'
               ),
               archetype_node_id: 'at0008',
               time_validity: new DvInterval(
                  lower: new DvDate(
                     value: '2020-01-01'
                  ),
                  lower_included: true,
                  lower_unbounded: false,
                  upper_included: false,
                  upper_unbounded: true
               ),
               credentials: new ItemTree(
                  name: new DvText(
                     value: 'tree'
                  ),
                  archetype_node_id: 'at0009',
                  items: [
                     new Element(
                        name: new DvText(
                           value: 'capability name'
                        ),
                        archetype_node_id: 'at0010',
                        value: new DvText(
                           value: "doctor"
                        )
                     )
                  ]
               )
            )
         ]
      )

      def serializer = new OpenEhrJsonSerializer()
      def string = serializer.serialize(role)
      println string

      def slurper = new JsonSlurper()
      def json_map = slurper.parseText(string)

      def validator = new JsonInstanceValidation('rm', '1.0.2')
      def errors = validator.validate(json_map)

      println errors

      assert !errors

      def parser = new OpenEhrJsonParserQuick(true) // true validates against JSON Schema
      // parser.setSchemaFlavorAPI()
      def role_out = parser.parseJson(string)


      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR (EHR_STATUS only)
      RmValidator2 rm_validator = new RmValidator2(opt_manager)
      RmValidationReport report = rm_validator.dovalidate(role_out, 'com.cabolabs.openehr_opt.namespaces.default')

      assert !report.errors
   }

   void testRoleDtoSerialization()
   {
      def role = new RoleDto(
         name: new DvText(
            value: 'generic role'
         ),
         uid: new ObjectVersionId(
            value: '40329c20-39a8-4c10-8282-6d9b66c372fd::ATOMIK::1'
         ),
         archetype_node_id: 'openEHR-DEMOGRAPHIC-ROLE.generic_role_with_capabilities.v1',
         archetype_details: new Archetyped(
            archetype_id: new ArchetypeId(
               value: 'openEHR-DEMOGRAPHIC-ROLE.generic_role_with_capabilities.v1'
            ),
            template_id: new TemplateId(
               value: 'generic_role_complete'
            ),
            rm_version: '1.0.2'
         ),
         time_validity: new DvInterval(
            lower: new DvDate(
               value: '2020-01-01'
            ),
            lower_included: true,
            lower_unbounded: false,
            upper_included: false,
            upper_unbounded: true
         ),
         performer: new PartyRef(
            namespace: 'DEMOGRAPHIC',
            type: 'PERSON',
            id: new ObjectVersionId(
               value: '40329c20-39a8-4c10-8282-6d9b66c37999::ATOMIK::1'
            )
         ),
         details: new ItemTree(
            name: new DvText(
               value: 'tree'
            ),
            archetype_node_id: 'at0001', // << FIXME: change structure to comply with archetype
            items: [
               new Element(
                  name: new DvText(
                     value: 'identifier'
                  ),
                  archetype_node_id: 'at0002',
                  value: new DvIdentifier(
                     id: 'ROLE123',
                     issuer: 'Hospital X',
                     type: 'test1',
                     assigner: 'Hospital X'
                  )
               )
            ]
         ),
         identities: [
            new PartyIdentity(
               name: new DvText(
                  value: 'identity'
               ),
               archetype_node_id: 'at0004',
               details: new ItemTree(
                  name: new DvText(
                     value: 'tree'
                  ),
                  archetype_node_id: 'at0005', // << change structure to comply with archetype
                  items: [
                     new Element(
                        name: new DvText(
                           value: 'name'
                        ),
                        archetype_node_id: 'at0006',
                        value: new DvText(
                           value: 'patient'
                        )
                     )
                  ]
               )
            )
         ],
         capabilities: [
            new Capability(
               name: new DvText(
                  value: 'capability'
               ),
               archetype_node_id: 'at0008',
               time_validity: new DvInterval(
                  lower: new DvDate(
                     value: '2020-01-01'
                  ),
                  lower_included: true,
                  lower_unbounded: false,
                  upper_included: false,
                  upper_unbounded: true
               ),
               credentials: new ItemTree(
                  name: new DvText(
                     value: 'tree'
                  ),
                  archetype_node_id: 'at0009',
                  items: [
                     new Element(
                        name: new DvText(
                           value: 'capability name'
                        ),
                        archetype_node_id: 'at0010',
                        value: new DvText(
                           value: "doctor"
                        )
                     )
                  ]
               )
            )
         ]
      )

      def serializer = new OpenEhrJsonSerializer()
      def string = serializer.serialize(role)
      println string

      def slurper = new JsonSlurper()
      def json_map = slurper.parseText(string)

      def validator = new JsonInstanceValidation('api', '1.0.2')
      def errors = validator.validate(json_map)

      println errors

      assert !errors

      def parser = new OpenEhrJsonParserQuick(true) // true validates against JSON Schema
      parser.setSchemaFlavorAPI()
      def role_out = parser.parseJson(string)


      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR (EHR_STATUS only)
      RmValidator2 rm_validator = new RmValidator2(opt_manager)
      RmValidationReport report = rm_validator.dovalidate(role_out, 'com.cabolabs.openehr_opt.namespaces.default')

      assert !report.errors
   }

   /*
   new PersonDto( // The RoleDto has the ActorDto directly, not a REF
      name: new DvText(
         value: 'generic person'
      ),
      uid: new HierObjectId(
         value: '40329c20-39a8-4c10-8282-6d9b66c37999' // this should be a version object id
      ),
      archetype_node_id: 'openEHR-DEMOGRAPHIC-PERSON.generic_person.v1',
      archetype_details: new Archetyped(
         archetype_id: new ArchetypeId(
            value: 'openEHR-DEMOGRAPHIC-PERSON.generic_person.v1'
         ),
         template_id: new TemplateId(
            value: 'generic_person'
         ),
         rm_version: '1.0.2'
      ),
      //
      // Won't have roles because it's contained in the role, though we could add some to check how the serialization behaves
      //
      languages: [
         new DvText("es")
      ],
      details: new ItemTree(
         name: new DvText(
            value: 'tree'
         ),
         archetype_node_id: 'at0001', // << FIXME: change structure to comply with archetype
         items: [
            new Element(
               name: new DvText(
                  value: 'identifier'
               ),
               archetype_node_id: 'at0002',
               value: new DvIdentifier(
                  id: 'PERSON1234',
                  issuer: 'Hospital X',
                  type: 'PERSON_ID'
               )
            )
         ]
      ),
      identities: [
         new PartyIdentity(
            archetype_node_id: 'at0003',
            name: new DvText(
               value: 'Name'
            ),
            details: new ItemTree(
               name: new DvText(
                  value: 'tree'
               ),
               archetype_node_id: 'at0004', // << FIXME: change structure to comply with archetype
               items: [
                  new Element(
                     name: new DvText(
                        value: 'Full name'
                     ),
                     archetype_node_id: 'at0005',
                     value: new DvText("Pablo Pazos")
                  )
               ]
            )
         )
      ]
   )
   */
}