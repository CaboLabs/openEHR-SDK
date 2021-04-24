package com.cabolabs.openehr.formats

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Archetyped
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
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
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDate
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDuration
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvTime
import com.cabolabs.openehr.rm_1_0_2.data_types.text.CodePhrase
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvCodedText
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvText
import com.cabolabs.openehr.rm_1_0_2.data_types.text.TermMapping
import com.cabolabs.openehr.rm_1_0_2.data_types.uri.DvEhrUri
import com.cabolabs.openehr.rm_1_0_2.data_types.uri.DvUri
import com.cabolabs.openehr.rm_1_0_2.support.identification.*

// Old Groovy
import groovy.util.XmlSlurper
import groovy.util.slurpersupport.GPathResult

// New Groovy 3.0.7+
//import groovy.xml.XmlSlurper
//import groovy.xml.slurpersupport.GPathResult

class OpenEhrXmlParser {
   
   // ========= ENTRY POINTS =========

   // used to parse compositions and other descendant from Locatable
   Locatable parseXml(String xml)
   {
      def slurper = new XmlSlurper(false, false)
      def gpath = slurper.parseText(xml)
      String type = gpath.'@xsi:type'.text()
      
      if (!type)
      {
         throw new Exception("Can't parse XML if root node doesn't have a xsi:type")
      }
      
      def method = 'parse'+ type
      return this."$method"(gpath)
   }

   // used to parse versions because is not Locatable
   Version parseVersionXml(String xml)
   {
      def slurper = new XmlSlurper(false, false)
      def gpath = slurper.parseText(xml)
      String type = gpath.'@xsi:type'.text()
      
      if (!type)
      {
         throw new Exception("Can't parse XML if root node doesn't have a xsi:type")
      }
      
      def method = 'parse'+ type
      return this."$method"(gpath)
   }

   // ========= FIll METHODS =========

   private void fillLOCATABLE(Locatable l, GPathResult xml)
   {
      // name can be text or coded
      String type = xml.name.'@xsi:type'.text()
      if (!type) type = 'DV_TEXT'
      String method = 'parse'+ type
      
      l.name = this."$method"(xml.name)
      
      l.archetype_node_id = xml.archetype_node_id
      
      if (!xml.uid.isEmpty())
      {
         type = xml.uid.'@xsi:type'.text()
         method = 'parse'+ type
         l.uid = this."$method"(xml.uid)
      }
      
      if (!xml.archetype_details.isEmpty())
         l.archetype_details = this.parseARCHETYPED(xml.archetype_details)
   }
   
   private void fillENTRY(Entry e, GPathResult xml)
   {
      String type, method
      
      
      e.encoding = this.parseCODE_PHRASE(xml.encoding)
      e.language = this.parseCODE_PHRASE(xml.language)
      
      
      type = xml.subject.'@xsi:type'.text()
      method = 'parse'+ type
      e.subject = this."$method"(xml.subject)
      
      
      if (!xml.provider.isEmpty())
      {
         type = xml.provider.'@xsi:type'.text()
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
   
   private void fillCARE_ENTRY(CareEntry c, GPathResult xml)
   {
      if (!xml.protocol.isEmpty())
      {
         String type = xml.protocol.'@xsi:type'.text()
         String method = 'parse'+ type
         c.protocol = this."$method"(xml.protocol)         
      }
      
      if (!xml.guideline_id.isEmpty())
      {
         c.guideline_id = this.parseOBJECT_REF(xml.guideline_id)
      }
      
      this.fillENTRY(c, xml)
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
      method = 'parse'+ type
      at.reason = this."$method"(xml.reason)
      
      // TODO: test if this is parsed as a boolean or as a string
      at.is_pending = xml.is_pending.toBoolean()
      
      return at
   }

      
   private Composition parseCOMPOSITION(GPathResult xml)
   {
      Composition compo = new Composition()
      println "compo"+ xml
      this.fillLOCATABLE(compo, xml)
      
      compo.language = this.parseCODE_PHRASE(xml.language)
      compo.territory = this.parseCODE_PHRASE(xml.territory)
      compo.category = this.parseDV_CODED_TEXT(xml.category)
      
      String type, method
      
      type = xml.composer.'@xsi:type'.text() // party proxy or descendants
      method = 'parse'+ type
      compo.composer = this."$method"(xml.composer)
      
      compo.context = parseEVENT_CONTEXT(xml.context)
      
      def content = []
      
      xml.content.each { content_item ->
         type = content_item.'@xsi:type'.text()
         method = 'parse'+ type
         compo.content.add(this."$method"(content_item))
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
   
   private EventContext parseEVENT_CONTEXT(GPathResult xml)
   {
      EventContext e = new EventContext()
      e.start_time = this.parseDV_DATE_TIME(xml.start_time)
      
      if (e.end_time)
         e.end_time = this.parseDV_DATE_TIME(xml.end_time)
      
      e.location = xml.location
      
      e.setting = this.parseDV_CODED_TEXT(xml.setting)
      
      if (!xml.other_context.isEmpty())
      {         
         String type, method
         type = xml.other_context.'@xsi:type'.text()
         method = 'parse'+ type
         e.other_context = this."$method"(xml.other_context)
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
      String method = 'parse'+ type
      p.performer = this."$method"(xml.performer)
      
      return p
   }
   
   private Section parseSECTION(GPathResult xml)
   {
      Section s = new Section()
      
      this.fillLOCATABLE(s, xml)
      
      String type, method
      
      xml.items.each { content_item ->
         type = content_item.'@xsi:type'.text()
         method = 'parse'+ type
         this."$method"(content_item)
      }
      
      return s
   }
   
   private AdminEntry parseADMIN_ENTRY(GPathResult xml)
   {
      AdminEntry a = new AdminEntry()
      
      this.fillLOCATABLE(a, xml)
      this.fillENTRY(a, xml)
      
      String type = xml.data.'@xsi:type'.text()
      String method = 'parse'+ type
      a.data = this."$method"(xml.data)
      
      return a
   }
   
   private Observation parseOBSERVATION(GPathResult xml)
   {
      Observation o = new Observation()
      
      this.fillLOCATABLE(o, xml)
      this.fillCARE_ENTRY(o, xml)
      
      if (!xml.data.isEmpty())
      {
         o.data = this.parseHISTORY(xml.data)
      }
      
      if (!xml.state.isEmpty())
      {
         o.state = this.parseHISTORY(xml.state)
      }
      
      return o
   }
   
   private History parseHISTORY(GPathResult xml)
   {
      History h = new History()
      
      this.fillLOCATABLE(h, xml)
      
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
      xml.events.each { event ->
         type = event.'@xsi:type'.text()
         method = 'parse'+ type
         h.events.add("$method"(event))
      }     
      
      return h
   }
   
   private PointEvent parsePOINT_EVENT(GPathResult xml)
   {
      PointEvent e = new PointEvent()
      
      this.fillLOCATABLE(e, xml)
      
      e.time = this.parseDV_DATE_TIME(xml.time)
      
      String type, method
      
      if (!xml.state.isEmpty())
      {         
         type = xml.state.'@xsi:type'.text()
         method = 'parse'+ type
         e.state = this."$method"(xml.state)
      }
      
      if (!xml.data.isEmpty())
      {
         type = xml.data.'@xsi:type'.text()
         method = 'parse'+ type
         e.data = this."$method"(xml.data)
      }
      
      return e
   }
   
   private IntervalEvent parseINTERVAL_EVENT(GPathResult xml)
   {
      IntervalEvent e = new IntervalEvent()
      
      this.fillLOCATABLE(e, xml)
      
      e.time = this.parseDV_DATE_TIME(xml.time)
      
      String type, method
      
      if (!xml.state.isEmpty())
      {
         type = xml.state.'@xsi:type'.text()
         method = 'parse'+ type
         e.state = this."$method"(xml.state)
      }
      
      if (!xml.data.isEmpty())
      {
         type = xml.data.'@xsi:type'.text()
         method = 'parse'+ type
         e.data = this."$method"(xml.data)
      }
      
      e.width = this.parseDV_DURATION(xml.width)
      
      e.math_function = this.parseDV_CODED_TEXT(xml.math_function)
      
      if (!xml.sample_count.isEmpty())
      {         
         e.sample_count = xml.sample_count
      }
      
      return e
   }
   
   private Evaluation parseEVALUATION(GPathResult xml)
   {
      Evaluation e = new Evaluation()
      
      this.fillLOCATABLE(e, xml)
      this.fillCARE_ENTRY(e, xml)
      
      String type = xml.data.'@xsi:type'.text()
      String method = 'parse'+ type
      e.data = this."$method"(xml.data)
      
      return e
   }
   
   private Instruction parseINSTRUCTION(GPathResult xml)
   {
      Instruction i = new Instruction()
      
      this.fillLOCATABLE(i, xml)
      this.fillCARE_ENTRY(i, xml)
      
      String type, method
      
      
      type = xml.narrative.'@xsi:type'.text()
      if (!type) type = 'DV_TEXT'
      method = 'parse'+ type
      i.narrative = this."$method"(xml.narrative)
      
      
      if (!xml.expiry_time.isEmpty())
         i.expiry_time = this.parseDV_DATE_TIME(xml.expiry_time)
      
         
      if (!xml.wf_definition.isEmpty())
         i.wf_definition = this.parseDV_PARSABLE(xml.wf_definition)
      
      
      xml.activities.each { js_activity ->
         
         i.activities.add(this.parseACTIVITY(js_activity))
      }
      
      return i
   }
   
   private Action parseACTION(GPathResult xml)
   {
      Action a = new Action()
      
      this.fillLOCATABLE(a, xml)
      this.fillCARE_ENTRY(a, xml)
      
      String type = xml.description.'@xsi:type'.text()
      String method = 'parse'+ type
      a.description = this."$method"(xml.description)
      
      a.time = this.parseDV_DATE_TIME(xml.time)

      a.ism_transition = this.parseISM_TRANSITION(xml.ism_transition)
      
      if (!xml.instruction_details.isEmpty())
         a.instruction_details = this.parseINSTRUCTION_DETAILS(xml.instruction_details)
      
      return a
   }

   private IsmTransition parseISM_TRANSITION(GPathResult xml)
   {
      IsmTransition i = new IsmTransition()

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
   
   private InstructionDetails parseINSTRUCTION_DETAILS(GPathResult xml)
   {
      InstructionDetails i = new InstructionDetails()
      
      i.instruction_id = this.parseLOCATABLE_REF(xml.instruction_id)
      
      i.activity_id = xml.activity_id
      
      if (!xml.wf_details.isEmpty())
      {
         String type = xml.wf_details.'@xsi:type'.text()
         String method = 'parse'+ type
         i.wf_details = this."$method"(xml.wf_details)
      }
      
      return i
   }
   
   private Activity parseACTIVITY(GPathResult xml)
   {
      String type = xml.description.'@xsi:type'.text()
      String method = 'parse'+ type
      
      Activity a = new Activity(
         description: this."$method"(xml.description),
         action_archetype_id: xml.action_archetype_id
      )
      
      if (!xml.timing.isEmpty())
      {
         a.timing = this.parseDV_PARSABLE(xml.timing)
      }
      
      this.fillLOCATABLE(a, xml)
      
      return a
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
         type: xml.type.toInteger(),
         precision: xml.precision?.toInteger()
      )
      
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
      
      d.data = xml.data.text().getBytes()
      
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
   
   
   private ItemTree parseITEM_TREE(GPathResult xml)
   {
      ItemTree t = new ItemTree()
      
      this.fillLOCATABLE(t, xml)
      
      String type, method
      
      xml.items.each { item ->
         type = item.'@xsi:type'.text()
         method = 'parse'+ type
         //println " - " + method
         t.items.add(this."$method"(item))
      }
      
      return t
   }
   
   private ItemList parseITEM_LIST(GPathResult xml)
   {
      ItemList l = new ItemList()
      
      this.fillLOCATABLE(l, xml)
      
      xml.items.each { element ->
         l.items.add(this.parseELEMENT(element))
      }
      
      return l
   }
   
   private ItemTable parseITEM_TABLE(GPathResult xml)
   {
      ItemTable t = new ItemTable()
      
      this.fillLOCATABLE(t, xml)
      
      String type, method
      
	  // FIXME: rows are CLUSTERS, we don't need to get the dynamic method
      xml.rows.each { item -> 
         type = item.'@xsi:type'.text()
         method = 'parse'+ type
         t.items.add(this."$method"(item))
      }
      
      return t
   }
   
   private ItemSingle parseITEM_SINGLE(GPathResult xml)
   {
      ItemSingle s = new ItemSingle()
      
      this.fillLOCATABLE(s, xml)
      
      s.item = this.parseELEMENT(xml.item)
      
      return s
   }
   
   private Cluster parseCLUSTER(GPathResult xml)
   {
      Cluster c = new Cluster()
      
      this.fillLOCATABLE(c, xml)
      
      String type, method
      
      xml.items.each { item ->
         type = item.'@xsi:type'.text()
         method = 'parse'+ type
         c.items.add(this."$method"(item))
      }
      
      return c
   }
   
   private Element parseELEMENT(GPathResult xml)
   {
      Element e = new Element()
      
      this.fillLOCATABLE(e, xml)
      
      if (!xml.value.isEmpty())
      {
         String type = xml.value.'@xsi:type'.text()
         String method = 'parse'+ type
         e.value = this."$method"(xml.value)
      }
      
      if (!xml.null_flavour.isEmpty())
      {
         e.null_flavour = this.parseDV_CODED_TEXT(xml.null_flavour)
      }
      
      return e
   }
   
   private DvInterval parseDV_INTERVAL(GPathResult xml)
   {
      DvInterval i = new DvInterval()
      
      String type, method
      
      type = xml.lower ? xml.lower.'@xsi:type'.text() : xml.upper.'@xsi:type'.text()
      method = 'parse'+ type
      
      if (!xml.lower.isEmpty())
      {         
         i.lower = this."$method"(xml.lower)
      }
      
      if (!xml.upper.isEmpty())
      {
         i.upper = this."$method"(xml.upper)
      }
      
      i.lower_included = xml.lower_included
      i.lower_unbounded = xml.lower_unbounded
      i.upper_included = xml.upper_included
      i.upper_unbounded = xml.upper_unbounded
      
      return i
   }

}