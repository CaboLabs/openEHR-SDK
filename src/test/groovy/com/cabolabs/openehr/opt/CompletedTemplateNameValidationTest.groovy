package com.cabolabs.openehr.opt

import groovy.util.GroovyTestCase
import com.cabolabs.openehr.formats.OpenEhrJsonParser
import com.cabolabs.openehr.formats.OpenEhrJsonParserQuick
import com.cabolabs.openehr.dto_1_0_2.demographic.ActorDto
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import com.cabolabs.openehr.rm_1_0_2.ehr.EhrStatus
import com.cabolabs.openehr.validation.*
import com.cabolabs.openehr.opt.manager.*

/**
 * EHRServerNG loads OPTs into the shared OptManager cache with complete=true
 * (OperationalTemplateIndexerService.onOptStored, OptFSService.loadAll, etc.),
 * not the plain load() the rest of the SDK test suite uses.
 *
 * For a Locatable whose name has no explicit <rm_attribute_name>name</rm_attribute_name>
 * C_ATTRIBUTE in the OPT (the common case: the fixed value is implicit, bound via
 * the node's own node_id to a term_definitions entry with a matching code),
 * OperationalTemplate.complete() used to inject an unconstrained placeholder
 * instead of resolving that implicit constraint - which meant any name value
 * validated once the template had gone through complete(), silently. Fixed in
 * OperationalTemplate.completeNodeSingle(): for attr=='name'/type=='DV_TEXT' it
 * now resolves ownerArchetypeRoot.getText(nodeId) and materializes a real
 * fixed-value constraint, same as archetypes that do declare it explicitly.
 * These tests pin that fix down, and would fail again if it regressed.
 */
class CompletedTemplateNameValidationTest extends GroovyTestCase {

   void testPersonCompleteImplicitNameConstraints_completedTemplate_stillCaught()
   {
      // person_complete.opt has ZERO explicit <rm_attribute_name>name</rm_attribute_name>
      // constraints anywhere - confirmed by grep. Root PERSON, ITEM_TREE (details),
      // CLUSTER, CONTACT, ADDRESS, PARTY_IDENTITY all rely on the implicit
      // node_id -> term_definitions text constraint.
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)

      // force a fresh, completed load - same call EHRServerNG's
      // OperationalTemplateIndexerService.onOptStored makes.
      opt_manager.load('person_complete', 'com.cabolabs.openehr_opt.namespaces.default', true)

      String json = '''
      {
          "_type": "PERSON",
          "name": { "_type": "DV_TEXT", "value": "TOTALLY WRONG PERSON NAME" },
          "archetype_details": {
              "archetype_id": { "value": "openEHR-DEMOGRAPHIC-PERSON.person_complete.v0" },
              "template_id": { "value": "person_complete" },
              "rm_version": "1.0.2"
          },
          "archetype_node_id": "openEHR-DEMOGRAPHIC-PERSON.person_complete.v0",
          "details": {
              "_type": "ITEM_TREE",
              "name": { "_type": "DV_TEXT", "value": "Treex" },
              "archetype_node_id": "at0037",
              "items": [
                  {
                      "_type": "CLUSTER",
                      "name": { "_type": "DV_TEXT", "value": "Identifiersxxx" },
                      "archetype_node_id": "at0010",
                      "items": [
                          {
                              "_type": "ELEMENT",
                              "name": { "_type": "DV_TEXT", "value": "Identifierxxx" },
                              "archetype_node_id": "at0011",
                              "value": { "_type": "DV_IDENTIFIER", "id": "101010", "type": "MRI" }
                          }
                      ]
                  }
              ]
          },
          "contacts": [
              {
                  "_type": "CONTACT",
                  "name": { "_type": "DV_TEXT", "value": "Contact meansxxx" },
                  "archetype_node_id": "at0004",
                  "time_validity": {
                      "lower": { "_type": "DV_DATE", "value": "2023-07-17" },
                      "lower_included": true, "lower_unbounded": false,
                      "upper_included": false, "upper_unbounded": true
                  },
                  "addresses": [
                      {
                          "_type": "ADDRESS",
                          "name": { "_type": "DV_TEXT", "value": "Home addressxxx" },
                          "archetype_node_id": "at0005",
                          "details": {
                              "_type": "ITEM_TREE",
                              "name": { "_type": "DV_TEXT", "value": "Treexxx" },
                              "archetype_node_id": "at0039",
                              "items": [
                                  {
                                      "_type": "ELEMENT",
                                      "name": { "_type": "DV_TEXT", "value": "Street addressxxx" },
                                      "archetype_node_id": "at0006",
                                      "value": { "_type": "DV_TEXT", "value": "Miguel Barreiro 3285" }
                                  }
                              ]
                          }
                      }
                  ]
              }
          ],
          "identities": [
              {
                  "_type": "PARTY_IDENTITY",
                  "name": { "_type": "DV_TEXT", "value": "Identityxxx" },
                  "archetype_node_id": "at0003",
                  "details": {
                      "_type": "ITEM_TREE",
                      "name": { "_type": "DV_TEXT", "value": "Tree" },
                      "archetype_node_id": "at0038",
                      "items": [
                          {
                              "_type": "ELEMENT",
                              "name": { "_type": "DV_TEXT", "value": "Full name" },
                              "archetype_node_id": "at0007",
                              "value": { "_type": "DV_TEXT", "value": "Pablo Pazos" }
                          }
                      ]
                  }
              }
          ]
      }
      '''

      def parser = new OpenEhrJsonParserQuick(true)
      parser.setSchemaFlavorAPI()
      ActorDto actor = parser.parseActorDto(json)

      assert actor

      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(actor, 'com.cabolabs.openehr_opt.namespaces.default')

      assert report.errors
      assert report.errors.find { it.dataPath == '/name/value/value' } // root PERSON.name
      assert report.errors.find { it.dataPath == '/details/name/value/value' } // ITEM_TREE
      assert report.errors.find { it.path == '/details/items(0)' } // CLUSTER
      assert report.errors.find { it.path == '/contacts(0)' } // CONTACT
      assert report.errors.find { it.path == '/identities(0)' } // PARTY_IDENTITY
   }

   void testPulseCompositionImplicitNameConstraints_completedTemplate_stillCaught()
   {
      // pulse.opt also has zero explicit name constraints, root COMPOSITION and
      // nested OBSERVATION rely on the implicit term-text constraint.
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/rm_validation").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)

      // cache key must match archetype_details.template_id.value exactly ("Pulse",
      // capital P) since getOpt()'s cache lookup does no normalization of its own -
      // only the on-disk filename lookup inside load() normalizes to "pulse.opt".
      opt_manager.load('Pulse', '', true)

      File file = new File(getClass().getResource("/rm_validation/pulsecomposition.json").toURI())
      def parser = new OpenEhrJsonParserQuick()
      Composition c = parser.parseJson(file.text)
      c.name.value = "TOTALLY WRONG COMPOSITION NAME"
      c.content[0].name.value = "TOTALLY WRONG OBSERVATION NAME"

      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(c, "")

      assert report.errors
      assert report.errors.find { it.dataPath == '/name/value/value' }
      assert report.errors.find { it.path == '/content(0)' }
   }

   void testEhrStatusImplicitNameConstraint_completedTemplate_stillCaught()
   {
      // ehr_status_any_en_v1.opt: tampering without complete() produces
      // "expected name is 'Generic Status' and actual name is 'X'" - the implicit
      // term-text path, confirming no explicit name constraint there either.
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)

      opt_manager.load('ehr_status_any_en_v1', 'com.cabolabs.openehr_opt.namespaces.default', true)

      String json_ehr_status = $/
         {
            "_type": "EHR_STATUS",
            "archetype_node_id": "openEHR-EHR-EHR_STATUS.generic.v1",
            "archetype_details": {
               "archetype_id": { "value": "openEHR-EHR-EHR_STATUS.any.v1" },
               "template_id": { "value": "ehr_status_any_en_v1" },
               "rm_version": "1.0.2"
            },
            "name": { "_type": "DV_TEXT", "value": "TOTALLY WRONG EHR_STATUS NAME" },
            "subject": {
               "external_ref": {
                  "id": { "_type": "GENERIC_ID", "value": "ins01", "scheme": "id_scheme" },
                  "namespace": "DEMOGRAPHIC",
                  "type": "PERSON"
               }
            },
            "is_modifiable": true,
            "is_queryable": true
         }
      /$

      def parser = new OpenEhrJsonParser(true)
      EhrStatus status = parser.parseJson(json_ehr_status)

      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(status, 'com.cabolabs.openehr_opt.namespaces.default')

      assert report.errors
      assert report.errors.find { it.dataPath == '/name/value/value' }
   }

   void testQuestionnaireExplicitNameConstraint_completedTemplate_stillCaught()
   {
      // Control case: life_style.opt (template_id "Life style", matching
      // questionnaire.json) HAS an explicit name constraint on the COMPOSITION
      // root (8 <rm_attribute_name>name</rm_attribute_name> occurrences total).
      // completeRecursive() only touches a node when it doesn't already have a
      // name attribute, so already-constrained nodes are unaffected by complete().
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/rm_validation").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)

      // cache key must match archetype_details.template_id.value exactly
      // ("Life style"), file on disk normalizes to life_style.opt.
      opt_manager.load('Life style', '', true)

      File file = new File(getClass().getResource("/rm_validation/questionnaire.json").toURI())
      def parser = new OpenEhrJsonParserQuick()
      Composition c = parser.parseJson(file.text)
      c.name.value = "TOTALLY WRONG NAME"

      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(c, "")

      assert report.errors
      assert report.errors.find { it.dataPath == '/name/value/value' }
   }

   // ===================================================================
   // Structural verification of OperationalTemplate.complete()'s name handling.
   // Inspects the ObjectNode tree directly, not just validation reports.
   // ===================================================================

   void testCompleteGuard_doesNotTouchNodeThatAlreadyHasNameConstraint()
   {
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/rm_validation").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)

      // fresh, NOT completed, load first to inspect the "before" state
      opt_manager.load('Life style', '', false)
      def opt = opt_manager.getOpt('Life style', '')

      def nameAttrBefore = opt.definition.getAttr('name')
      assert nameAttrBefore != null // life_style.opt DOES constrain the root name
      assert nameAttrBefore.children[0].getAttr('value') != null // has a real value constraint
      def valueListBefore = nameAttrBefore.children[0].getAttr('value').children[0].item.list

      assert valueListBefore == ['Life style']

      // now complete() the SAME already-parsed instance directly (no re-parse,
      // so any difference is attributable only to complete() itself)
      opt.complete()

      def nameAttrAfter = opt.definition.getAttr('name')

      // same object reference - complete() did not replace/duplicate it
      assert nameAttrAfter.is(nameAttrBefore)
      assert opt.definition.attributes.findAll{ it.rmAttributeName == 'name' }.size() == 1

      def valueListAfter = nameAttrAfter.children[0].getAttr('value').children[0].item.list
      assert valueListAfter == ['Life style'] // constraint value untouched
   }

   void testCompleteGuard_resolvesImplicitTermTextConstraintWhenNodeHasNone()
   {
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)

      opt_manager.load('person_complete', 'com.cabolabs.openehr_opt.namespaces.default', false)
      def opt = opt_manager.getOpt('person_complete', 'com.cabolabs.openehr_opt.namespaces.default')

      // before complete(): root PERSON has no name constraint at all
      assert opt.definition.getAttr('name') == null

      opt.complete()

      def nameAttrAfter = opt.definition.getAttr('name')
      assert nameAttrAfter != null // complete() added it, since none existed
      assert opt.definition.attributes.findAll{ it.rmAttributeName == 'name' }.size() == 1 // exactly one, not duplicated

      // The DV_TEXT alternative gets the implicit constraint resolved and
      // materialized as a real fixed-value list, from term_definitions keyed by
      // the node's own node_id (at0000 -> "person_complete") - not left open.
      def dvTextAlt = nameAttrAfter.children.find { it.rmTypeName == 'DV_TEXT' }
      assert dvTextAlt != null
      assert dvTextAlt.getAttr('value').children[0].item.list == ['person_complete']

      // DV_CODED_TEXT is untouched/open - its fixed-value check happens via the
      // separate terminology=='local' cross-check in validate(DvCodedText,...),
      // not via a nested 'value' constraint here.
      def dvCodedTextAlt = nameAttrAfter.children.find { it.rmTypeName == 'DV_CODED_TEXT' }
      assert dvCodedTextAlt != null
      assert dvCodedTextAlt.getAttr('value') == null
   }
}
