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

// Old Groovy
import groovy.util.XmlSlurper
import groovy.util.slurpersupport.GPathResult

// New Groovy 3.0.7+
//import groovy.xml.XmlSlurper
//import groovy.xml.slurpersupport.GPathResult

class OpenEhrXmlParser {
   
   // ========= ENTRY POINTS =========

   // TODO: parse folder

   // TODO:
   Ehr parseEhr(String xml)
   {

   }

   EhrStatus parseEhrStatus(String xml)
   {

   }

   EhrStatus parseEhrStatus(GPathResult xml)
   {

   }

   // used to parse compositions and other descendant from Locatable
   Locatable parseXml(String xml)
   {
      def slurper = new XmlSlurper(false, false)
      def gpath = slurper.parseText(xml)
      String type = gpath.'@xsi:type'.text()
      
      if (!type)
      {
         throw new XmlCompositionParseException("Can't parse XML if root node doesn't have a xsi:type")
      }
      
      def method = 'parse'+ type
      return this."$method"(gpath, null, '/', '/')
   }

   // used to parse versions because is not Locatable
   Version parseVersionXml(String xml)
   {
      def slurper = new XmlSlurper(false, false)
      def gpath   = slurper.parseText(xml)
      String type = gpath.'@xsi:type'.text()
      
      if (!type)
      {
         throw new XmlCompositionParseException("Can't parse XML if root node doesn't have a xsi:type")
      }
      
      def method = 'parse'+ type
      Version out
      try
      {
         out = this."$method"(gpath)
      }
      catch (Exception e)
      {
         throw new XmlCompositionParseException("Can't parse XML, check ${type} is a VERSION type. If you tried to parse a LOCATABLE, use the parseXml method", e)
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
            throw new XmlCompositionParseException("Can't parse XML if node doesn't have a xsi:type")
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
            throw new XmlCompositionParseException("Can't parse XML if node doesn't have a xsi:type")
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

   private void fillPATHABLE(Pathable p, Pathable parent, String path, String dataPath)
   {
      p.parent   = parent
      p.path     = path
      p.dataPath = dataPath
   }

   private void fillLOCATABLE(Locatable l, GPathResult xml, Pathable parent, String path, String dataPath)
   {
      // name can be text or coded
      String type = xml.name.'@xsi:type'.text()
      if (!type) type = 'DV_TEXT'
      String method = 'parse'+ type
      
      l.name = this."$method"(xml.name)
      
      l.archetype_node_id = xml.@archetype_node_id.text()

      //println "archetype node id: "+ xml.@archetype_node_id.text()
      
      if (!xml.uid.isEmpty())
      {
         type = xml.uid.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlCompositionParseException("@xsi:type required for "+ dataPath +".uid")
         }
         method = 'parse'+ type
         l.uid = this."$method"(xml.uid)
      }
      
      if (!xml.archetype_details.isEmpty())
         l.archetype_details = this.parseARCHETYPED(xml.archetype_details)

      this.fillPATHABLE(l, parent, path, dataPath)
   }
   
   private void fillENTRY(Entry e, GPathResult xml, Pathable parent, String path, String dataPath)
   {
      String type, method

      this.fillLOCATABLE(e, xml, parent, path, dataPath)
      
      e.encoding = this.parseCODE_PHRASE(xml.encoding)
      e.language = this.parseCODE_PHRASE(xml.language)
      
      
      type = xml.subject.'@xsi:type'.text()
      if (!type)
      {
         throw new XmlCompositionParseException("@xsi:type required for "+ dataPath +".subject")
      }
      method = 'parse'+ type
      e.subject = this."$method"(xml.subject)
      
      
      if (!xml.provider.isEmpty())
      {
         type = xml.provider.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlCompositionParseException("@xsi:type required for "+ dataPath +".provider")
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
   
   private void fillCARE_ENTRY(CareEntry c, GPathResult xml, Pathable parent, String path, String dataPath)
   {
      if (!xml.protocol.isEmpty())
      {
         String type = xml.protocol.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlCompositionParseException("@xsi:type required for "+ dataPath +".protocol")
         }
         String method = 'parse'+ type
         c.protocol = this."$method"(xml.protocol, parent,
                                     (path != '/' ? path +'/protocol' : '/protocol'),
                                     (dataPath != '/' ? dataPath +'/protocol' : '/protocol')
                                    )         
      }
      
      if (!xml.guideline_id.isEmpty())
      {
         c.guideline_id = this.parseOBJECT_REF(xml.guideline_id)
      }
      
      this.fillENTRY(c, xml, parent, path, dataPath)
   }
   

   // ========= PARSE METHODS =========

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
            throw new XmlCompositionParseException("@xsi:type required for "+ dataPath +".data")
         }
         def method = 'parse'+ type
         ov.data = this."$method"(xml.data, null, '/', '/')
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
         throw new XmlCompositionParseException("@xsi:type required for "+ dataPath +".committer")
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
         throw new XmlCompositionParseException("@xsi:type required for "+ dataPath +".committer")
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
         throw new XmlCompositionParseException("@xsi:type required for "+ dataPath +".reason")
      }
      method = 'parse'+ type
      at.reason = this."$method"(xml.reason)
      
      // TODO: test if this is parsed as a boolean or as a string
      at.is_pending = xml.is_pending.toBoolean()
      
      return at
   }

      
   private Composition parseCOMPOSITION(GPathResult xml, Pathable parent, String path, String dataPath)
   {
      Composition compo = new Composition()

      this.fillLOCATABLE(compo, xml, parent, path, dataPath)
      
      compo.language  = this.parseCODE_PHRASE(xml.language)
      compo.territory = this.parseCODE_PHRASE(xml.territory)
      compo.category  = this.parseDV_CODED_TEXT(xml.category)
      
      String type, method
      
      type = xml.composer.'@xsi:type'.text() // party proxy or descendants
      if (!type)
      {
         throw new XmlCompositionParseException("@xsi:type required for "+ dataPath +".composer")
      }
      method = 'parse'+ type
      compo.composer = this."$method"(xml.composer)

      // NOTE: these paths are not using the node_id, are just attribute paths
      compo.context = parseEVENT_CONTEXT(xml.context, compo,
                                         (path != '/' ? path +'/context' : '/context'),
                                         (dataPath != '/' ? dataPath +'/context' : '/context')
                                        )
      
      def content = []
      
      xml.content.eachWithIndex { content_item, i ->
         type = content_item.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlCompositionParseException("@xsi:type required for "+ dataPath +".content[$i]")
         }
         method = 'parse'+ type
         compo.content.add(
            this."$method"(content_item, compo,
                           (path != '/' ? path +'/content' : '/content'),
                           (dataPath != '/' ? dataPath +'/content['+ i +']' : '/content['+ i +']')
                          )
         )
      }
      
      return compo
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
         throw new XmlCompositionParseException("@xsi:type required for OBJECT_REF.id")
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
         throw new XmlCompositionParseException("@xsi:type required for PARTY_REF.id")
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
         throw new XmlCompositionParseException("@xsi:type required for LOCATABLE_REF.id")
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
   

   private EventContext parseEVENT_CONTEXT(GPathResult xml, Pathable parent, String path, String dataPath)
   {
      EventContext e = new EventContext()

      this.fillPATHABLE(e, parent, path, dataPath)

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
            throw new XmlCompositionParseException("@xsi:type required for "+ dataPath +".other_context")
         }
         method = 'parse'+ type
         e.other_context = this."$method"(xml.other_context, e,
            path + (path != '/' ? '/' : '') + 'other_context',
            dataPath + (dataPath != '/' ? '/' : '') + 'other_context'
         )
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
         throw new XmlCompositionParseException("@xsi:type required for PARTICIPATION.performer")
      }
      String method = 'parse'+ type
      p.performer = this."$method"(xml.performer)
      
      return p
   }
   
   private Section parseSECTION(GPathResult xml, Pathable parent, String path, String dataPath)
   {
      Section section = new Section()
      
      this.fillLOCATABLE(section, xml, parent, path, dataPath)
      
      String type, method
      
      xml.items.eachWithIndex { content_item, i ->
         type = content_item.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlCompositionParseException("@xsi:type required for "+ dataPath +".items[$i]")
         }
         method = 'parse'+ type
         section.items.add(
            this."$method"(content_item, section,
                           (path != '/' ? path +'/items' : '/items'),
                           (dataPath != '/' ? dataPath +'/items['+ i +']' : '/items['+ i +']')
                          )
         )
      }
      
      return section
   }
   
   private AdminEntry parseADMIN_ENTRY(GPathResult xml, Pathable parent, String path, String dataPath)
   {
      AdminEntry a = new AdminEntry()
      
      this.fillENTRY(a, xml, parent, path, dataPath)
      
      String type = xml.data.'@xsi:type'.text()
      if (!type)
      {
         throw new XmlCompositionParseException("@xsi:type required for "+ dataPath +".data")
      }
      String method = 'parse'+ type
      a.data = this."$method"(xml.data, a,
                               (path != '/' ? path +'/data' : '/data'),
                               (dataPath != '/' ? dataPath +'/data' : '/data')
                             )
      
      return a
   }
   
   private Observation parseOBSERVATION(GPathResult xml, Pathable parent, String path, String dataPath)
   {
      Observation o = new Observation()
      
      this.fillCARE_ENTRY(o, xml, parent, path, dataPath)
      
      if (!xml.data.isEmpty())
      {
         o.data = this.parseHISTORY(xml.data, o,
                                     (path != '/' ? path +'/data' : '/data'),
                                     (dataPath != '/' ? dataPath +'/data' : '/data')
                                   )
      }
      
      if (!xml.state.isEmpty())
      {
         o.state = this.parseHISTORY(xml.state, o,
                                     (path != '/' ? path +'/state' : '/state'),
                                     (dataPath != '/' ? dataPath +'/state' : '/state')
                                    )
      }
      
      return o
   }
   
   private History parseHISTORY(GPathResult xml, Pathable parent, String path, String dataPath)
   {
      History h = new History()
      
      this.fillLOCATABLE(h, xml, parent, path, dataPath)
      
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
         h.events.add(
            this."$method"(event, h,
                           (path != '/' ? path +'/events' : '/events'),
                           (dataPath != '/' ? dataPath +'/events['+ i +']' : '/events['+ i +']')
                          )
         )
      }     
      
      return h
   }
   
   private PointEvent parsePOINT_EVENT(GPathResult xml, Pathable parent, String path, String dataPath)
   {
      PointEvent e = new PointEvent()
      
      this.fillLOCATABLE(e, xml, parent, path, dataPath)
      
      e.time = this.parseDV_DATE_TIME(xml.time)
      
      String type, method

      if (!xml.data.isEmpty())
      {
         type = xml.data.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlCompositionParseException("@xsi:type required for "+ dataPath +".data")
         }
         method = 'parse'+ type
         e.data = this."$method"(xml.data, e,
                                 (path != '/' ? path +'/data' : '/data'),
                                 (dataPath != '/' ? dataPath +'/data' : '/data')
                                )
      }

      if (!xml.state.isEmpty())
      {         
         type = xml.state.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlCompositionParseException("@xsi:type required for "+ dataPath +".state")
         }
         method = 'parse'+ type
         e.state = this."$method"(xml.state, e,
                                  (path != '/' ? path +'/state' : '/state'),
                                  (dataPath != '/' ? dataPath +'/state' : '/state')
                                 )
      }
      
      return e
   }
   
   private IntervalEvent parseINTERVAL_EVENT(GPathResult xml, Pathable parent, String path, String dataPath)
   {
      IntervalEvent e = new IntervalEvent()
      
      this.fillLOCATABLE(e, xml, parent, path, dataPath)
      
      e.time = this.parseDV_DATE_TIME(xml.time)
      
      String type, method

      if (!xml.data.isEmpty())
      {
         type = xml.data.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlCompositionParseException("@xsi:type required for "+ dataPath +".data")
         }
         method = 'parse'+ type
         e.data = this."$method"(xml.data, e,
                                 (path != '/' ? path +'/data' : '/data'),
                                 (dataPath != '/' ? dataPath +'/data' : '/data')
                                )
      }
      
      if (!xml.state.isEmpty())
      {
         type = xml.state.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlCompositionParseException("@xsi:type required for "+ dataPath +".state")
         }
         method = 'parse'+ type
         e.state = this."$method"(xml.state, e,
                                  (path != '/' ? path +'/state' : '/state'),
                                  (dataPath != '/' ? dataPath +'/state' : '/state')
                                 )
      }
      
      e.width = this.parseDV_DURATION(xml.width)
      
      e.math_function = this.parseDV_CODED_TEXT(xml.math_function)
      
      if (!xml.sample_count.isEmpty())
      {         
         e.sample_count = xml.sample_count
      }
      
      return e
   }
   
   private Evaluation parseEVALUATION(GPathResult xml, Pathable parent, String path, String dataPath)
   {
      Evaluation e = new Evaluation()
      
      this.fillCARE_ENTRY(e, xml, parent, path, dataPath)
      
      String type = xml.data.'@xsi:type'.text()
      if (!type)
      {
         throw new XmlCompositionParseException("@xsi:type required for "+ dataPath +".data")
      }
      String method = 'parse'+ type
      e.data = this."$method"(xml.data, e,
                               (path != '/' ? path +'/data' : '/data'),
                               (dataPath != '/' ? dataPath +'/data' : '/data')
                             )
      
      return e
   }
   
   private Instruction parseINSTRUCTION(GPathResult xml, Pathable parent, String path, String dataPath)
   {
      Instruction ins = new Instruction()
      
      this.fillCARE_ENTRY(ins, xml, parent, path, dataPath)
      
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
            this.parseACTIVITY(js_activity, ins,
                        (path != '/' ? path +'/activities' : '/activities'),
                        (dataPath != '/' ? dataPath +'/activities['+ i +']' : '/activities['+ i +']')
                       )
         )
      }
      
      return ins
   }
   
   private Action parseACTION(GPathResult xml, Pathable parent, String path, String dataPath)
   {
      Action a = new Action()
      
      this.fillCARE_ENTRY(a, xml, parent, path, dataPath)
      
      String type = xml.description.'@xsi:type'.text()
      if (!type)
      {
         throw new XmlCompositionParseException("@xsi:type required for "+ dataPath +".description")
      }
      String method = 'parse'+ type

      a.description = this."$method"(xml.description, a,
         (path != '/' ? path +'/description' : '/description'),
         (dataPath != '/' ? dataPath +'/description' : '/description')
      )
      
      a.time = this.parseDV_DATE_TIME(xml.time)

      a.ism_transition = this.parseISM_TRANSITION(xml.ism_transition, a,
                                 (path != '/' ? path +'/ism_transition' : '/ism_transition'),
                                 (dataPath != '/' ? dataPath +'/ism_transition' : '/ism_transition')
                              )
      
      if (!xml.instruction_details.isEmpty())
         a.instruction_details = this.parseINSTRUCTION_DETAILS(xml.instruction_details, a,
                                 (path != '/' ? path +'/instruction_details' : '/instruction_details'),
                                 (dataPath != '/' ? dataPath +'/instruction_details' : '/instruction_details')
                              )
      
      return a
   }

   private IsmTransition parseISM_TRANSITION(GPathResult xml, Pathable parent, String path, String dataPath)
   {
      IsmTransition i = new IsmTransition()

      this.fillPATHABLE(i, parent, path, dataPath)

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
   
   private InstructionDetails parseINSTRUCTION_DETAILS(GPathResult xml, Pathable parent, String path, String dataPath)
   {
      InstructionDetails i = new InstructionDetails()

      this.fillPATHABLE(i, parent, path, dataPath)
      
      i.instruction_id = this.parseLOCATABLE_REF(xml.instruction_id)
      
      i.activity_id = xml.activity_id
      
      if (!xml.wf_details.isEmpty())
      {
         String type = xml.wf_details.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlCompositionParseException("@xsi:type required for "+ dataPath +".wf_details")
         }
         String method = 'parse'+ type
         i.wf_details = this."$method"(xml.wf_details)
      }
      
      return i
   }
   
   private Activity parseACTIVITY(GPathResult xml, Pathable parent, String path, String dataPath)
   {
      String type = xml.description.'@xsi:type'.text()
      if (!type)
      {
         throw new XmlCompositionParseException("@xsi:type required for "+ dataPath +".description")
      }
      String method = 'parse'+ type
      
      Activity a = new Activity(
         action_archetype_id: xml.action_archetype_id
      )

      a.description = this."$method"(xml.description, a,
         (path != '/' ? path +'/description' : '/description'),
         (dataPath != '/' ? dataPath +'/description' : '/description')
      )

      if (!xml.timing.isEmpty())
      {
         a.timing = this.parseDV_PARSABLE(xml.timing)
      }
      
      this.fillLOCATABLE(a, xml, parent, path, dataPath)
      
      return a
   }
   

   
   private ItemTree parseITEM_TREE(GPathResult xml, Pathable parent, String path, String dataPath)
   {
      ItemTree t = new ItemTree()
      
      this.fillLOCATABLE(t, xml, parent, path, dataPath)
      
      String type, method
      
      xml.items.eachWithIndex { item, i ->
         type = item.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlCompositionParseException("@xsi:type required for "+ dataPath +".items[$i]")
         }
         method = 'parse'+ type
         t.items.add(
            this."$method"(item, t,
                        (path != '/' ? path +'/items' : '/items'),
                        (dataPath != '/' ? dataPath +'/items['+ i +']' : '/items['+ i +']')
                       )
         )
      }
      
      return t
   }
   
   private ItemList parseITEM_LIST(GPathResult xml, Pathable parent, String path, String dataPath)
   {
      ItemList l = new ItemList()
      
      this.fillLOCATABLE(l, xml, parent, path, dataPath)
      
      xml.items.eachWithIndex { element, i ->
         l.items.add(
            this.parseELEMENT(element, l,
                        (path != '/' ? path +'/items' : '/items'),
                        (dataPath != '/' ? dataPath +'/items['+ i +']' : '/items['+ i +']')
                       )
         )
      }
      
      return l
   }
   
   private ItemTable parseITEM_TABLE(GPathResult xml, Pathable parent, String path, String dataPath)
   {
      ItemTable t = new ItemTable()
      
      this.fillLOCATABLE(t, xml, parent, path, dataPath)
      
      String type, method
      
	   // FIXME: rows are CLUSTERS, we don't need to get the dynamic method
      xml.rows.eachWithIndex { item, i -> 
         type = item.'@xsi:type'.text()
         method = 'parse'+ type
         t.rows.add(
            this."$method"(item, t, 
                            (path != '/' ? path +'/rows' : '/rows'),
                            (dataPath != '/' ? dataPath +'/rows['+ i +']' : '/rows['+ i +']')
                          )
         )
      }
      
      return t
   }
   
   private ItemSingle parseITEM_SINGLE(GPathResult xml, Pathable parent, String path, String dataPath)
   {
      ItemSingle s = new ItemSingle()
      
      this.fillLOCATABLE(s, xml, parent, path, dataPath)
      
      s.item = this.parseELEMENT(xml.item, s,
                                 (path != '/' ? path +'/item' : '/item'),
                                 (dataPath != '/' ? dataPath +'/item' : '/item')
                              )
      
      return s
   }
   
   private Cluster parseCLUSTER(GPathResult xml, Pathable parent, String path, String dataPath)
   {
      Cluster c = new Cluster()
      
      this.fillLOCATABLE(c, xml, parent, path, dataPath)
      
      String type, method
      
      xml.items.eachWithIndex { item, i ->
         type = item.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlCompositionParseException("@xsi:type required for "+ dataPath +".items[$i]")
         }
         method = 'parse'+ type
         c.items.add(
            this."$method"(item, c,
                           (path != '/' ? path +'/items' : '/items'),
                           (dataPath != '/' ? dataPath +'/items['+ i +']' : '/items['+ i +']')
                          )
         )
      }
      
      return c
   }
   
   private Element parseELEMENT(GPathResult xml, Pathable parent, String path, String dataPath)
   {
      Element e = new Element()
      
      this.fillLOCATABLE(e, xml, parent, path, dataPath)
      
      if (!xml.value.isEmpty())
      {
         String type = xml.value.'@xsi:type'.text()
         if (!type)
         {
            throw new XmlCompositionParseException("@xsi:type required for "+ dataPath +".value")
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
         value: xml.value
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