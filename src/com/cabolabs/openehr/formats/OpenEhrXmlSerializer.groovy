package com.cabolabs.openehr.formats

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Archetyped
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.rm_1_0_2.common.generic.Participation
import com.cabolabs.openehr.rm_1_0_2.common.generic.PartyIdentified
import com.cabolabs.openehr.rm_1_0_2.common.generic.PartyProxy
import com.cabolabs.openehr.rm_1_0_2.common.generic.PartyRelated
import com.cabolabs.openehr.rm_1_0_2.common.generic.PartySelf
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import com.cabolabs.openehr.rm_1_0_2.composition.EventContext
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.*
import com.cabolabs.openehr.rm_1_0_2.composition.content.navigation.Section
import com.cabolabs.openehr.rm_1_0_2.data_structures.history.Event
import com.cabolabs.openehr.rm_1_0_2.data_structures.history.History
import com.cabolabs.openehr.rm_1_0_2.data_structures.history.IntervalEvent
import com.cabolabs.openehr.rm_1_0_2.data_structures.history.PointEvent
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
import com.cabolabs.openehr.rm_1_0_2.support.identification.GenericId
import com.cabolabs.openehr.rm_1_0_2.support.identification.HierObjectId
import com.cabolabs.openehr.rm_1_0_2.support.identification.LocatableRef
import com.cabolabs.openehr.rm_1_0_2.support.identification.ObjectId
import com.cabolabs.openehr.rm_1_0_2.support.identification.ObjectRef
import com.cabolabs.openehr.rm_1_0_2.support.identification.ObjectVersionId
import com.cabolabs.openehr.rm_1_0_2.support.identification.PartyRef
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

   private void fillPartyProxy(PartyProxy o)
   {
      if (o.external_ref)
      {
         def method = this.method(o.id)
         builder.external_ref('xsi:type': openEhrType(o.external_ref)) {
            this."$method"(o.external_ref)
         }
      }
   }
   
   private void serializePartySelf(PartySelf o)
   {
      this.fillPartyProxy(o)
   }
   
   private void serializePartyIdentified(PartyIdentified o)
   {
      this.fillPartyProxy(o)
      
      if (o.name)
      {
         builder.name(o.name)
      }
      
      o.identifiers.each { identifier ->
         builder.identifiers {
            this.serializeDvIdentifier(identifier)
         }
      }
   }
   
   private void serializePartyRelated(PartyRelated o)
   {
      this.fillPartyProxy(o)
      
      if (o.name)
      {
         builder.name(o.name)
      }
      
      o.identifiers.each { identifier ->
         builder.identifiers {
            this.serializeDvIdentifier(identifier)
         }
      }
      
      builder.relationship {
         this.serializeDvCodedText(o.relationship)
      }
   }

   private void serializeObjectRef(ObjectRef o)
   {
      builder.namespace(o.namespace)

      builder.type(o.type)

      def method = this.method(o.id)
      builder.id('xsi:type': openEhrType(o.id)) {
         this."$method"(o.id)
      }
   }

   private void serializePartyRef(PartyRef o)
   {
      builder.namespace(o.namespace)

      builder.type(o.type)

      def method = this.method(o.id)
      builder.id('xsi:type': openEhrType(o.id)) {
         this."$method"(o.id)
      }
   }

   private void serializeLocatableRef(LocatableRef o)
   {
      builder.namespace(o.namespace)

      builder.type(o.type)

      if (o.path)
      {
         builder.path(o.path)
      }

      def method = this.method(o.id)
      builder.id('xsi:type': openEhrType(o.id)) {
         this."$method"(o.id)
      }
   }
   
   private void fillObjectId(ObjectId o)
   {
      builder.value(o.value)
   }

   private void serializeTerminologyId(TerminologyId o)
   {
      this.fillObjectId(o)
   }

   private void serializeGenericId(GenericId o)
   {
      this.fillObjectId(o)
   }

   private void serializeArchetypeId(ArchetypeId o)
   {
      this.fillObjectId(o)
   }

   private void serializeObjectVersionId(ObjectVersionId o)
   {
      this.fillObjectId(o)
   }

   private void serializeHierObjectId(HierObjectId o)
   {
      this.fillObjectId(o)
   }

   private void serializeTemplateId(TemplateId o)
   {
      this.fillObjectId(o)
   }

   private void fillEntry(Entry o)
   {
      builder.language() {
         this.serializeCodePhrase(o.language)
      }
      builder.encoding() {
         this.serializeCodePhrase(o.encoding)
      }
      
      String method = this.method(o.subject)
      builder.subject('xsi:type': this.openEhrType(o.subject)) {
         this."$method"(o.subject)
      }
      
      if (o.provider)
      {
         method = this.method(o.provider)
         builder.provider('xsi:type': this.openEhrType(o.provider)) {
            this."$method"(o.provider)
         }
      }
      
      if (o.other_participations)
      {
         o.other_participations.each { participation ->
            
            builder.other_participations {
               this.serializeParticipation(participation)
            }
         }
      }

      if (o.workflow_id)
      {
         method = this.method(o.workflow_id)
         builder.workflow_id('xsi:type': this.openEhrType(o.workflow_id)) {
            this."$method"(o.workflow_id)
         }
      }
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
      
      if (o.guideline_id)
      {
         method = this.method(o.guideline_id)
         builder.guideline_id('xsi:type': this.openEhrType(o.guideline_id)) {
            this."$method"(o.guideline_id)
         }
      }
   }
   
   private void fillEvent(Event o)
   {
      this.fillLocatable(o)
      
      builder.time {
         this.serializeDvDateTime(o.time)
      }
      
      String method = this.method(o.data)
      builder.data('xsi:type': this.openEhrType(o.data)) {
         this."$method"(o.data)
      }
      
      if (o.state)
      {
         method = this.method(o.state)
         builder.state('xsi:type': this.openEhrType(o.state)) {
            this."$method"(o.state)
         }
      }
   }
   
   private void serializeArchetyped(Archetyped o)
   {
      builder.archetype_details {
         archetype_id {
            serializeArchetypeId(o.archetype_id)
         }
         template_id {
           serializeTemplateId(o.template_id)
         }
         rm_version(o.rm_version)
      }
   }
   
   /*
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
   */
   
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
      
      if (e.health_care_facility)
      {
         builder.health_care_facility {
            this.serializePartyIdentified(e.health_care_facility)
         }
      }
      
      if (e.participations)
      {
         e.participations.each { participation ->
            
            builder.participations {
               this.serializeParticipation(participation)
            }
         }
      }
   }
   
   void serializeParticipation(Participation o)
   {
      String method = this.method(o.function) // text or coded text
      builder.function('xsi:type': this.openEhrType(o.function)) {
         this."$method"(o.function)
      }
      
      if (o.time)
      {
         builder.time {
            this.serializeDvInterval(o.time)
         }
      }
      
      builder.mode {
         this.serializeDvCodedText(o.mode)
      }
      
      method = this.method(o.performer)
      builder.performer('xsi:type': this.openEhrType(o.performer)) {
         this."$method"(o.performer)
      }
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

   void serializeObservation(Observation o)
   {
      this.fillLocatable(o)
      this.fillCareEntry(o)
      
      builder.data {
         this.serializeHistory(o.data)
      }
      
      if (o.state)
      {
         builder.state {
            this.serializeHistory(o.state)
         }
      }
   }
  
   void serializeHistory(History o)
   {
      this.fillLocatable(o)
      
      builder.origin {
         this.serializeDvDateTime(o.origin)
      }
      
      if (o.period)
      {
         this.serializeDvDuration(o.period)
      }
      
      if (o.duration)
      {
         this.serializeDvDuration(o.duration)
      }
      
      // TODO: summary (is not in the RM impl yet)
      
      String method
      o.events.each { event ->
         method = this.method(event)
         builder.events('xsi:type': this.openEhrType(event)) {
            this."$method"(event)
         }
      }
   }
   
   void serializePointEvent(PointEvent o)
   {
      this.fillEvent(o)
   }
   
   void serializeIntervalEvent(IntervalEvent o)
   {
      this.fillEvent(o)
      
      builder.width {
         this.serializeDvDuration(o.width)
      }
      
      builder.math_function {
         this.serializeDvCodedText(o.math_function)
      }
      
      if (o.sample_count)
      {
         builder.sample_count(o.sample_count)
      }
   }
   
   void serializeEvaluation(Evaluation o)
   {
      this.fillLocatable(o)
      this.fillCareEntry(o)
      
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
         builder.wf_definition {
            this.serializeDvParsable(o.wf_definition)
         }
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
      
      builder.timing {
         this.serializeDvParsable(o.timing)
      }
      
      builder.action_archetype_id(o.action_archetype_id)
   }
   
   void serializeAction(Action o)
   {
      this.fillLocatable(o)
      this.fillCareEntry(o)
      
      builder.time(this.serializeDvDateTime(o.time))
      
      String method = this.method(o.description)
      builder.description('xsi:type': this.openEhrType(o.description)) {
         this."$method"(o.description)
      }
      
      builder.ism_transition {
         this.serializeIsmTransition(o.ism_transition)
      }
      
      if (o.instruction_details)
      {
         builder.instruction_details {
            this.serializeInstructionDetails(o.instruction_details)
         }
      }
   }
   
   void serializeIsmTransition(IsmTransition o)
   {
      builder.current_state {
         this.serializeDvCodedText(o.current_state)
      }
      
      if (o.transition)
      {
         builder.transition {
            this.serializeDvCodedText(o.transition)
         }
      }
      
      if (o.careflow_step)
      {
         builder.careflow_step {
            this.serializeDvCodedText(o.careflow_step)
         }
      }
   }
   
   void serializeInstructionDetails(InstructionDetails o)
   {
      builder.instruction_id {
         this.serializeLocatableRef(o.instruction_id)
      }
      
      builder.activity_id(o.activity_id)
      
      if (o.wf_details)
      {
         String method = this.method(o.wf_details)
         builder.wf_details('xsi:type': this.openEhrType(o.wf_details)) {
            this."$method"(o.wf_details)
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

   
   void serializeDvDateTime(DvDateTime o)
   {
      // TODO: accuracy, ...
      builder.value(o.value)
   }
   
   void serializeDvDate(DvDate o)
   {
      // TODO
      builder.value(o.value)
   }
   
   void serializeDvTime(DvTime o)
   {
      // TODO
      builder.value(o.value)
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
      builder.issuer(o.issuer)
      builder.assigner(o.assigner)
      builder.id(o.id)
      builder.type(o.type)
   }
   
   void serializeDvQuantity(DvQuantity o)
   {
      // TODO: inherited attributes
      
      builder.magnitude(o.magnitude)
      builder.units(o.units)
      
      if (o.precision)
      {
         builder.precision(o.precision)
      }
   }
   
   void serlializeDvCount(DvCount o)
   {
      // TODO: inherited attributes
      
      builder.magnitude(o.magnitude)
   }
   
   void serializeDvProportion(DvProportion o)
   {
      // TODO: inherited attributes
      
      builder.numerator(o.numerator)
      builder.denominator(o.denominator)
      builder.type(o.type)
      
      if (o.precision != null && o.precision >= 0)
      {
         builder.precision(o.precision)
      }
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
      builder.value(o.value)
   }
   
   void serializeDvEhrUri(DvEhrUri o)
   {
      builder.value(o.value)
   }
   
   void serializeDvInterval(DvInterval o)
   {
      // TODO
   }
   
   /*
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
   */
}
