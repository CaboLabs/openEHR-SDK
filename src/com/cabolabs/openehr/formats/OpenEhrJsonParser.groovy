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
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.Evaluation
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.Instruction
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.Observation
import com.cabolabs.openehr.rm_1_0_2.composition.content.navigation.Section
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemTree
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.Cluster
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.Element
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDuration
import com.cabolabs.openehr.rm_1_0_2.data_types.text.CodePhrase
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvCodedText
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvText
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
   
   private Archetyped parseARCHETYPEDMap(Map json)
   {
      
   }
   
   private Composition parseCOMPOSITIONMap(Map json)
   {
      Composition compo = new Composition()
      fillLOCATABLE(compo, json)
      
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
         this."$method"(content_item)
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
      println "its a event context"
   }
   
   private Section parseSECTIONMap(Map json)
   {
      println "its a section"
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
      println "its an instruction"
      
      if (json.protocol) // can be null
      {
         String type = json.protocol._type
         String method = 'parse'+ type +'Map'
         def protocol = this."$method"(json.protocol)
      }
      
      json.activities.each { js_activity ->
         
         def activity = parseACTIVITYMap(js_activity)
      }
      
      return null
   }
   
   private Action parseACTIONMap(Map json)
   {
      println "its an action"
   }
   
   private Activity parseACTIVITYMap(Map json)
   {
      
   }
   
   private TerminologyId parseTERMINOLOGY_IDMap(Map json)
   {
      new TerminologyId(
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
   
   private DvDuration parseDV_DURATIONMap(Map json)
   {
      
   }
   
   private UIDBasedId parseUID_BASED_IDMap(Map json)
   {
      
   }
   
   private ItemTree parseITEM_TREEMap(Map json)
   {
      println "its a tree"
   }
   
   private Cluster parseCLUSTERMap(Map json)
   {
      
   }
   
   private Element parseELEMENTMap(Map json)
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
