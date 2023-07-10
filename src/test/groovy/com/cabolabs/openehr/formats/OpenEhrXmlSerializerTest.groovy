package com.cabolabs.openehr.formats

import groovy.util.GroovyTestCase
import com.cabolabs.openehr.dto_1_0_2.ehr.EhrDto
import com.cabolabs.openehr.rm_1_0_2.ehr.EhrStatus
import com.cabolabs.openehr.rm_1_0_2.common.generic.PartySelf
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Archetyped
import com.cabolabs.openehr.rm_1_0_2.support.identification.*
import com.cabolabs.openehr.rm_1_0_2.data_types.basic.*
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.*
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.*
import com.cabolabs.openehr.rm_1_0_2.data_types.text.*
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemTree
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.*

import com.cabolabs.openehr.rm_1_0_2.demographic.*

import com.cabolabs.openehr.validation.*

import com.cabolabs.openehr.opt.manager.*
import com.cabolabs.openehr.opt.instance_validation.*

class OpenEhrXmlSerializerTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")

   void testPersonSerialize()
   {
      def person = new Person(
         name: new DvText(
            value: 'person_complete'
         ),
         archetype_details: new Archetyped(
            archetype_id: new ArchetypeId(
               value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
            ),
            template_id: new TemplateId(
               value: 'person_complete'
            ),
            rm_version: '1.0.2'
         ),
         archetype_node_id: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0',
         roles: [
            // TODO: for Person, roles are PartyRef, for PersonDTP are Role
         ],
         languages: [
            new DvText(value: 'es')
         ],
         details: new ItemTree(
            name: new DvText(
               value: 'Tree'
            ),
            archetype_details: new Archetyped(
               archetype_id: new ArchetypeId(
                  value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
               ),
               template_id: new TemplateId(
                  value: 'person_complete'
               ),
               rm_version: '1.0.2'
            ),
            archetype_node_id: 'at0037',
            items: [
               new Cluster(
                  name: new DvText(
                     value: 'Identifiers'
                  ),
                  archetype_details: new Archetyped(
                     archetype_id: new ArchetypeId(
                        value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                     ),
                     template_id: new TemplateId(
                        value: 'person_complete'
                     ),
                     rm_version: '1.0.2'
                  ),
                  archetype_node_id: 'at0010',
                  items: [
                     new Element(
                        name: new DvText(
                           value: 'Identifier'
                        ),
                        archetype_details: new Archetyped(
                           archetype_id: new ArchetypeId(
                              value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                           ),
                           template_id: new TemplateId(
                              value: 'person_complete'
                           ),
                           rm_version: '1.0.2'
                        ),
                        archetype_node_id: 'at0011',
                        value: new DvIdentifier(
                           issuer: 'Hospital X',
                           assigner: 'Hospital X',
                           id: '1234545545',
                           type: 'MRN'
                        )
                     )
                  ]
               )
            ]
         ),
         contacts: [
            new Contact(
               name: new DvText(
                  value: 'Contact means'
               ),
               archetype_details: new Archetyped(
                  archetype_id: new ArchetypeId(
                     value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                  ),
                  template_id: new TemplateId(
                     value: 'person_complete'
                  ),
                  rm_version: '1.0.2'
               ),
               archetype_node_id: 'at0004',
               time_validity: new DvInterval(
                  lower: new DvDate(
                     value: '2023-01-01'
                  ),
                  upper_unbounded: true,
                  lower_included: true
               ),
               addresses: [
                  new Address(
                     name: new DvText(
                        value: 'Home address'
                     ),
                     archetype_details: new Archetyped(
                        archetype_id: new ArchetypeId(
                           value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                        ),
                        template_id: new TemplateId(
                           value: 'person_complete'
                        ),
                        rm_version: '1.0.2'
                     ),
                     archetype_node_id: 'at0005',
                     details: new ItemTree(
                        name: new DvText(
                           value: 'Tree'
                        ),
                        archetype_details: new Archetyped(
                           archetype_id: new ArchetypeId(
                              value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                           ),
                           template_id: new TemplateId(
                              value: 'person_complete'
                           ),
                           rm_version: '1.0.2'
                        ),
                        archetype_node_id: 'at0039',
                        items: [
                           new Element(
                              name: new DvText(
                                 value: 'Street address'
                              ),
                              archetype_details: new Archetyped(
                                 archetype_id: new ArchetypeId(
                                    value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                                 ),
                                 template_id: new TemplateId(
                                    value: 'person_complete'
                                 ),
                                 rm_version: '1.0.2'
                              ),
                              archetype_node_id: 'at0006',
                              value: new DvText(
                                 value: 'Miguel Barreiro 3285'
                              )
                           ),
                           new Element(
                              name: new DvText(
                                 value: 'City'
                              ),
                              archetype_details: new Archetyped(
                                 archetype_id: new ArchetypeId(
                                    value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                                 ),
                                 template_id: new TemplateId(
                                    value: 'person_complete'
                                 ),
                                 rm_version: '1.0.2'
                              ),
                              archetype_node_id: 'at0015',
                              value: new DvText(
                                 value: 'Montevideo'
                              )
                           ),
                           new Element(
                              name: new DvText(
                                 value: 'State / province / department'
                              ),
                              archetype_details: new Archetyped(
                                 archetype_id: new ArchetypeId(
                                    value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                                 ),
                                 template_id: new TemplateId(
                                    value: 'person_complete'
                                 ),
                                 rm_version: '1.0.2'
                              ),
                              archetype_node_id: 'at0016',
                              value: new DvText(
                                 value: 'Montevideo'
                              )
                           ),
                           new Element(
                              name: new DvText(
                                 value: 'Postal code'
                              ),
                              archetype_details: new Archetyped(
                                 archetype_id: new ArchetypeId(
                                    value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                                 ),
                                 template_id: new TemplateId(
                                    value: 'person_complete'
                                 ),
                                 rm_version: '1.0.2'
                              ),
                              archetype_node_id: 'at0017',
                              value: new DvText(
                                 value: '11300'
                              )
                           ),
                           new Element(
                              name: new DvText(
                                 value: 'Country'
                              ),
                              archetype_details: new Archetyped(
                                 archetype_id: new ArchetypeId(
                                    value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                                 ),
                                 template_id: new TemplateId(
                                    value: 'person_complete'
                                 ),
                                 rm_version: '1.0.2'
                              ),
                              archetype_node_id: 'at0018',
                              value: new DvText(
                                 value: 'Uruguay'
                              )
                           )
                        ]
                     )
                  ),
                  new Address(
                     name: new DvText(
                        value: 'Work address'
                     ),
                     archetype_details: new Archetyped(
                        archetype_id: new ArchetypeId(
                           value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                        ),
                        template_id: new TemplateId(
                           value: 'person_complete'
                        ),
                        rm_version: '1.0.2'
                     ),
                     archetype_node_id: 'at0022',
                     details: new ItemTree(
                        name: new DvText(
                           value: 'Tree'
                        ),
                        archetype_details: new Archetyped(
                           archetype_id: new ArchetypeId(
                              value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                           ),
                           template_id: new TemplateId(
                              value: 'person_complete'
                           ),
                           rm_version: '1.0.2'
                        ),
                        archetype_node_id: 'at0040',
                        items: [
                           new Element(
                              name: new DvText(
                                 value: 'Street address'
                              ),
                              archetype_details: new Archetyped(
                                 archetype_id: new ArchetypeId(
                                    value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                                 ),
                                 template_id: new TemplateId(
                                    value: 'person_complete'
                                 ),
                                 rm_version: '1.0.2'
                              ),
                              archetype_node_id: 'at0023',
                              value: new DvText(
                                 value: 'Juan Paullier 995'
                              )
                           ),
                           new Element(
                              name: new DvText(
                                 value: 'City'
                              ),
                              archetype_details: new Archetyped(
                                 archetype_id: new ArchetypeId(
                                    value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                                 ),
                                 template_id: new TemplateId(
                                    value: 'person_complete'
                                 ),
                                 rm_version: '1.0.2'
                              ),
                              archetype_node_id: 'at0024',
                              value: new DvText(
                                 value: 'Montevideo'
                              )
                           ),
                           new Element(
                              name: new DvText(
                                 value: 'State / province / department'
                              ),
                              archetype_details: new Archetyped(
                                 archetype_id: new ArchetypeId(
                                    value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                                 ),
                                 template_id: new TemplateId(
                                    value: 'person_complete'
                                 ),
                                 rm_version: '1.0.2'
                              ),
                              archetype_node_id: 'at0025',
                              value: new DvText(
                                 value: 'Montevideo'
                              )
                           ),
                           new Element(
                              name: new DvText(
                                 value: 'Postal code'
                              ),
                              archetype_details: new Archetyped(
                                 archetype_id: new ArchetypeId(
                                    value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                                 ),
                                 template_id: new TemplateId(
                                    value: 'person_complete'
                                 ),
                                 rm_version: '1.0.2'
                              ),
                              archetype_node_id: 'at0026',
                              value: new DvText(
                                 value: '11200'
                              )
                           ),
                           new Element(
                              name: new DvText(
                                 value: 'Country'
                              ),
                              archetype_details: new Archetyped(
                                 archetype_id: new ArchetypeId(
                                    value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                                 ),
                                 template_id: new TemplateId(
                                    value: 'person_complete'
                                 ),
                                 rm_version: '1.0.2'
                              ),
                              archetype_node_id: 'at0027',
                              value: new DvText(
                                 value: 'Uruguay'
                              )
                           )
                        ]
                     )
                  ),
                  new Address(
                     name: new DvText(
                        value: 'Phone number'
                     ),
                     archetype_details: new Archetyped(
                        archetype_id: new ArchetypeId(
                           value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                        ),
                        template_id: new TemplateId(
                           value: 'person_complete'
                        ),
                        rm_version: '1.0.2'
                     ),
                     archetype_node_id: 'at0028',
                     details: new ItemTree(
                        name: new DvText(
                           value: 'Tree'
                        ),
                        archetype_details: new Archetyped(
                           archetype_id: new ArchetypeId(
                              value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                           ),
                           template_id: new TemplateId(
                              value: 'person_complete'
                           ),
                           rm_version: '1.0.2'
                        ),
                        archetype_node_id: 'at0041',
                        items: [
                           new Element(
                              name: new DvText(
                                 value: 'Number'
                              ),
                              archetype_details: new Archetyped(
                                 archetype_id: new ArchetypeId(
                                    value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                                 ),
                                 template_id: new TemplateId(
                                    value: 'person_complete'
                                 ),
                                 rm_version: '1.0.2'
                              ),
                              archetype_node_id: 'at0029',
                              value: new DvText(
                                 value: '555 034 145'
                              )
                           ),
                           new Element(
                              name: new DvText(
                                 value: 'Country code'
                              ),
                              archetype_details: new Archetyped(
                                 archetype_id: new ArchetypeId(
                                    value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                                 ),
                                 template_id: new TemplateId(
                                    value: 'person_complete'
                                 ),
                                 rm_version: '1.0.2'
                              ),
                              archetype_node_id: 'at0030',
                              value: new DvText(
                                 value: '+598'
                              )
                           )
                        ]
                     )
                  ),
                  new Address(
                     name: new DvText(
                        value: 'Email'
                     ),
                     archetype_details: new Archetyped(
                        archetype_id: new ArchetypeId(
                           value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                        ),
                        template_id: new TemplateId(
                           value: 'person_complete'
                        ),
                        rm_version: '1.0.2'
                     ),
                     archetype_node_id: 'at0031',
                     details: new ItemTree(
                        name: new DvText(
                           value: 'Tree'
                        ),
                        archetype_details: new Archetyped(
                           archetype_id: new ArchetypeId(
                              value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                           ),
                           template_id: new TemplateId(
                              value: 'person_complete'
                           ),
                           rm_version: '1.0.2'
                        ),
                        archetype_node_id: 'at0042',
                        items: [
                           new Element(
                              name: new DvText(
                                 value: 'Email address'
                              ),
                              archetype_details: new Archetyped(
                                 archetype_id: new ArchetypeId(
                                    value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                                 ),
                                 template_id: new TemplateId(
                                    value: 'person_complete'
                                 ),
                                 rm_version: '1.0.2'
                              ),
                              archetype_node_id: 'at0032',
                              value: new DvText(
                                 value: 'info@cabolabs.com'
                              )
                           )
                        ]
                     )
                  )
               ]
            )
         ],
         identities: [
            new PartyIdentity(
               name: new DvText(
                  value: 'Identifier'
               ),
               archetype_details: new Archetyped(
                  archetype_id: new ArchetypeId(
                     value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                  ),
                  template_id: new TemplateId(
                     value: 'person_complete'
                  ),
                  rm_version: '1.0.2'
               ),
               archetype_node_id: 'at0003',
               details: new ItemTree(
                  name: new DvText(
                     value: 'Identifier'
                  ),
                  archetype_details: new Archetyped(
                     archetype_id: new ArchetypeId(
                        value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                     ),
                     template_id: new TemplateId(
                        value: 'person_complete'
                     ),
                     rm_version: '1.0.2'
                  ),
                  archetype_node_id: 'at0038',
                  items: [
                     new Element(
                        name: new DvText(
                           value: 'Full name'
                        ),
                        archetype_details: new Archetyped(
                           archetype_id: new ArchetypeId(
                              value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                           ),
                           template_id: new TemplateId(
                              value: 'person_complete'
                           ),
                           rm_version: '1.0.2'
                        ),
                        archetype_node_id: 'at0007',
                        value: new DvText(
                           value: 'Pablo Pazos'
                        )
                     ),
                     new Element(
                        name: new DvText(
                           value: 'Date of birth'
                        ),
                        archetype_details: new Archetyped(
                           archetype_id: new ArchetypeId(
                              value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                           ),
                           template_id: new TemplateId(
                              value: 'person_complete'
                           ),
                           rm_version: '1.0.2'
                        ),
                        archetype_node_id: 'at0008',
                        value: new DvDate(
                           value: '2023-07-09'
                        )
                     ),
                     new Element(
                        name: new DvText(
                           value: 'Sex'
                        ),
                        archetype_details: new Archetyped(
                           archetype_id: new ArchetypeId(
                              value: 'openEHR-DEMOGRAPHIC-PERSON.person_complete.v0'
                           ),
                           template_id: new TemplateId(
                              value: 'person_complete'
                           ),
                           rm_version: '1.0.2'
                        ),
                        archetype_node_id: 'at0009',
                        value: new DvCodedText(
                           value: 'Masculine',
                           defining_code: new CodePhrase(
                              code_string: 'at0033',
                              terminology_id: new TerminologyId(
                                 value: 'local'
                              )
                           )
                        )
                     )
                  ]
               )
            )
         ],
         relationships: [
            new PartyRelationship(
               name: new DvText(
                  value: 'generic relationship'
               ),
               archetype_details: new Archetyped(
                  archetype_id: new ArchetypeId(
                     value: 'openEHR-DEMOGRAPHIC-PARTY_RELATIONSHIP.generic_relationship.v1'
                  ),
                  template_id: new TemplateId(
                     value: 'generic_relationship'
                  ),
                  rm_version: '1.0.2'
               ),
               archetype_node_id: 'openEHR-DEMOGRAPHIC-PARTY_RELATIONSHIP.generic_relationship.v1',
               details: new ItemTree(
                  name: new DvText(
                     value: 'tree'
                  ),
                  archetype_details: new Archetyped(
                     archetype_id: new ArchetypeId(
                        value: 'openEHR-DEMOGRAPHIC-PARTY_RELATIONSHIP.generic_relationship.v1'
                     ),
                     template_id: new TemplateId(
                        value: 'generic_relationship'
                     ),
                     rm_version: '1.0.2'
                  ),
                  archetype_node_id: 'at0001',
                  items: [
                     new Element(
                        name: new DvText(
                           value: 'relationship type'
                        ),
                        archetype_details: new Archetyped(
                           archetype_id: new ArchetypeId(
                              value: 'openEHR-DEMOGRAPHIC-PARTY_RELATIONSHIP.generic_relationship.v1'
                           ),
                           template_id: new TemplateId(
                              value: 'generic_relationship'
                           ),
                           rm_version: '1.0.2'
                        ),
                        archetype_node_id: 'at0002',
                        value: new DvCodedText(
                           value: 'Natural child',
                           defining_code: new CodePhrase(
                              code_string: '75226009',
                              terminology_id: new TerminologyId(
                                 value: 'SNOMED-CT'
                              )
                           )
                        )
                     )
                  ]
               ),
               time_validity: new DvInterval(
                  lower: new DvDate(
                     value: '2023-01-01'
                  ),
                  upper_unbounded: true,
                  lower_included: true
               ),
               source: new PartyRef(
                  id: new HierObjectId(
                     value: '1234-1234-1234-1234'
                  ),
                  type: 'PERSON',
                  namespace: 'demographic'
               ),
               target: new PartyRef(
                  id: new HierObjectId(
                     value: '5678-1234-1234-1234'
                  ),
                  type: 'PERSON',
                  namespace: 'demographic'
               )
            )
         ]
      )

      def serializer = new OpenEhrXmlSerializer(true)
      def personString = serializer.serialize(person)

      println personString



      //def slurper = new XmlSlurper(false, false)
      //def gpath = slurper.parseText(personString)

      // NOTE: favour should be 'api' and current validator doesn't have an option for that, we neither have the XSD for that.
      def inputStream = getClass().getResourceAsStream('/xsd/Version.xsd')
      def schemaValidator = new XmlValidation(inputStream)
      if (!schemaValidator.validate(personString))
      {
         println schemaValidator.getErrors()
      }
   }

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

      def serializer = new OpenEhrXmlSerializer()
      def ehr_string = serializer.serialize(dto_ehr)

      /* TODO: validate XML against API schema

      def slurper = new XmlSlurper(false, false)
      def gpath = slurper.parseText(xml)

      // NOTE: favour should be 'api' and current validator doesn't have an option for that, we neither have the XSD for that.
      def inputStream = getClass().getResourceAsStream('/xsd/Version.xsd')
      schemaValidator = new XmlValidation(inputStream)
      if (!schemaValidator.validate(xml))
      {
         println schemaValidator.getErrors()
      }
      */
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

      def serializer = new OpenEhrXmlSerializer()
      def string = serializer.serialize(role)
      println string

      /* TODO: XML
      def slurper = new JsonSlurper()
      def json_map = slurper.parseText(string)

      def validator = new JsonInstanceValidation('api', '1.0.2')
      def errors = validator.validate(json_map)

      println errors

      assert !errors
      */

      /* TODO: XML
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
      */
   }
}