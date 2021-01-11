package com.cabolabs.openehr.formats

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Archetyped
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.rm_1_0_2.common.generic.PartyIdentified
import com.cabolabs.openehr.rm_1_0_2.common.generic.PartyRelated
import com.cabolabs.openehr.rm_1_0_2.common.generic.PartySelf
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import com.cabolabs.openehr.rm_1_0_2.composition.EventContext
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.Action
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.Activity
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.CareEntry
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.Entry
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.Evaluation
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.Instruction
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.Observation
import com.cabolabs.openehr.rm_1_0_2.composition.content.navigation.Section
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemTree
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.Cluster
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.Element
import com.cabolabs.openehr.rm_1_0_2.data_types.basic.DvBoolean
import com.cabolabs.openehr.rm_1_0_2.data_types.encapsulated.DvParsable
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDuration
import com.cabolabs.openehr.rm_1_0_2.data_types.text.CodePhrase
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvCodedText
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvText
import com.cabolabs.openehr.rm_1_0_2.support.identification.ArchetypeId
import com.cabolabs.openehr.rm_1_0_2.support.identification.GenericId
import com.cabolabs.openehr.rm_1_0_2.support.identification.TemplateId
import com.cabolabs.openehr.rm_1_0_2.support.identification.TerminologyId
import com.cabolabs.openehr.rm_1_0_2.support.identification.UIDBasedId
import groovy.json.JsonSlurper

class OpenEhrJsonParser {
   
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
   
   private void fillLOCATABLE(Locatable l, Map json)
   {
      // name can be text or coded
      String type = json.name._type
      String method = 'parse'+ type +'Map'
      
      l.name = this."$method"(json.name)
      
      l.archetype_node_id = json.archetype_node_id
      
      if (json.uid)
         l.uid = this.parseUID_BASED_IDMap(json.uid)
      
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
         // TODO
      }
      
      
      if (json.workflow_id)
      {
         // TODO
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
         c.guideline_id = this.parseOBJECT_REFMap(json.guideline_id)
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
      a.version_id = json.version_id
      return a
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
   
   private PartySelf parsePARTY_SELFMap(Map json)
   {
      
   }
   
   private PartyIdentified parsePARTY_IDENTIFIEDMap(Map json)
   {
      
   }
   
   private PartyRelated parsePARTY_RELATEDMap(Map json)
   {
      
   }
   
   private EventContext parseEVENT_CONTEXTMap(Map json)
   {
      EventContext e = new EventContext()
      e.start_time = this.parseDV_DATE_TIMEMap(json.start_time)
      
      if (e.end_time)
         e.end_time = this.parseDV_DATE_TIMEMap(json.end_time)
      
      e.location = json.location
      
      e.setting = this.parseDV_CODED_TEXTMap(json.setting)
      
      if (json.other_details)
      {         
         String type, method
         type = json.other_details._type
         method = 'parse'+ type +'Map'
         e.other_details = this."$method"(json.other_details)
      }
      
      // TODO: health_care_facility
      // TODO: participations
      
      return e
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
   
   private Observation parseOBSERVATIONMap(Map json)
   {
      println "its a observation"
   }
   
   private Evaluation parseEVALUATIONMap(Map json)
   {
      println "its an evaluation"
   }
   
   private Instruction parseINSTRUCTIONMap(Map json)
   {
      Instruction i = new Instruction()
      this.fillLOCATABLE(i, json)
      this.fillENTRY(i, json)
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
      
         
      if (json.protocol) // can be null
      {
         type = json.protocol._type
         method = 'parse'+ type +'Map'
         i.protocol = this."$method"(json.protocol)
      }
      
      
      if (json.guideline_id)
      {
         // TODO
      }
      
      
      json.activities.each { js_activity ->
         
         i.activities.add(this.parseACTIVITYMap(js_activity))
      }
      
      return i
   }
   
   private Action parseACTIONMap(Map json)
   {
      println "its an action"
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
   
   private DvDateTime parseDV_DATE_TIMEMap(Map json)
   {
      new DvDateTime(value: json.value)
   }
   
   private DvDuration parseDV_DURATIONMap(Map json)
   {
      
   }
   
   private DvParsable parseDV_PARSABLEMap(Map json)
   {
      DvParsable p = new DvParsable(
         value: json.value,
         formalism: json.formalism,
         size: json.values().size()
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
   
   private UIDBasedId parseUID_BASED_IDMap(Map json)
   {
      
   }
   
   private ItemTree parseITEM_TREEMap(Map json)
   {
      ItemTree t = new ItemTree()
      
      String type, method
      
      json.items.each { item ->
         type = item._type
         method = 'parse'+ type +'Map'
         t.items.add(this."$method"(item))
      }
      
      return t
   }
   
   private Cluster parseCLUSTERMap(Map json)
   {
      Cluster c = new Cluster()
      
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
   
   private DvBoolean parseDV_BOOLEANMap(Map json)
   {
      
   }
   
   
   
   private Locatable parseLocatableMap(Map json)
   {
      switch (json._type)
      {
         case 'COMPOSITION':
            println "its a compo"
         break
         case 'EVENT_CONTEXT':
            println "its a event context"
         break
         case 'OBSERVATION':
            println "its a obs"
         break
      }
   }
}
