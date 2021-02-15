package com.cabolabs.openehr.formats

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Archetyped
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import com.cabolabs.openehr.rm_1_0_2.composition.EventContext
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.Activity
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.AdminEntry
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.CareEntry
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.Entry
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.Instruction
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.Observation
import com.cabolabs.openehr.rm_1_0_2.composition.content.navigation.Section
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemList
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemSingle
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemTable
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemTree
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.Cluster
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.Element
import com.cabolabs.openehr.rm_1_0_2.data_types.basic.DvBoolean
import com.cabolabs.openehr.rm_1_0_2.data_types.basic.DvIdentifier
import com.cabolabs.openehr.rm_1_0_2.data_types.encapsulated.DvMultimedia
import com.cabolabs.openehr.rm_1_0_2.data_types.encapsulated.DvParsable
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.DvCount
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.DvInterval
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.DvOrdinal
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.DvProportion
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.DvQuantity
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDate
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDuration
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvTime
import com.cabolabs.openehr.rm_1_0_2.data_types.text.CodePhrase
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvCodedText
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvText
import com.cabolabs.openehr.rm_1_0_2.data_types.uri.DvEhrUri
import com.cabolabs.openehr.rm_1_0_2.data_types.uri.DvUri
import com.cabolabs.openehr.rm_1_0_2.support.identification.ArchetypeId
import com.cabolabs.openehr.rm_1_0_2.support.identification.HierObjectId
import com.cabolabs.openehr.rm_1_0_2.support.identification.ObjectVersionId
import com.cabolabs.openehr.rm_1_0_2.support.identification.TemplateId
import com.cabolabs.openehr.rm_1_0_2.support.identification.TerminologyId
import groovy.xml.MarkupBuilder
import java.text.SimpleDateFormat

class OpenEhrXmlSerializer {
   
   def writer
   def builder
   
   private String method(Object obj)
   {
      String type = obj.getClass().getSimpleName()
      String method = 'serialize'+ type
      return method
   }
   
   private void fillLocatable(Locatable o)
   {
      println 'fillLocatable >> ' + o
      
      String method = this.method(o.name) // text or coded
      builder.name() {
         this."$method"(o.name)
      }
      
      if (o.uid)
      {
         method = this.method(o.uid)
         builder.uid('xsi:type': openEhrType(o.uid)) {
            this."$method"(o.uid)
         }
      }
      
      // TODO: links
      
      if (o.archetype_details)
      {
         serializeArchetyped(o.archetype_details)
      }
      
      // this should be an attribute
      //builder.archetype_node_id(o.archetype_node_id)
      
      // TODO: feeder audit
   }
   
   private void fillEntry(Entry o)
   {
      builder.language() {
         this.serializeCodePhrase(o.language)
      }
      builder.encoding() {
         this.serializeCodePhrase(o.encoding)
      }
      // TODO: subject
      // TODO: provider
      // TODO: other_participations
      // TODO: workflow_id
   }
   
   private void fillCareEntry(CareEntry o)
   {
      this.fillEntry(o)
      
      if (o.protocol)
      {
         String method = this.method(o.protocol)
         builder.protocol('xsi:type': this.openEhrType(o.protocol)) {
            this."$method"(o.protocol)
         }
      }
      
      // TODO: guideline_id
   }
   
   private void serializeArchetyped(Archetyped o)
   {
      builder.archetype_details {
         serializeArchetypeId(o.archetype_id)
         serializeTemplateId(o.template_id)
         rm_version(o.rm_version)
      }
   }
   
   private void serializeArchetypeId(ArchetypeId o)
   {
      builder.archetype_id() {
         value(o.value)
      }
   }
   
   private void serializeTemplateId(TemplateId o)
   {
      builder.template_id() {
         value(o.value)
      }
   }
   
   // transforms a Java type into the correspondent openEHR type name
   // EventContext => EVENT_CONTEXT
   private String openEhrType(Object o)
   {
      o.getClass().getSimpleName().replaceAll("[A-Z]", '_$0').toUpperCase().replaceAll( /^_/, '')
   }
   
   String serialize(Locatable o)
   {
      writer = new StringWriter()
      builder = new MarkupBuilder(writer)
      builder.setDoubleQuotes(true)
      
      String method = this.method(o)
      this."$method"(o)
      
      return writer.toString()
   }
   
   void serializeComposition(Composition c)
   {
      String method
      
      builder.composition(xmlns:'http://schemas.openehr.org/v1',
         'xmlns:xsi':'http://www.w3.org/2001/XMLSchema-instance',
         archetype_node_id: c.archetype_node_id)
      {
         //generateCompositionHeader(addParticipations) // name, language, territory, ...
         //generateCompositionContent(opt.definition.archetypeId)
         fillLocatable(c)
         language {
            serializeCodePhrase(c.language)
         }
         territory {
            serializeCodePhrase(c.territory)
         }
         category {
            serializeDvCodedText(c.category)
         }
         composer {
            
         }
         
         if (c.context)
         {
           context() {
              serializeEventContext(c.context)
           }
         }
          
         c.content.each { content_item ->
          
            method = this.method(content_item)
            content('xsi:type': this.openEhrType(content_item)) {
               this."$method"(content_item)
            }
         }
      }
   }
   
   void serializeEventContext(EventContext e)
   {
      builder.start_time() {
         serializeDvDateTime(e.start_time)
      }
      if (e.end_time)
      {
         builder.end_time() {
            serializeDvDateTime(e.end_time)
         }
      }
      
      if (e.location)
      {
         builder.location(e.location)
      }
      
      builder.setting() {
         serializeDvCodedText(e.setting)
      }
      
      if (e.other_context)
      {
         String method = this.method(e.other_context)
         builder.other_context('xsi:type': this.openEhrType(e.other_context)) {
            this."$method"(e.other_context)
         }
      }
      
      // TODO: health_care_facility, participations
   }
   
   void serializeItemTree(ItemTree o)
   {
      this.fillLocatable(o)
      
      String method
      o.items.each { item ->
         method = this.method(item)
         builder.items('xsi:type': this.openEhrType(item)) {
            this."$method"(item)
         }
      }
   }
   
   void serializeItemList(ItemList o)
   {
      this.fillLocatable(o)

      o.items.each { item ->
         builder.items('xsi:type': 'ELEMENT') {
            this.serializeElement(item)
         }
      }
   }
   
   void serializeItemSingle(ItemSingle o)
   {
      this.fillLocatable(o)
      
      o.item {
         this.serializeElement(o.item)
      }
   }
   
   void serializeItemTable(ItemTable o)
   {
      this.fillLocatable(o)
      
      // TODO
   }
   
   void serializeElement(Element o)
   {
      this.fillLocatable(o)
      
      if (o.value)
      {
         String method = this.method(o.value)
         builder.value('xsi:type': this.openEhrType(o.value)) {
            this."$method"(o.value)
         }
      }
      
      if (o.null_flavour)
      {
         builder.null_flavour() {
            this.serializeDvCodedText(o.null_flavour)
         }
      }
   }
   
   void serializeCluster(Cluster o)
   {
      this.fillLocatable(o)
      
      String method
      o.items.each { item ->
         method = this.method(item)
         builder.items('xsi:type': this.openEhrType(item)) {
            this."$method"(item)
         }
      }
   }
   
   void serializeSection(Section s)
   {
      s.items.each { content_item ->
         
         method = this.method(content_item)
         items('xsi:type': this.openEhrType(content_item)) {
            this."$method"(content_item)
         }
      }
   }
   
   void serializeAdminEntry(AdminEntry o)
   {
      this.fillLocatable(o)
      this.fillEntry(o)
      
      String method = this.method(o.data)
      builder.data('xsi:type': this.openEhrType(o.data)) {
         this."$method"(o.data)
      }
   }
   
   void serializeInstruction(Instruction o)
   {
      this.fillLocatable(o)
      this.fillCareEntry(o)
      
      String method = this.method(o.narrative)
      builder.narrative('xsi:type': this.openEhrType(o.narrative)) {
         this."$method"(o.narrative)
      }
      
      if (o.expiry_time)
      {
         builder.expirity_time() {
            this.serializeDvDateTime(o.expiry_time)
         }
      }
      
      if (o.wf_definition)
      {
         // TODO
      }
      
      o.activities.each { activity ->
         builder.activities() {
            this.serializeActivity(activity)
         }
      }
   }
   
   void serializeActivity(Activity o)
   {
      this.fillLocatable(o)
      
      String method = this.method(o.description)
      builder.description('xsi:type': this.openEhrType(o.description)) {
         this."$method"(o.description)
      }
      
      // TODO: timing
      
      builder.action_archetype_id(o.action_archetype_id)
   }
   
   void serializeObservation(Observation o)
   {
      this.fillLocatable(o)
      this.fillCareEntry(o)
      
      // TODO: data, state HISTORY
   }
   
   void serializeDvDateTime(DvDateTime o)
   {
      // TODO: accuracy, ...
      builder.value(o.value)
   }
   
   void serializeDvDate(DvDate o)
   {
      // TODO
   }
   
   void serializeDvTime(DvTime o)
   {
      // TODO
   }
   
   void serializeDvText(DvText o)
   {
      builder.value(o.value)
      
      // TODO: mappings
   }
   
   void serializeDvCodedText(DvCodedText o)
   {
      builder.value(o.value)
      
      builder.defining_code()
      {
         this.serializeCodePhrase(o.defining_code)
      }
   }
   
   void serializeDvOrdinal(DvOrdinal o)
   {
      builder.value(o.value)
      
      builder.symbol() {
         this.serializeDvCodedText(o.symbol)
      }
   }
   
   void serializeDvDuration(DvDuration o)
   {
     builder.value(o.value)
     
     // TODO: accuracy, magnitude_status, ... all attributes from superclasses
   }
   
   void serializeDvBoolean(DvBoolean o)
   {
      builder.value(o.value)
   }
   
   void serializeCodePhrase(CodePhrase o)
   {
      builder.code_string(o.code_string)
      
      builder.terminology_id() {
         this.serializeTerminologyId(o.terminology_id)
      }
   }
   
   void serializeDvIdentifier(DvIdentifier o)
   {
      // TODO
   }
   
   void serializeDvQuantity(DvQuantity o)
   {
      // TODO
   }
   
   void serlializeDvCount(DvCount o)
   {
      // TODO
   }
   
   void serializeDvProportion(DvProportion o)
   {
      // TODO
   }
   
   void serializeDvParsable(DvParsable o)
   {
      // TODO
   }
   
   void serializeDvMultimedia(DvMultimedia o)
   {
      // TODO
   }
   
   void serializeDvUri(DvUri o)
   {
      // TODO
   }
   
   void serializeDvEhrUri(DvEhrUri o)
   {
      // TODO
   }
   
   void serializeDvInterval(DvInterval o)
   {
      // TODO
   }
   
   void serializeTerminologyId(TerminologyId o)
   {
      builder.value(o.value)
   }
   
   void serializeHierObjectId(HierObjectId o)
   {
      builder.value(o.value)
   }
   
   void serializeObjectVersionId(ObjectVersionId o)
   {
      builder.value(o.value)
   }
}
