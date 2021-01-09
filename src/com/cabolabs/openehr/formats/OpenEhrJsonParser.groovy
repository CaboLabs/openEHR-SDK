package com.cabolabs.openehr.formats

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
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
   
   private Composition parseCOMPOSITIONMap(Map json)
   {
      println "its a compo"
      
      def context = parseEVENT_CONTEXTMap(json.context)
      def content = []
      
      String type, method
      json.content.each { content_item ->
         type = content_item._type
         method = 'parse'+ type +'Map'
         this."$method"(content_item)
      }
      
      return null
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
      
   private CodePhrase parseCODE_PHRASEMap(Map json)
   {
      
   }
   
   private DvText parseDV_TEXTMap(Map json)
   {
      
   }
   
   private DvCodedText parseDV_CODED_TEXTMap(Map json)
   {
      
   }
   
   private DvDuration parseDV_DURATIONMap(Map json)
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
