package com.cabolabs.openehr.formats

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.*
import com.cabolabs.openehr.rm_1_0_2.common.change_control.Contribution
import com.cabolabs.openehr.rm_1_0_2.common.change_control.OriginalVersion
import com.cabolabs.openehr.rm_1_0_2.common.change_control.Version
import com.cabolabs.openehr.rm_1_0_2.common.generic.*
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import com.cabolabs.openehr.rm_1_0_2.composition.EventContext
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.*
import com.cabolabs.openehr.rm_1_0_2.composition.content.navigation.Section
import com.cabolabs.openehr.rm_1_0_2.data_structures.history.History
import com.cabolabs.openehr.rm_1_0_2.data_structures.history.IntervalEvent
import com.cabolabs.openehr.rm_1_0_2.data_structures.history.PointEvent
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.*
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.Cluster
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.Element
import com.cabolabs.openehr.rm_1_0_2.data_types.basic.DvBoolean
import com.cabolabs.openehr.rm_1_0_2.data_types.basic.DvIdentifier
import com.cabolabs.openehr.rm_1_0_2.data_types.encapsulated.DvMultimedia
import com.cabolabs.openehr.rm_1_0_2.data_types.encapsulated.DvParsable
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.*
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.*
import com.cabolabs.openehr.rm_1_0_2.data_types.text.*
import com.cabolabs.openehr.rm_1_0_2.data_types.uri.*
import com.cabolabs.openehr.rm_1_0_2.ehr.Ehr
import com.cabolabs.openehr.rm_1_0_2.ehr.EhrStatus
import com.cabolabs.openehr.rm_1_0_2.support.identification.*
import com.cabolabs.openehr.dto_1_0_2.common.change_control.ContributionDto

import com.cabolabs.openehr.dto_1_0_2.ehr.EhrDto
import com.cabolabs.openehr.rm_1_0_2.common.directory.Folder

// Old Groovy
import groovy.util.XmlSlurper
import groovy.util.slurpersupport.GPathResult

import com.cabolabs.openehr.opt.instance_validation.*

// New Groovy 3.0.7+
//import groovy.xml.XmlSlurper
//import groovy.xml.slurpersupport.GPathResult

@groovy.util.logging.Log4j
class OpenEhrXmlParser {

   def schemaValidate = false
   def schemaFlavor = "rm"
   def schemaValidator
   def schemaValidationErrors

   def OpenEhrXmlParser(boolean schemaValidate = false)
   {
      this.schemaValidate = schemaValidate
   }

   def setSchemaFlavorAPI()
   {
      this.schemaFlavor = "api"
   }

   def setSchemaFlavorRM()
   {
      this.schemaFlavor = "rm"
   }

   /**
    * Returns errors from the schema validation.
    */
   def getValidationErrors()
   {
      if (!schemaValidate)
      {
         throw new Exception("schemaValidate wasn't set to true")
      }

      // TODO: need to check if the validation was run before calling this method.

      return this.schemaValidationErrors
   }

   // ========= ENTRY POINTS =========

   Ehr parseEhr(String xml)
   {
      def slurper = new XmlSlurper(false, false)
      def gpath = slurper.parseText(xml)

      if (this.schemaValidate)
      {
         // NOTE: favour should be 'rm'
         def inputStream = getClass().getResourceAsStream('/xsd/Version.xsd')
         schemaValidator = new XmlValidation(inputStream)
         if (!schemaValidator.validate(xml))
         {
            schemaValidationErrors = schemaValidator.getErrors()
            return
         }
      }

      def ehr = new Ehr()

      ehr.system_id = this.parseHIER_OBJECT_ID(gpath.system_id)

      ehr.ehr_id = this.parseHIER_OBJECT_ID(gpath.ehr_id)

      ehr.time_created = this.parseDV_DATE_TIME(gpath.time_created)

      // TODO: check it's an OBJECT_REF not a EHR_STATUS instance
      ehr.ehr_status = this.parseOBJECT_REF(gpath.ehr_status)

      return ehr
   }

   EhrDto parseEhrDto(String xml)
   {
      def slurper = new XmlSlurper(false, false)
      def gpath = slurper.parseText(xml)

      if (this.schemaValidate)
      {
         // TODO: setup validation and validate XML
         // NOTE: favour should be 'api'
      }

      def ehr = new EhrDto()

      ehr.system_id = this.parseHIER_OBJECT_ID(gpath.system_id)

      ehr.ehr_id = this.parseHIER_OBJECT_ID(gpath.ehr_id)

      ehr.time_created = this.parseDV_DATE_TIME(gpath.time_created)

      // TODO: check this is EHR_STATUS not OBJECT_REF
      ehr.ehr_status = this.parseEHR_STATUS(gpath.ehr_status)

      if (gpath.ehr_access)
      {
         ehr.ehr_access = this.parseEHR_ACCESS(gpath.ehr_access)
      }

      return ehr
   }


   // used to parse compositions and other descendant from Locatable
   Locatable parseLocatable(String xml)
   {
      def slurper = new XmlSlurper(false, false)
      def gpath = slurper.parseText(xml)

      if (this.schemaValidate)
      {
         if (gpath.archetype_details.isEmpty() || gpath.archetype_details.rm_version.isEmpty()) // rm version aware
         {
            throw new Exception("archetype_details.rm_version is required for the root of any archetypable class")
         }

         def inputStream = getClass().getResourceAsStream('/xsd/Version.xsd')
         schemaValidator = new XmlValidation(inputStream)
         if (!schemaValidator.validate(xml))
         {
            schemaValidationErrors = schemaValidator.getErrors()
            return
         }
      }

      String type = gpath.'@xsi:type'.text()

      if (!type)
      {
         throw new XmlParseException("Can't parse XML if root node doesn't have a xsi:type")
      }

      def method = 'parse'+ type
      return this."$method"(gpath)
   }


   // used to parse versions because is not Locatable
   Version parseVersion(String xml)
   {
      def slurper = new XmlSlurper(false, false)
      def gpath   = slurper.parseText(xml)

      if (this.schemaValidate)
      {
         if (!gpath.archetype_details || !gpath.archetype_details.rm_version) // rm version aware
         {
            throw new Exception("archetype_details.rm_version is required for the root of any archetypable class")
         }

         // TODO: add the xml schema validation and error reporting
      }

      String type = gpath.'@xsi:type'.text()

      if (!type)
      {
         throw new XmlParseException("Can't parse XML if root node doesn't have a xsi:type")
      }

      // TODO: support IMPORTED_VERSION
      if (!['ORIGINAL_VERSION', 'IMPORTED_VERSION'].contains(type))
      {
         throw new XmlParseException("Can't parse JSON: type ${type} should be either ORIGINAL_VERSION or IMPORTED_VERSION")
      }

      def method = 'parse'+ type
      Version out
      try
      {
         out = this."$method"(gpath)
      }
      catch (Exception e)
      {
         println e.message
         throw new XmlParseException("Can't parse XML, check ${type} is a VERSION type. If you tried to parse a LOCATABLE, use the parseLocatable method", e)
      }
      return out
   }


   // Used to parse the payload for POST /contributon of the openEHR REST API
   List<Version> parseVersionList(String xml)
   {
      /*
      <contribution>
        <versions xsi:type="ORIGINAL_VERSION">
        ...
        </versions>
        <versions xsi:type="ORIGINAL_VERSION">
        ...
        </versions>
        ...
      </contribution>
      */

      def slurper   = new XmlSlurper(false, false)
      def gpath     = slurper.parseText(xml)
      String type, method

      List versions = []

      gpath.versions.each { version_gpath ->

         type = version_gpath.'@xsi:type'.text()

         if (!type)
         {
            throw new XmlParseException("Can't parse XML if node doesn't have a xsi:type")
         }

         if (!['ORIGINAL_VERSION', 'IMPORTED_VERSION'].contains(type))
         {
            throw new XmlParseException("Can't parse JSON: type ${type} should be either ORIGINAL_VERSION or IMPORTED_VERSION")
         }

         method = 'parse'+ type
         versions << this."$method"(version_gpath)
      }

      return versions
   }

   ContributionDto parseContributionDto(String xml)
   {
      def slurper = new XmlSlurper(false, false)
      def gpath   = slurper.parseText(xml)

      if (this.schemaValidate)
      {
         // TODO:

         // TODO: can't get the rm_version from a contribution

         // NOTE: since this is DTO, the schema flavor will always be api
         //this.schemaValidator = new JsonInstanceValidation('api', '1.0.2') // FIXME: hardcoded rm_version, should be a parameter
      }

      String type, method
      Version version

      def contribution = new ContributionDto(versions: [])

      // in the DTO the uid is optional
      if (!gpath.uid.isEmpty())
      {
         contribution.uid = this.parseHIER_OBJECT_ID(gpath.uid)
      }

      gpath.versions.each { version_gpath ->

         type = version_gpath.'@xsi:type'.text() // ORIGINAL_VERSION, IMPORTED_VERSION

         if (!type)
         {
            throw new XmlParseException("Can't parse XML if node doesn't have a xsi:type")
         }

         method = 'parse'+ type
         contribution.versions << this."$method"(version_gpath)
      }

      contribution.audit = this.parseAUDIT_DETAILS(gpath.audit)

      return contribution
   }

   Contribution parseContribution(String xml)
   {
      def slurper = new XmlSlurper(false, false)
      def gpath   = slurper.parseText(xml)
      String type, method

      // TODO: schema validation?

      def contribution = new Contribution(versions: new HashSet())

      // note for the RM the uid is mandatory, in the DTO the uid is optional
      contribution.uid = this.parseHIER_OBJECT_ID(gpath.uid)

      gpath.versions.each { version_gpath ->

         contribution.versions << this.parseOBJECT_REF(version_gpath) // contribiution has objectrefs
      }

      contribution.audit = this.parseAUDIT_DETAILS(gpath.audit)

      return contribution
   }

   // Use to parse Contribution, which has ObjectRefs to Versions, from a list of Versions
   private ObjectRef parseVersionToObjectRef(Version version)
   {
      ObjectRef o   = new ObjectRef()

      o.namespace   = 'ehr'
      o.type        = 'VERSION'
      o.id          = version.uid // OBJECT_VERSION_ID

      return o
   }


   // ========= FIll METHODS =========

   private void fillLOCATABLE(Locatable l, GPathResult xml, Pathable parent)
   {
      // name can be text or coded
      String type = xml.name.'@xsi:type'.text()
      if (!type) type = 'DV_TEXT'
      String method = 'parse'+ type

      l.name = this."$method"(xml.name)

      l.archetype_node_id = xml.@archetype_node_id.text() // can be null

      //println "archetype node id: "+ xml.@archetype_node_id.text()

      if (!xml.uid.isEmpty())
      {
         type = xml.uid.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlParseException("@xsi:type required for LOCATABLE.uid")
         }
         method = 'parse'+ type
         l.uid = this."$method"(xml.uid)
      }

      if (!xml.archetype_details.isEmpty())
         l.archetype_details = this.parseARCHETYPED(xml.archetype_details)

      //this.fillPATHABLE(l, parent)
   }

   private void fillENTRY(Entry e, GPathResult xml, Pathable parent)
   {
      String type, method

      this.fillLOCATABLE(e, xml, parent)

      e.encoding = this.parseCODE_PHRASE(xml.encoding)
      e.language = this.parseCODE_PHRASE(xml.language)


      type = xml.subject.'@xsi:type'.text()
      if (!type)
      {
         throw new XmlParseException("@xsi:type required for ENTRY.subject")
      }
      method = 'parse'+ type
      e.subject = this."$method"(xml.subject)


      if (!xml.provider.isEmpty())
      {
         type = xml.provider.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlParseException("@xsi:type required for ENTRY.provider")
         }
         method = 'parse'+ type
         e.provider = this."$method"(xml.provider)
      }


      if (!xml.other_participations.isEmpty())
      {
         def participation
         xml.other_participations.each { _participation ->

            participation = this.parsePARTICIPATION(_participation)
            e.other_participations.add(participation)
         }
      }


      if (!xml.workflow_id.isEmpty())
      {
         e.workflow_id = this.parseOBJECT_REF(xml.workflow_id)
      }
   }

   private void fillCARE_ENTRY(CareEntry c, GPathResult xml, Pathable parent)
   {
      if (!xml.protocol.isEmpty())
      {
         String type = xml.protocol.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlParseException("@xsi:type required for CARE_ENTRY.protocol")
         }
         String method = 'parse'+ type
         //String path_node_id = (xml.protocol.@archetype_node_id.text().startsWith('at') ? xml.protocol.@archetype_node_id.text() : 'archetype_id='+ xml.protocol.@archetype_node_id.text())
         c.protocol = this."$method"(xml.protocol, parent)
      }

      if (!xml.guideline_id.isEmpty())
      {
         c.guideline_id = this.parseOBJECT_REF(xml.guideline_id)
      }

      this.fillENTRY(c, xml, parent)
   }

   /* TODO:

   - fillPARTY
   - fillACTOR
   - fillPartyDto
   - fillActorDto

   */


   // ========= PARSE METHODS =========

   // TODO: parse demographics


    // NOTE: parseEHR_STATUS is a top level class so it doesn't have a Pathable parent and the path is /
   private EhrStatus parseEHR_STATUS(GPathResult xml)
   {
      def status = new EhrStatus()

      this.fillLOCATABLE(status, xml, null)

      // subject is mandatory, is the ref inside that might be optional
      //if (map.subject)
      //{
         status.subject = this.parsePARTY_SELF(xml.subject)
      //}

      // TODO: test these two are parsed correctly
      status.is_modifiable = xml.is_modifiable.text().toBoolean()
      status.is_queryable = xml.is_queryable.text().toBoolean()

      if (!xml.other_details.isEmpty())
      {
         String method = 'parse'+ xml.other_details.'@xsi:type'.text()
         status.other_details = this."$method"(xml.other_details, status)
      }

      return status
   }


   private Composition parseCOMPOSITION(GPathResult xml)
   {
      Composition compo = new Composition()

      this.fillLOCATABLE(compo, xml, null)

      compo.language  = this.parseCODE_PHRASE(xml.language)
      compo.territory = this.parseCODE_PHRASE(xml.territory)
      compo.category  = this.parseDV_CODED_TEXT(xml.category)

      String type, method

      type = xml.composer.'@xsi:type'.text() // party proxy or descendants
      if (!type)
      {
         throw new XmlParseException("@xsi:type required for COMPOSITION.composer")
      }
      method = 'parse'+ type
      compo.composer = this."$method"(xml.composer)

      // NOTE: these paths are not using the node_id, are just attribute paths
      compo.context = parseEVENT_CONTEXT(xml.context, compo)

      def content = []

      xml.content.eachWithIndex { content_item, i ->
         type = content_item.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlParseException("@xsi:type required for COMPOSITION.content[$i]")
         }
         method = 'parse'+ type
         if (!compo.content) compo.content = []
         compo.content.add(
            this."$method"(content_item, compo)
         )
      }

      return compo
   }

   // This will parse the top level directory that doesn't have a LOCATABLE parent
   private Folder parseFOLDER(GPathResult xml)
   {
      parseFolderInternal(xml, null)
   }

   // Reuses code for both parseFOLDER methods
   private Folder parseFolderInternal(GPathResult xml, Folder parent)
   {
      def folder = new Folder()

      this.fillLOCATABLE(folder, xml, parent)

      if (!xml.items.isEmpty())
      {
         folder.items = []
         xml.items.each { item ->
            folder.items << this.parseOBJECT_REF(item)
         }
      }

      if (!xml.folders.isEmpty())
      {
         folder.folders = []
         xml.folders.each { subfolder ->

            folder.folders << this.parseFolderInternal(subfolder, folder)
         }
      }

      return folder
   }


   private OriginalVersion parseORIGINAL_VERSION(GPathResult xml)
   {
      OriginalVersion ov = new OriginalVersion()

      ov.uid = this.parseOBJECT_VERSION_ID(xml.uid)

      if (!xml.signature.isEmpty())
      {
         ov.signature = xml.signature
      }

      if (!xml.preceding_version_uid.isEmpty())
      {
         ov.preceding_version_uid = this.parseOBJECT_VERSION_ID(xml.preceding_version_uid)
      }

      // TODO: other_input_version_uids

      ov.lifecycle_state = this.parseDV_CODED_TEXT(xml.lifecycle_state)

      ov.contribution = this.parseOBJECT_REF(xml.contribution)

      // TODO: AuditDetails could be subclass ATTESTATION
      ov.commit_audit = this.parseAUDIT_DETAILS(xml.commit_audit)

      if (!xml.attestations.isEmpty())
      {
         xml.attestations.each { attestation ->

            ov.attestations.add(this.parseATTESTATION(attestation))
         }
      }

      if (!xml.data.isEmpty())
      {
         def type = xml.data.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlParseException("@xsi:type required for ORIGINAL_VERSION.data")
         }
         def method = 'parse'+ type
         ov.data = this."$method"(xml.data)
      }

      return ov
   }

   private AuditDetails parseAUDIT_DETAILS(GPathResult xml)
   {
      AuditDetails ad = new AuditDetails()

      ad.system_id      = xml.system_id
      ad.time_committed = this.parseDV_DATE_TIME(xml.time_committed)
      ad.change_type    = this.parseDV_CODED_TEXT(xml.change_type)

      if (!xml.description.isEmpty())
      {
         ad.description = this.parseDV_TEXT(xml.description)
      }

      def type = xml.committer.'@xsi:type'.text()
      if (!type)
      {
         throw new XmlParseException("@xsi:type required for AUDIT_DETAILS.committer")
      }
      def method = 'parse'+ type
      ad.committer = this."$method"(xml.committer)

      return ad
   }

   private Attestation parseATTESTATION(GPathResult xml)
   {
      Attestation at = new Attestation()

      // AuditDetails fields
      at.system_id      = xml.system_id
      at.time_committed = this.parseDV_DATE_TIME(xml.time_committed)
      at.change_type    = this.parseDV_CODED_TEXT(xml.change_type)

      if (!xml.description.isEmpty())
      {
         at.description = this.parseDV_TEXT(xml.description)
      }

      def type = xml.committer.'@xsi:type'.text()
      if (!type)
      {
         throw new XmlParseException("@xsi:type required for ATTESTATION.committer")
      }
      def method = 'parse'+ type
      at.committer = this."$method"(xml.committer)

      // Attestation fields
      if (!xml.attested_view.isEmpty())
      {
         at.attested_view = this.parseDV_MULTIMEDIA(xml.attested_view)
      }

      if (!xml.proof.isEmpty())
      {
         at.proof = xml.proof
      }

      // TODO: xml.items

      type = xml.reason.'@xsi:type'.text() // text or coded
      if (!type)
      {
         throw new XmlParseException("@xsi:type required for ATTESTATION.reason")
      }
      method = 'parse'+ type
      at.reason = this."$method"(xml.reason)

      // TODO: test if this is parsed as a boolean or as a string
      at.is_pending = xml.is_pending.toBoolean()

      return at
   }

   private ReferenceRange parseREFERENCE_RANGE(GPathResult xml)
   {
      ReferenceRange rr = new ReferenceRange()

      rr.meaning = this.parseDV_TEXT(xml.meaning)

      rr.range = this.parseDV_INTERVAL(xml.range)

      return rr
   }

   private void fillDV_ORDERED(DvOrdered d, GPathResult xml)
   {
      if (!xml.normal_status.isEmpty())
      {
         d.normal_status = this.parseCODE_PHRASE(xml.normal_status)
      }

      if (!xml.normal_range.isEmpty())
      {
         d.normal_range = this.parseDV_INTERVAL(xml.normal_range)
      }

      if (!xml.other_reference_ranges.isEmpty())
      {
         def ref_range
         xml.other_reference_ranges.each { _reference_range ->

            ref_range = this.parseREFERENCE_RANGE(_reference_range)
            d.other_reference_ranges.add(ref_range)
         }
      }
   }

   private void fillDV_QUANTIFIED(DvQuantified d, GPathResult xml)
   {
      this.fillDV_ORDERED(d, xml)

      if (!xml.magnitude_status.isEmpty())
      {
         d.magnitude_status = xml.magnitude_status
      }
   }

   private void fillDV_AMOUNT(DvAmount d, GPathResult xml)
   {
      this.fillDV_ORDERED(d, xml)

      if (!xml.accuracy.isEmpty())
      {
         d.accuracy = xml.accuracy
      }

      if (!xml.accuracy_is_percent.isEmpty())
      {
         d.accuracy_is_percent = xml.accuracy_is_percent
      }
   }

   private ArchetypeId parseARCHETYPE_ID(GPathResult xml)
   {
      new ArchetypeId(value: xml.value)
   }

   private TemplateId parseTEMPLATE_ID(GPathResult xml)
   {
      new TemplateId(value: xml.value)
   }

   private Archetyped parseARCHETYPED(GPathResult xml)
   {
      Archetyped a = new Archetyped()
      a.archetype_id = this.parseARCHETYPE_ID(xml.archetype_id)
      a.template_id = this.parseTEMPLATE_ID(xml.template_id)
      a.rm_version = xml.rm_version
      return a
   }

   private PartySelf parsePARTY_SELF(GPathResult xml)
   {
      PartySelf p = new PartySelf()

      if (!xml.external_ref.isEmpty())
      {
         p.external_ref = this.parsePARTY_REF(xml.external_ref)
      }

      return p
   }

   private PartyIdentified parsePARTY_IDENTIFIED(GPathResult xml)
   {
      PartyIdentified p = new PartyIdentified()

      if (!xml.name.isEmpty())
      {
         p.name = xml.name
      }

      xml.identifiers.each { identifier ->

         p.identifiers.add(this.parseDV_IDENTIFIER(identifier))
      }

      if (!xml.external_ref.isEmpty())
      {
         p.external_ref = this.parsePARTY_REF(xml.external_ref)
      }

      return p
   }

   private PartyRelated parsePARTY_RELATED(GPathResult xml)
   {
      PartyRelated p = new PartyRelated()

      if (!xml.name.isEmpty())
      {
         p.name = xml.name
      }

      xml.identifiers.each { identifier ->

         p.identifiers.add(this.parseDV_IDENTIFIER(identifier))
      }

      if (!xml.external_ref.isEmpty())
      {
         p.external_ref = this.parsePARTY_REF(xml.external_ref)
      }

      if (!xml.relationship.isEmpty())
      {
         p.relationship = this.parseDV_CODED_TEXT(xml.relationship)
      }

      return p
   }

   private ObjectRef parseOBJECT_REF(GPathResult xml)
   {
      ObjectRef o = new ObjectRef()

      o.namespace = xml.namespace
      o.type = xml.type

      String type = xml.id.'@xsi:type'.text()
      if (!type)
      {
         throw new XmlParseException("@xsi:type required for OBJECT_REF.id")
      }
      String method = 'parse'+ type
      o.id = this."$method"(xml.id)

      return o
   }

   private PartyRef parsePARTY_REF(GPathResult xml)
   {
      PartyRef p = new PartyRef()

      p.namespace = xml.namespace
      p.type = xml.type

      String type = xml.id.'@xsi:type'.text()
      if (!type)
      {
         throw new XmlParseException("@xsi:type required for PARTY_REF.id")
      }
      String method = 'parse'+ type
      p.id = this."$method"(xml.id)

      return p
   }

   private LocatableRef parseLOCATABLE_REF(GPathResult xml)
   {
      LocatableRef o = new LocatableRef()

      o.namespace = xml.namespace
      o.type = xml.type

      if (!xml.path.isEmpty())
         o.path = xml.path

      String type = xml.id.'@xsi:type'.text()
      if (!type)
      {
         throw new XmlParseException("@xsi:type required for LOCATABLE_REF.id")
      }
      String method = 'parse'+ type
      o.id = this."$method"(xml.id)

      return o
   }


   private DvIdentifier parseDV_IDENTIFIER(GPathResult xml)
   {
      DvIdentifier i = new DvIdentifier()

      i.issuer = xml.issuer
      i.assigner = xml.assigner
      i.id = xml.id
      i.type = xml.type

      return i
   }


   private EventContext parseEVENT_CONTEXT(GPathResult xml, Pathable parent)
   {
      EventContext e = new EventContext()

      //this.fillPATHABLE(e, parent)

      e.start_time = this.parseDV_DATE_TIME(xml.start_time)

      if (e.end_time)
         e.end_time = this.parseDV_DATE_TIME(xml.end_time)

      e.location = xml.location

      e.setting = this.parseDV_CODED_TEXT(xml.setting)

      if (!xml.other_context.isEmpty())
      {
         String type, method
         type = xml.other_context.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlParseException("@xsi:type required for EVENT_CONTEXT.other_context")
         }
         method = 'parse'+ type
         e.other_context = this."$method"(xml.other_context, e)
      }

      // TODO: health_care_facility

      xml.participations.each { participation ->
         e.participations.add(this.parsePARTICIPATION(participation))
      }

      return e
   }

   private Participation parsePARTICIPATION(GPathResult xml)
   {
      Participation p = new Participation()

      p.function = this.parseDV_TEXT(xml.function)

      if (!xml.time.isEmpty())
      {
         p.time = this.parseDV_INTERVAL(xml.time)
      }

      p.mode = this.parseDV_CODED_TEXT(xml.mode)

      String type = xml.performer.'@xsi:type'.text()
      if (!type)
      {
         throw new XmlParseException("@xsi:type required for PARTICIPATION.performer")
      }
      String method = 'parse'+ type
      p.performer = this."$method"(xml.performer)

      return p
   }

   private Section parseSECTION(GPathResult xml, Pathable parent)
   {
      Section section = new Section()

      this.fillLOCATABLE(section, xml, parent)

      String type, method

      xml.items.eachWithIndex { content_item, i ->
         type = content_item.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlParseException("@xsi:type required for SECTION.items[$i]")
         }
         method = 'parse'+ type
         if (!section.items) section.items = []
         section.items.add(
            this."$method"(content_item, section)
         )
      }

      return section
   }

   private AdminEntry parseADMIN_ENTRY(GPathResult xml, Pathable parent)
   {
      AdminEntry a = new AdminEntry()

      this.fillENTRY(a, xml, parent)

      String type = xml.data.'@xsi:type'.text()
      if (!type)
      {
         throw new XmlParseException("@xsi:type required for ACMIN_ENTRY.data")
      }
      String method = 'parse'+ type
      a.data = this."$method"(xml.data, a)

      return a
   }

   private Observation parseOBSERVATION(GPathResult xml, Pathable parent)
   {
      Observation o = new Observation()

      this.fillCARE_ENTRY(o, xml, parent)

      if (!xml.data.isEmpty())
      {
         o.data = this.parseHISTORY(xml.data, o)
      }

      if (!xml.state.isEmpty())
      {
         o.state = this.parseHISTORY(xml.state, o)
      }

      return o
   }

   private History parseHISTORY(GPathResult xml, Pathable parent)
   {
      History h = new History()

      this.fillLOCATABLE(h, xml, parent)

      h.origin = this.parseDV_DATE_TIME(xml.origin)

      if (!xml.period.isEmpty())
      {
         h.period = this.parseDV_DURATION(xml.period)
      }

      if (!xml.duration.isEmpty())
      {
         h.duration = this.parseDV_DURATION(xml.duration)
      }

      String type, method
      xml.events.eachWithIndex { event, i ->
         type = event.'@xsi:type'.text()
         method = 'parse'+ type
         if (!h.events) h.events = []
         h.events.add(
            this."$method"(event, h)
         )
      }

      return h
   }

   private PointEvent parsePOINT_EVENT(GPathResult xml, Pathable parent)
   {
      PointEvent e = new PointEvent()

      this.fillLOCATABLE(e, xml, parent)

      e.time = this.parseDV_DATE_TIME(xml.time)

      String type, method

      if (!xml.data.isEmpty())
      {
         type = xml.data.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlParseException("@xsi:type required for POINT_EVENT.data")
         }
         method = 'parse'+ type
         e.data = this."$method"(xml.data, e)
      }

      if (!xml.state.isEmpty())
      {
         type = xml.state.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlParseException("@xsi:type required for POINT_EVENT.state")
         }
         method = 'parse'+ type
         e.state = this."$method"(xml.state, e)
      }

      return e
   }

   private IntervalEvent parseINTERVAL_EVENT(GPathResult xml, Pathable parent)
   {
      IntervalEvent e = new IntervalEvent()

      this.fillLOCATABLE(e, xml, parent)

      e.time = this.parseDV_DATE_TIME(xml.time)

      String type, method

      if (!xml.data.isEmpty())
      {
         type = xml.data.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlParseException("@xsi:type required for  INTERVAL_EVENT.data")
         }
         method = 'parse'+ type
         e.data = this."$method"(xml.data, e)
      }

      if (!xml.state.isEmpty())
      {
         type = xml.state.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlParseException("@xsi:type required for INTERVAL_EVENT.state")
         }
         method = 'parse'+ type
         e.state = this."$method"(xml.state, e)
      }

      e.width = this.parseDV_DURATION(xml.width)

      e.math_function = this.parseDV_CODED_TEXT(xml.math_function)

      if (!xml.sample_count.isEmpty())
      {
         e.sample_count = xml.sample_count
      }

      return e
   }

   private Evaluation parseEVALUATION(GPathResult xml, Pathable parent)
   {
      Evaluation e = new Evaluation()

      this.fillCARE_ENTRY(e, xml, parent)

      String type = xml.data.'@xsi:type'.text()
      if (!type)
      {
         throw new XmlParseException("@xsi:type required for EVALUATION.data")
      }
      String method = 'parse'+ type
      e.data = this."$method"(xml.data, e)

      return e
   }

   private Instruction parseINSTRUCTION(GPathResult xml, Pathable parent)
   {
      Instruction ins = new Instruction()

      this.fillCARE_ENTRY(ins, xml, parent)

      String type, method

      type = xml.narrative.'@xsi:type'.text()
      if (!type) type = 'DV_TEXT'
      method = 'parse'+ type
      ins.narrative = this."$method"(xml.narrative)


      if (!xml.expiry_time.isEmpty())
         ins.expiry_time = this.parseDV_DATE_TIME(xml.expiry_time)


      if (!xml.wf_definition.isEmpty())
         ins.wf_definition = this.parseDV_PARSABLE(xml.wf_definition)


      xml.activities.eachWithIndex { js_activity, i ->

         ins.activities.add(
            this.parseACTIVITY(js_activity, ins)
         )
      }

      return ins
   }

   private Action parseACTION(GPathResult xml, Pathable parent)
   {
      Action a = new Action()

      this.fillCARE_ENTRY(a, xml, parent)

      String type = xml.description.'@xsi:type'.text()
      if (!type)
      {
         throw new XmlParseException("@xsi:type required for ACTION.description")
      }
      String method = 'parse'+ type

      a.description = this."$method"(xml.description, a)

      a.time = this.parseDV_DATE_TIME(xml.time)

      a.ism_transition = this.parseISM_TRANSITION(xml.ism_transition, a)

      if (!xml.instruction_details.isEmpty())
         a.instruction_details = this.parseINSTRUCTION_DETAILS(xml.instruction_details, a)

      return a
   }

   private IsmTransition parseISM_TRANSITION(GPathResult xml, Pathable parent)
   {
      IsmTransition i = new IsmTransition()

      //this.fillPATHABLE(i, parent)

      i.current_state = this.parseDV_CODED_TEXT(xml.current_state)

      if (!xml.transition.isEmpty())
      {
         i.transition = this.parseDV_CODED_TEXT(xml.transition)
      }

      if (!xml.careflow_step.isEmpty())
      {
         i.careflow_step = this.parseDV_CODED_TEXT(xml.careflow_step)
      }

      return i
   }

   private InstructionDetails parseINSTRUCTION_DETAILS(GPathResult xml, Pathable parent)
   {
      InstructionDetails i = new InstructionDetails()

      //this.fillPATHABLE(i, parent)

      i.instruction_id = this.parseLOCATABLE_REF(xml.instruction_id)

      i.activity_id = xml.activity_id

      if (!xml.wf_details.isEmpty())
      {
         String type = xml.wf_details.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlParseException("@xsi:type required for INSTRUCTION_DETAILS.wf_details")
         }
         String method = 'parse'+ type
         i.wf_details = this."$method"(xml.wf_details)
      }

      return i
   }

   private Activity parseACTIVITY(GPathResult xml, Pathable parent)
   {
      String type = xml.description.'@xsi:type'.text()
      if (!type)
      {
         throw new XmlParseException("@xsi:type required for ACTIVITY.description")
      }
      String method = 'parse'+ type

      Activity a = new Activity(
         action_archetype_id: xml.action_archetype_id
      )

      a.description = this."$method"(xml.description, a)

      if (!xml.timing.isEmpty())
      {
         a.timing = this.parseDV_PARSABLE(xml.timing)
      }

      this.fillLOCATABLE(a, xml, parent)

      return a
   }



   private ItemTree parseITEM_TREE(GPathResult xml, Pathable parent)
   {
      ItemTree t = new ItemTree()

      this.fillLOCATABLE(t, xml, parent)

      String type, method

      xml.items.eachWithIndex { item, i ->
         type = item.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlParseException("@xsi:type required for ITEM_TREE.items[$i]")
         }
         method = 'parse'+ type
         if (!t.items) t.items = []
         t.items.add(
            this."$method"(item, t)
         )
      }

      return t
   }

   private ItemList parseITEM_LIST(GPathResult xml, Pathable parent)
   {
      ItemList l = new ItemList()

      this.fillLOCATABLE(l, xml, parent)

      xml.items.eachWithIndex { element, i ->
      if (!l.items) l.items = []
         l.items.add(
            this.parseELEMENT(element, l)
         )
      }

      return l
   }

   private ItemTable parseITEM_TABLE(GPathResult xml, Pathable parent)
   {
      ItemTable t = new ItemTable()

      this.fillLOCATABLE(t, xml, parent)

      String type, method

      // FIXME: rows are CLUSTERS, we don't need to get the dynamic method
      xml.rows.eachWithIndex { item, i ->
         type = item.'@xsi:type'.text()
         method = 'parse'+ type
         if (!t.rows) t.rows = []
         t.rows.add(
            this."$method"(item, t)
         )
      }

      return t
   }

   private ItemSingle parseITEM_SINGLE(GPathResult xml, Pathable parent)
   {
      ItemSingle s = new ItemSingle()

      this.fillLOCATABLE(s, xml, parent)

      s.item = this.parseELEMENT(xml.item, s)

      return s
   }

   private Cluster parseCLUSTER(GPathResult xml, Pathable parent)
   {
      Cluster c = new Cluster()

      this.fillLOCATABLE(c, xml, parent)

      String type, method

      xml.items.eachWithIndex { item, i ->
         type = item.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlParseException("@xsi:type required for CLUSTER.items[$i]")
         }
         method = 'parse'+ type
         if (!c.items) c.items = []
         c.items.add(
            this."$method"(item, c)
         )
      }

      return c
   }

   private Element parseELEMENT(GPathResult xml, Pathable parent)
   {
      Element e = new Element()

      this.fillLOCATABLE(e, xml, parent)

      if (!xml.value.isEmpty())
      {
         String type = xml.value.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlParseException("@xsi:type required for ELEMENT.value")
         }
         String method = 'parse'+ type
         e.value = this."$method"(xml.value)
      }

      if (!xml.null_flavour.isEmpty())
      {
         e.null_flavour = this.parseDV_CODED_TEXT(xml.null_flavour)
      }

      return e
   }



   private TerminologyId parseTERMINOLOGY_ID(GPathResult xml)
   {
      new TerminologyId(
         value: xml.value
      )
   }

   private GenericId parseGENERIC_ID(GPathResult xml)
   {
      new GenericId(
         scheme: xml.scheme,
         value: xml.value
      )
   }

   private CodePhrase parseCODE_PHRASE(GPathResult xml)
   {
      new CodePhrase(
         code_string: xml.code_string,
         terminology_id: this.parseTERMINOLOGY_ID(xml.terminology_id)
      )
   }

   private HierObjectId parseHIER_OBJECT_ID(GPathResult xml)
   {
      new HierObjectId(
         value: xml.value
      )
   }

   private ObjectVersionId parseOBJECT_VERSION_ID(GPathResult xml)
   {
      new ObjectVersionId(
         value: xml.value
      )
   }

   private VersionTreeId parseVERSION_TREE_ID(GPathResult xml)
   {
      new VersionTreeId(
         value: xml.value
      )
   }


   private DvText parseDV_TEXT(GPathResult xml)
   {
      new DvText(value: xml.value)
   }

   private DvCodedText parseDV_CODED_TEXT(GPathResult xml)
   {
      new DvCodedText(
         value: xml.value,
         defining_code: this.parseCODE_PHRASE(xml.defining_code)
      )
   }

   private TermMapping parseTERM_MAPPING(GPathResult xml)
   {
      new TermMapping(
         match: xml.match,
         purpose: this.parseDV_CODED_TEXT(xml.purpose),
         target: this.parseCODE_PHRASE(xml.target)
      )
   }

   private DvDateTime parseDV_DATE_TIME(GPathResult xml)
   {
      // TODO: DvAbsoluteQuantity
      new DvDateTime(value: xml.value)
   }

   private DvDate parseDV_DATE(GPathResult xml)
   {
      // TODO: DvAbsoluteQuantity
      new DvDate(value: xml.value)
   }

   private DvTime parseDV_TIME(GPathResult xml)
   {
      // TODO: DvAbsoluteQuantity
      new DvTime(value: xml.value)
   }

   private DvDuration parseDV_DURATION(GPathResult xml)
   {
      DvDuration d = new DvDuration()

      d.value = xml.value.text()

      this.fillDV_AMOUNT(d, xml)

      return d
   }

   private DvQuantity parseDV_QUANTITY(GPathResult xml)
   {
      DvQuantity q = new DvQuantity()

      q.magnitude = xml.magnitude.toDouble() // Double.valueOf(xml.magnitude.text())

      q.units = xml.units.text()

      if (!xml.precision.isEmpty())
      {
         q.precision = xml.precision.text().toInteger()
      }

      this.fillDV_AMOUNT(q, xml)

      return q
   }

   private DvCount parseDV_COUNT(GPathResult xml)
   {
      DvCount c = new DvCount()

      c.magnitude = xml.magnitude.text().toInteger()

      this.fillDV_AMOUNT(c, xml)

      return c
   }

   private DvProportion parseDV_PROPORTION(GPathResult xml)
   {
      DvProportion d = new DvProportion(
         numerator: xml.numerator.toFloat(),
         denominator: xml.denominator.toFloat(),
         type: xml.type.toInteger()
      )

      if (!xml.precision.isEmpty())
      {
         d.precision = xml.precision.toInteger()
      }

      this.fillDV_AMOUNT(d, xml)

      return d
   }

   private DvOrdinal parseDV_ORDINAL(GPathResult xml)
   {
      DvOrdinal d = new DvOrdinal(
         value: xml.value.toInteger(),
         symbol: this.parseDV_CODED_TEXT(xml.symbol)
      )

      this.fillDV_ORDERED(d, xml)

      return d
   }

   private DvParsable parseDV_PARSABLE(GPathResult xml)
   {
      DvParsable p = new DvParsable(
         value: xml.value,
         formalism: xml.formalism,
         size: xml.value.size()
      )

      if (!xml.charset.isEmpty())
      {
         p.charset = this.parseCODE_PHRASE(xml.charset)
      }

      if (!xml.language.isEmpty())
      {
         p.language = this.parseCODE_PHRASE(xml.language)
      }

      return p
   }

   private DvMultimedia parseDV_MULTIMEDIA(GPathResult xml)
   {
      DvMultimedia d = new DvMultimedia()

      if (!xml.charset.isEmpty())
      {
         p.charset = this.parseCODE_PHRASE(xml.charset)
      }

      if (!xml.language.isEmpty())
      {
         p.language = this.parseCODE_PHRASE(xml.language)
      }

      d.alternate_text = xml.alternate_text

      if (!xml.uri.isEmpty())
      {
         d.uri = this.parseDV_URI(xml.uri)
      }

      d.data = xml.data.text().getBytes() // stores the encoded value!

      d.media_type = this.parseCODE_PHRASE(xml.media_type)

      if (!xml.compression_algorithm.isEmpty())
      {
         d.compression_algorithm = this.parseCODE_PHRASE(xml.compression_algorithm)
      }

      d.size = xml.size.toInteger()

      // TODO: integrity_check, integrity_check_algorithm, thumbnail

      return d
   }

   private DvUri parseDV_URI(GPathResult xml)
   {
      new DvUri(
         value: xml.value
      )
   }

   private DvEhrUri parseDV_EHR_URI(GPathResult xml)
   {
      new DvEhrUri(
         value: xml.value
      )
   }

   private DvBoolean parseDV_BOOLEAN(GPathResult xml)
   {
      new DvBoolean(
         value: xml.value.toBoolean() // TODO: test this is parsed correctly or needs .text().toBoolean()
      )
   }


   private DvInterval parseDV_INTERVAL(GPathResult xml)
   {
      DvInterval i = new DvInterval()

      String type, method

      // if there is no type, there is no lower or upper
      type = !xml.lower.isEmpty() ? xml.lower.'@xsi:type'.text() : xml.upper.'@xsi:type'.text()
      method = 'parse'+ type

      if (!xml.lower.isEmpty())
      {
         i.lower = this."$method"(xml.lower)
      }

      if (!xml.upper.isEmpty())
      {
         i.upper = this."$method"(xml.upper)
      }

      if (!xml.lower_included.isEmpty())
      {
         i.lower_included = xml.lower_included.toBoolean()
      }

      if (!xml.upper_included.isEmpty())
      {
         i.upper_included = xml.upper_included.toBoolean()
      }

      i.lower_unbounded = xml.lower_unbounded.toBoolean() // toBoolean is important here to get the right value
      i.upper_unbounded = xml.upper_unbounded.toBoolean()

      return i
   }

}