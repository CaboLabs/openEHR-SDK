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
import groovy.json.JsonSlurper

class OpenEhrJsonParser {

   // ========= ENTRY POINTS =========

   // used to parse compositions and other descendant from Locatable
   Locatable parseJson(String json)
   {
      def slurper = new JsonSlurper()
      def map = slurper.parseText(json)
      String type = map._type
      
      if (!type)
      {
         throw new Exception("Can't parse JSON if root node doesn't have a value for _type")
      }
      
      def method = 'parse'+ type +'Map'
      return this."$method"(map)
   }
   
   // used to parse versions because is not Locatable
   Version parseVersionJson(String json)
   {
      def slurper = new JsonSlurper()
      def map = slurper.parseText(json)
      String type = map._type
      
      if (!type)
      {
         throw new Exception("Can't parse JSON if root node doesn't have a value for _type")
      }
      
      def method = 'parse'+ type
      return this."$method"(map)
   }

   // ========= FIll METHODS =========
   
   private void fillLOCATABLE(Locatable l, Map json)
   {
      // name can be text or coded
      String type = json.name._type
      String method = 'parse'+ type +'Map'
      
      l.name = this."$method"(json.name)
      
      l.archetype_node_id = json.archetype_node_id
      
      if (json.uid)
      {
         type = json.uid._type
         method = 'parse'+ type +'Map'
         l.uid = this."$method"(json.uid)
      }
      
      if (json.archetype_details)
         l.archetype_details = this.parseARCHETYPEDMap(json.archetype_details)
   }
   
   private void fillENTRY(Entry e, Map json)
   {
      String type, method
      
      
      e.encoding = this.parseCODE_PHRASEMap(json.encoding)
      e.language = this.parseCODE_PHRASEMap(json.language)
      
      
      type = json.subject._type
      method = 'parse'+ type +'Map'
      e.subject = this."$method"(json.subject)
      
      
      if (json.provider)
      {
         type = json.provider._type
         method = 'parse'+ type +'Map'
         e.provider = this."$method"(json.provider)
      }
      
      
      if (json.other_participations)
      {
         def participation
         json.other_participations.each { _participation ->

            participation = this.parsePARTICIPATION(_participation)
            e.other_participations.add(participation)
         }
      }
      
      
      if (json.workflow_id)
      {
         e.workflow_id = this.parseOBJECT_REF(json.workflow_id)
      }
   }
   
   private void fillCARE_ENTRY(CareEntry c, Map json)
   {
      if (json.protocol)
      {
         String type = json.protocol._type
         String method = 'parse'+ type +'Map'
         c.protocol = this."$method"(json.protocol)         
      }
      
      if (json.guideline_id)
      {
         c.guideline_id = this.parseOBJECT_REF(json.guideline_id)
      }
      
      this.fillENTRY(c, json)
   }
   
   private void fillDV_ORDERED(DvOrdered d, Map json)
   {
      if (json.normal_status)
      {
         d.normal_status = this.parseCODE_PHRASEMap(json.normal_status)
      }
      
      if (json.normal_range)
      {
         d.normal_range = this.parseDV_INTERVAL(json.normal_range)
      }
      
      if (json.other_reference_ranges)
      {
         def ref_range
         json.other_reference_ranges.each { _reference_range ->
            
            ref_range = this.parseREFERENCE_RANGE(_reference_range)
            d.other_reference_ranges.add(ref_range)
         }
      }
   }

   // ========= PARSE METHODS =========

   private OriginalVersion parseORIGINAL_VERSION(Map json)
   {
      OriginalVersion ov = new OriginalVersion()
      
      ov.uid = this.parseOBJECT_VERSION_IDMap(json.uid)
      
      if (json.signature)
      {
         ov.signature = json.signature
      }
      
      if (json.preceding_version_uid)
      {
         ov.preceding_version_uid = this.parseOBJECT_VERSION_IDMap(json.preceding_version_uid)
      }
      
      // TODO: other_input_version_uids
      
      ov.lifecycle_state = this.parseDV_CODED_TEXTMap(json.lifecycle_state)
      
      ov.contribution = this.parseOBJECT_REF(json.contribution)
      
      // TODO: AuditDetails could be subclass ATTESTATION
      ov.commit_audit = this.parseAUDIT_DETAILS(json.commit_audit)
      
      if (json.attestations)
      {
         json.attestations.each { attestation ->
            
            ov.attestations.add(this.parseATTESTATION(attestation))
         }
      }
      
      if (json.data)
      {
         def type = json.data._type
         def method = 'parse'+ type +'Map'
         ov.data = this."$method"(json.data)
      }
      
      return ov
   }
   
   private AuditDetails parseAUDIT_DETAILS(Map json)
   {
      AuditDetails ad = new AuditDetails()
      
      ad.system_id      = json.system_id
      ad.time_committed = this.parseDV_DATE_TIMEMap(json.time_committed)
      ad.change_type    = this.parseDV_CODED_TEXTMap(json.change_type)
      
      if (json.description)
      {
         ad.description = this.parseDV_TEXTMap(json.description)
      }
      
      def type = json.committer._type
      def method = 'parse'+ type +'Map'
      ad.committer = this."$method"(json.committer)
      
      return ad
   }
   
   private Attestation parseATTESTATION(Map json)
   {
      Attestation at = new Attestation()
      
      // AuditDetails fields
      at.system_id      = json.system_id
      at.time_committed = this.parseDV_DATE_TIMEMap(json.time_committed)
      at.change_type    = this.parseDV_CODED_TEXTMap(json.change_type)
      
      if (json.description)
      {
         at.description = this.parseDV_TEXTMap(json.description)
      }
      
      def type = json.committer._type
      def method = 'parse'+ type +'Map'
      at.committer = this."$method"(json.committer)
      
      // Attestation fields
      if (json.attested_view)
      {
         at.attested_view = this.parseDV_MULTIMEDIAMap(json.attested_view)
      }
      
      if (json.proof)
      {
         at.proof = json.proof
      }
      
      // TODO: json.items

      type = json.reason._type // text or coded
      method = 'parse'+ type +'Map'
      at.reason = this."$method"(json.reason)
      
      // TODO: test if this is parsed as a boolean or as a string
      at.is_pending = json.is_pending.toBoolean()
      
      return at
   }

      
   private Composition parseCOMPOSITIONMap(Map json)
   {
      Composition compo = new Composition()
      this.fillLOCATABLE(compo, json)
      
      compo.language = this.parseCODE_PHRASEMap(json.language)
      compo.territory = this.parseCODE_PHRASEMap(json.territory)
      compo.category = this.parseDV_CODED_TEXTMap(json.category)
      
      String type, method
      
      type = json.composer._type // party proxy or descendants
      method = 'parse'+ type +'Map'
      compo.composer = this."$method"(json.composer)
      
      compo.context = parseEVENT_CONTEXTMap(json.context)
      
      def content = []
      
      json.content.each { content_item ->
         type = content_item._type
         method = 'parse'+ type +'Map'
         compo.content.add(this."$method"(content_item))
      }
      
      return compo
   }

   private ReferenceRange parseREFERENCE_RANGE(Map json)
   {
      ReferenceRange rr = new ReferenceRange()

      rr.meaning = this.parseDV_TEXTMap(json.meaning)

      rr.range = this.parseDV_INTERVAL(json.range)

      return rr
   }

   
   private void fillDV_QUANTIFIED(DvQuantified d, Map json)
   {
      this.fillDV_ORDERED(d, json)
      
      if (json.magnitude_status)
      {
         d.magnitude_status = json.magnitude_status
      }
   }
   
   private void fillDV_AMOUNT(DvAmount d, Map json)
   {
      this.fillDV_ORDERED(d, json)
      
      if (json.accuracy)
      {
         d.accuracy = json.accuracy
      }
      
      if (json.accuracy_is_percent)
      {
         d.accuracy_is_percent = json.accuracy_is_percent
      }
   }
   
   private ArchetypeId parseARCHETYPE_IDMap(Map json)
   {
      new ArchetypeId(value: json.value)
   }
   
   private TemplateId parseTEMPLATE_IDMap(Map json)
   {
      new TemplateId(value: json.value)
   }
   
   private Archetyped parseARCHETYPEDMap(Map json)
   {
      Archetyped a = new Archetyped()
      a.archetype_id = this.parseARCHETYPE_IDMap(json.archetype_id)
      a.template_id = this.parseTEMPLATE_IDMap(json.template_id)
      a.rm_version = json.rm_version
      return a
   }
   
   private PartySelf parsePARTY_SELFMap(Map json)
   {
      PartySelf p = new PartySelf()
      
      if (json.external_ref)
      {
         p.external_ref = this.parsePARTY_REFMap(json.external_ref)
      }
      
      return p
   }
   
   private PartyIdentified parsePARTY_IDENTIFIEDMap(Map json)
   {
      PartyIdentified p = new PartyIdentified()
      
      if (json.name)
      {
         p.name = json.name
      }
      
      json.identifiers.each { identifier ->
         
         p.identifiers.add(this.parseDV_IDENTIFIERMap(identifier))
      }
      
      if (json.external_ref)
      {
         p.external_ref = this.parsePARTY_REFMap(json.external_ref)
      }
      
      return p
   }
   
   private PartyRelated parsePARTY_RELATEDMap(Map json)
   {
      PartyRelated p = new PartyRelated()
      
      if (json.name)
      {
         p.name = json.name
      }
      
      json.identifiers.each { identifier ->
         
         p.identifiers.add(this.parseDV_IDENTIFIERMap(identifier))
      }
      
      if (json.external_ref)
      {
         p.external_ref = this.parsePARTY_REFMap(json.external_ref)
      }
      
      if (json.relationship)
      {
         p.relationship = this.parseDV_CODED_TEXTMap(json.relationship)
      }
      
      return p
   }
   
   
   private ObjectRef parseOBJECT_REF(Map json)
   {
      ObjectRef o = new ObjectRef()
      
      o.namespace = json.namespace
      o.type = json.type
      
      String type = json.id._type
      String method = 'parse'+ type +'Map'
      o.id = this."$method"(json.id)
      
      return o
   }
   
   private PartyRef parsePARTY_REFMap(Map json)
   {
      PartyRef p = new PartyRef()
      
      p.namespace = json.namespace
      p.type = json.type
      
      String type = json.id._type
      String method = 'parse'+ type +'Map'
      p.id = this."$method"(json.id)
      
      return p
   }
   
   private LocatableRef parseLOCATABLE_REF(Map json)
   {
      LocatableRef o = new LocatableRef()
      
      o.namespace = json.namespace
      o.type = json.type
      
      if (json.path)
         o.path = json.path
      
      String type = json.id._type
      String method = 'parse'+ type +'Map'
      o.id = this."$method"(json.id)     
      
      return o
   }
   
   
   private DvIdentifier parseDV_IDENTIFIERMap(Map json)
   {
      DvIdentifier i = new DvIdentifier()
      
      i.issuer = json.issuer
      i.assigner = json.assigner
      i.id = json.id
      i.type = json.type
      
      return i
   }
   
   private EventContext parseEVENT_CONTEXTMap(Map json)
   {
      EventContext e = new EventContext()
      e.start_time = this.parseDV_DATE_TIMEMap(json.start_time)
      
      if (e.end_time)
         e.end_time = this.parseDV_DATE_TIMEMap(json.end_time)
      
      e.location = json.location
      
      e.setting = this.parseDV_CODED_TEXTMap(json.setting)
      
      if (json.other_context)
      {         
         String type, method
         type = json.other_context._type
         method = 'parse'+ type +'Map'
         e.other_context = this."$method"(json.other_context)
      }
      
      // TODO: health_care_facility
      
      json.participations.each { participation ->
         e.participations.add(this.parsePARTICIPATION(participation))
      }
      
      return e
   }
   
   private Participation parsePARTICIPATION(Map json)
   {
      Participation p = new Participation()
      
      p.function = this.parseDV_TEXTMap(json.function)
      
      if (json.time)
      {
         p.time = this.parseDV_INTERVAL(json.time)
      }
      
      p.mode = this.parseDV_CODED_TEXTMap(json.mode)
      
      String type = json.performer._type
      String method = 'parse'+ type + 'Map'
      p.performer = this."$method"(json.performer)
      
      return p
   }
   
   private Section parseSECTIONMap(Map json)
   {
      Section s = new Section()
      
      this.fillLOCATABLE(s, json)
      
      String type, method
      
      json.items.each { content_item ->
         type = content_item._type
         method = 'parse'+ type +'Map'
         this."$method"(content_item)
      }
      
      return s
   }
   
   private AdminEntry parseADMIN_ENTRYMap(Map json)
   {
      AdminEntry a = new AdminEntry()
      
      this.fillLOCATABLE(a, json)
      this.fillENTRY(a, json)
      
      String type = json.data._type
      String method = 'parse'+ type +'Map'
      a.data = this."$method"(json.data)
      
      return a
   }
   
   private Observation parseOBSERVATIONMap(Map json)
   {
      Observation o = new Observation()
      
      this.fillLOCATABLE(o, json)
      this.fillCARE_ENTRY(o, json)
      
      if (json.data)
      {
         o.data = this.parseHISTORY(json.data)
      }
      
      if (json.state)
      {
         o.state = this.parseHISTORY(json.state)
      }
      
      return o
   }
   
   private History parseHISTORY(Map json)
   {
      History h = new History()
      
      this.fillLOCATABLE(h, json)
      
      h.origin = this.parseDV_DATE_TIMEMap(json.origin)
      
      if (json.period)
      {         
         h.period = this.parseDV_DURATIONMap(json.period)
      }
      
      if (json.duration)
      {         
         h.duration = this.parseDV_DURATIONMap(json.duration)
      }

      String type, method
      json.events.each { event ->
         type = event._type
         method = 'parse'+ type
         h.events.add("$method"(event))
      }     
      
      return h
   }
   
   private PointEvent parsePOINT_EVENT(Map json)
   {
      PointEvent e = new PointEvent()
      
      this.fillLOCATABLE(e, json)
      
      e.time = this.parseDV_DATE_TIMEMap(json.time)
      
      String type, method
      
      if (json.state)
      {         
         type = json.state._type
         method = 'parse'+ type +'Map'
         e.state = this."$method"(json.state)
      }
      
      if (json.data)
      {
         type = json.data._type
         method = 'parse'+ type +'Map'
         e.data = this."$method"(json.data)
      }
      
      return e
   }
   
   private IntervalEvent parseINTERVAL_EVENT(Map json)
   {
      IntervalEvent e = new IntervalEvent()
      
      this.fillLOCATABLE(e, json)
      
      e.time = this.parseDV_DATE_TIMEMap(json.time)
      
      String type, method
      
      if (json.state)
      {
         type = json.state._type
         method = 'parse'+ type +'Map'
         e.state = this."$method"(json.state)
      }
      
      if (json.data)
      {
         type = json.data._type
         method = 'parse'+ type +'Map'
         e.data = this."$method"(json.data)
      }
      
      e.width = this.parseDV_DURATIONMap(json.width)
      
      e.math_function = this.parseDV_CODED_TEXTMap(json.math_function)
      
      if (json.sample_count != null)
      {         
         e.sample_count = json.sample_count
      }
      
      return e
   }
   
   private Evaluation parseEVALUATIONMap(Map json)
   {
      Evaluation e = new Evaluation()
      
      this.fillLOCATABLE(e, json)
      this.fillCARE_ENTRY(e, json)
      
      String type = json.data._type
      String method = 'parse'+ type +'Map'
      e.data = this."$method"(json.data)
      
      return e
   }
   
   private Instruction parseINSTRUCTIONMap(Map json)
   {
      Instruction i = new Instruction()
      
      this.fillLOCATABLE(i, json)
      this.fillCARE_ENTRY(i, json)
      
      String type, method
      
      
      type = json.narrative._type
      if (!type) type = 'DV_TEXT'
      method = 'parse'+ type +'Map'
      i.narrative = this."$method"(json.narrative)
      
      
      if (json.expiry_time)
         i.expiry_time = this.parseDV_DATE_TIMEMap(json.expiry_time)
      
         
      if (json.wf_definition)
         i.wf_definition = this.parseDV_PARSABLEMap(json.wf_definition)
      
      
      json.activities.each { js_activity ->
         
         i.activities.add(this.parseACTIVITYMap(js_activity))
      }
      
      return i
   }
   
   private Action parseACTIONMap(Map json)
   {
      Action a = new Action()
      
      this.fillLOCATABLE(a, json)
      this.fillCARE_ENTRY(a, json)
      
      String type = json.description._type
      String method = 'parse'+ type +'Map'
      a.description = this."$method"(json.description)
      
      a.time = this.parseDV_DATE_TIMEMap(json.time)

      a.ism_transition = this.parseISM_TRANSITION(json.ism_transition)
      
      if (json.instruction_details)
         a.instruction_details = this.parseINSTRUCTION_DETAILS(json.instruction_details)
      
      return a
   }

   private IsmTransition parseISM_TRANSITION(Map json)
   {
      IsmTransition i = new IsmTransition()

      i.current_state = this.parseDV_CODED_TEXTMap(json.current_state)

      if (json.transition)
      {
         i.transition = this.parseDV_CODED_TEXTMap(json.transition)
      }

      if (json.careflow_step)
      {
         i.careflow_step = this.parseDV_CODED_TEXTMap(json.careflow_step)
      }

      return i
   }
   
   private InstructionDetails parseINSTRUCTION_DETAILS(Map json)
   {
      InstructionDetails i = new InstructionDetails()
      
      i.instruction_id = this.parseLOCATABLE_REF(json.instruction_id)
      
      i.activity_id = json.activity_id
      
      if (json.wf_details)
      {
         String type = json.wf_details._type
         String method = 'parse'+ type +'Map'
         i.wf_details = this."$method"(json.wf_details)
      }
      
      return i
   }
   
   private Activity parseACTIVITYMap(Map json)
   {
      String type = json.description._type
      String method = 'parse'+ type +'Map'
      
      Activity a = new Activity(
         description: this."$method"(json.description),
         action_archetype_id: json.action_archetype_id
      )
      
      if (json.timing)
      {
         a.timing = this.parseDV_PARSABLEMap(json.timing)
      }
      
      this.fillLOCATABLE(a, json)
      
      return a
   }
   
   
   private TerminologyId parseTERMINOLOGY_IDMap(Map json)
   {
      new TerminologyId(
         value: json.value
      )
   }
   
   private GenericId parseGENERIC_IDMap(Map json)
   {
      new GenericId(
         scheme: json.scheme,
         value: json.value
      )
   }
      
   private CodePhrase parseCODE_PHRASEMap(Map json)
   {
      new CodePhrase(
         code_string: json.code_string,
         terminology_id: this.parseTERMINOLOGY_IDMap(json.terminology_id)
      )
   }
   
   private HierObjectId parseHIER_OBJECT_IDMap(Map json)
   {
      new HierObjectId(
         value: json.value
      )
   }
   
   private ObjectVersionId parseOBJECT_VERSION_IDMap(Map json)
   {
      new ObjectVersionId(
         value: json.value
      )
   }
   
   private VersionTreeId parseVERSION_TREE_IDMap(Map json)
   {
      new VersionTreeId(
         value: json.value
      )
   }
   
   
   private DvText parseDV_TEXTMap(Map json)
   {
      new DvText(value: json.value)
   }
   
   private DvCodedText parseDV_CODED_TEXTMap(Map json)
   {
      new DvCodedText(
         value: json.value,
         defining_code: this.parseCODE_PHRASEMap(json.defining_code)
      )
   }
   
   private TermMapping parseTERM_MAPPINGMap(Map json)
   {
      new TermMapping(
         match: json.match,
         purpose: this.parseDV_CODED_TEXTMap(json.purpose),
         target: this.parseCODE_PHRASEMap(json.target)
      )
   }
   
   private DvDateTime parseDV_DATE_TIMEMap(Map json)
   {
      // TODO: DvAbsoluteQuantity
      new DvDateTime(value: json.value)
   }
   
   private DvDate parseDV_DATEMap(Map json)
   {
      // TODO: DvAbsoluteQuantity
      new DvDate(value: json.value)
   }
   
   private DvTime parseDV_TIMEMap(Map json)
   {
      // TODO: DvAbsoluteQuantity
      new DvTime(value: json.value)
   }
   
   private DvDuration parseDV_DURATIONMap(Map json)
   {
      DvDuration d = new DvDuration()
      
      d.value = json.value
      
      this.fillDV_AMOUNT(d, json)
      
      return d
   }
   
   private DvQuantity parseDV_QUANTITYMap(Map json)
   {
      DvQuantity q = new DvQuantity()
      
      q.magnitude = json.magnitude
      
      q.units = json.units
      
      q.precision = json.precision
      
      this.fillDV_AMOUNT(q, json)
     
      return q
   }
   
   private DvCount parseDV_COUNTMap(Map json)
   {
      DvCount c = new DvCount()
      
      c.magnitude = json.magnitude
      
      this.fillDV_AMOUNT(c, json)
      
      return c
   }
   
   private DvProportion parseDV_PROPORTIONMap(Map json)
   {
      DvProportion d = new DvProportion(
         numerator: json.numerator,
         denominator: json.denominator,
         type: json.type,
         precision: json.precision
      )
      
      this.fillDV_AMOUNT(d, json)
      
      return d
   }
   
   private DvOrdinal parseDV_ORDINALMap(Map json)
   {
      DvOrdinal d = new DvOrdinal(
         value: json.value,
         symbol: this.parseDV_CODED_TEXTMap(json.symbol)
      )
      
      this.fillDV_ORDERED(d, json)
      
      return d
   }
   
   private DvParsable parseDV_PARSABLEMap(Map json)
   {
      DvParsable p = new DvParsable(
         value: json.value,
         formalism: json.formalism,
         size: json.value.size()
      )
      
      if (json.charset)
      {
         p.charset = this.parseCODE_PHRASEMap(json.charset)
      }
      
      if (json.language)
      {
         p.language = this.parseCODE_PHRASEMap(json.language)
      }
      
      return p
   }
   
   private DvMultimedia parseDV_MULTIMEDIAMap(Map json)
   {
      DvMultimedia d = new DvMultimedia()
      
      if (json.charset)
      {
         p.charset = this.parseCODE_PHRASEMap(json.charset)
      }
      
      if (json.language)
      {
         p.language = this.parseCODE_PHRASEMap(json.language)
      }
      
      d.alternate_text = json.alternate_text
      
      if (json.uri)
      {
         d.uri = this.parseDV_URIMap(json.uri)
      }
      
      d.data = json.data.getBytes()
      
      d.media_type = this.parseCODE_PHRASEMap(json.media_type)
      
      if (json.compression_algorithm)
      {
         d.compression_algorithm = this.parseCODE_PHRASEMap(json.compression_algorithm)
      }
      
      d.size = json.size
      
      // TODO: integrity_check, integrity_check_algorithm, thumbnail
      
      return d
   }
   
   private DvUri parseDV_URIMap(Map json)
   {
      new DvUri(
         value: json.value
      )
   }
   
   private DvEhrUri parseDV_EHR_URIMap(Map json)
   {
      new DvEhrUri(
         value: json.value
      )
   }
   
   private DvBoolean parseDV_BOOLEANMap(Map json)
   {
      new DvBoolean(
         value: json.value
      )
   }
   
   
   private ItemTree parseITEM_TREEMap(Map json)
   {
      ItemTree t = new ItemTree()
      
      this.fillLOCATABLE(t, json)
      
      String type, method
      
      json.items.each { item ->
         type = item._type
         method = 'parse'+ type +'Map'
         //println " - " + method
         t.items.add(this."$method"(item))
      }
      
      return t
   }
   
   private ItemList parseITEM_LISTMap(Map json)
   {
      ItemList l = new ItemList()
      
      this.fillLOCATABLE(l, json)
      
      json.items.each { element ->
         l.items.add(this.parseELEMENTMap(element))
      }
      
      return l
   }
   
   private ItemTable parseITEM_TABLEMap(Map json)
   {
      ItemTable t = new ItemTable()
      
      this.fillLOCATABLE(t, json)
      
      String type, method
      
	  // FIXME: rows are CLUSTERS, we don't need to get the dynamic method
      json.rows.each { item -> 
         type = item._type
         method = 'parse'+ type +'Map'
         t.items.add(this."$method"(item))
      }
      
      return t
   }
   
   private ItemSingle parseITEM_SINGLEMap(Map json)
   {
      ItemSingle s = new ItemSingle()
      
      this.fillLOCATABLE(s, json)
      
      s.item = this.parseELEMENTMap(json.item)
      
      return s
   }
   
   private Cluster parseCLUSTERMap(Map json)
   {
      Cluster c = new Cluster()
      
      this.fillLOCATABLE(c, json)
      
      String type, method
      
      json.items.each { item ->
         type = item._type
         method = 'parse'+ type +'Map'
         c.items.add(this."$method"(item))
      }
      
      return c
   }
   
   private Element parseELEMENTMap(Map json)
   {
      Element e = new Element()
      
      this.fillLOCATABLE(e, json)
      
      if (json.value)
      {
         String type = json.value._type
         String method = 'parse'+ type +'Map'
         e.value = this."$method"(json.value)
      }
      
      if (json.null_flavour)
      {
         e.null_flavour = this.parseDV_CODED_TEXTMap(json.null_flavour)
      }
      
      return e
   }
   
   private DvInterval parseDV_INTERVAL(Map json)
   {
      DvInterval i = new DvInterval()
      
      String type, method
      
      type = json.lower ? json.lower._type : json.upper._type
      method = 'parse'+ type +'Map'
      
      if (json.lower)
      {         
         i.lower = this."$method"(json.lower)
      }
      
      if (json.upper)
      {
         i.upper = this."$method"(json.upper)
      }
      
      i.lower_included = json.lower_included
      i.lower_unbounded = json.lower_unbounded
      i.upper_included = json.upper_included
      i.upper_unbounded = json.upper_unbounded
      
      return i
   }
   
}
