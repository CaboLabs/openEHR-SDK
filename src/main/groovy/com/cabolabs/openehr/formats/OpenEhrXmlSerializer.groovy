package com.cabolabs.openehr.formats

import groovy.xml.MarkupBuilder
import java.text.SimpleDateFormat

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Archetyped
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.rm_1_0_2.common.change_control.OriginalVersion
import com.cabolabs.openehr.rm_1_0_2.common.change_control.Version
import com.cabolabs.openehr.rm_1_0_2.common.generic.*
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import com.cabolabs.openehr.rm_1_0_2.composition.EventContext
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.*
import com.cabolabs.openehr.rm_1_0_2.composition.content.navigation.Section
import com.cabolabs.openehr.rm_1_0_2.data_structures.history.Event
import com.cabolabs.openehr.rm_1_0_2.data_structures.history.History
import com.cabolabs.openehr.rm_1_0_2.data_structures.history.IntervalEvent
import com.cabolabs.openehr.rm_1_0_2.data_structures.history.PointEvent
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.*
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.Cluster
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.Element
import com.cabolabs.openehr.rm_1_0_2.data_types.basic.DvBoolean
import com.cabolabs.openehr.rm_1_0_2.data_types.basic.DvIdentifier
import com.cabolabs.openehr.rm_1_0_2.data_types.encapsulated.*
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.*
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDate
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDuration
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvTime
import com.cabolabs.openehr.rm_1_0_2.data_types.text.CodePhrase
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvCodedText
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvText
import com.cabolabs.openehr.rm_1_0_2.data_types.uri.DvEhrUri
import com.cabolabs.openehr.rm_1_0_2.data_types.uri.DvUri
import com.cabolabs.openehr.rm_1_0_2.support.identification.*

class OpenEhrXmlSerializer {
   
   def writer
   def builder

   // transforms a Java type into the correspondent openEHR type name
   // EventContext => EVENT_CONTEXT
   private String openEhrType(Object o)
   {
      o.getClass().getSimpleName().replaceAll("[A-Z]", '_$0').toUpperCase().replaceAll( /^_/, '')
   }
   
   private String method(Object obj)
   {
      String type = obj.getClass().getSimpleName()
      String method = 'serialize'+ type
      return method
   }
   
   // this allows to serialize any LOCATABLE
   String serialize(Locatable o, boolean pretty = false)
   {
      return internalSerialize(o, pretty)
   }
   
   // since VERSION is not LOCATABLE, this allows to serialize a VERSION
   String serialize(Version v, boolean pretty = false)
   {
      return internalSerialize(v, pretty)
   }

   // this is used by both entry points, since the code is the same
   private String internalSerialize(Object o, boolean pretty = false)
   {
      writer = new StringWriter()

      if (pretty)
      {
         builder = new MarkupBuilder(writer)
      }
      else
      {
         builder = new MarkupBuilder(new IndentPrinter(new PrintWriter(writer), "", false))
      }

      builder.setDoubleQuotes(true)
      
      String method = this.method(o)
      this."$method"(o)
      
      return writer.toString()
   }

   private void fillLocatable(Locatable o)
   {
      //println 'fillLocatable >> ' + o
      
      String method = this.method(o.name) // text or coded
      builder.name('xsi:type': openEhrType(o.name)) {
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
         this.serializeArchetyped(o.archetype_details)
      }
      
      // this should be an attribute
      //builder.archetype_node_id(o.archetype_node_id)
      
      // TODO: feeder audit
   }
   
   private void serializeOriginalVersion(OriginalVersion v)
   {
      builder.version(xmlns:'http://schemas.openehr.org/v1',
         'xmlns:xsi':'http://www.w3.org/2001/XMLSchema-instance',
         'xsi:type': 'ORIGINAL_VERSION')
      {
         builder.contribution {
            this.serializeObjectRef(v.contribution)
         }
         
         // TODO: AuditDetails could be of subclass attestation
         builder.commit_audit {
            this.serializeAuditDetails(v.commit_audit)
         }
         
         if (v.signature)
         {
            builder.signature(v.signature)
         }
            
         builder.uid('xsi:type': 'OBJECT_VERSION_ID') {
            this.serializeObjectVersionId(v.uid)
         }
         
         if (v.data)
         {
            String type = v.data.getClass().getSimpleName()
            String method = 'serialize'+ type +'Internals'
            builder.data('xsi:type': openEhrType(v.data), archetype_node_id: v.data.archetype_node_id) {
               this."$method"(v.data)
            }
         }
         
         if (v.preceding_version_uid)
         {
            builder.preceding_version_uid {
               this.serializeObjectVersionId(v.preceding_version_uid)
            }
         }
         
         // TODO: preceding_version_uid
         
         
         if (v.attestations)
         {
            v.attestations.each { attestation ->
               builder.attestations {
                  this.serializeAttestation(attestation)
               }
            }
         }
         
         builder.lifecycle_state {
            this.serializeDvCodedText(v.lifecycle_state)
         }
      }
   }
   
   // only attributes without root node
   void serializeCompositionInternals(Composition c)
   {
      String method
      
      //generateCompositionHeader(addParticipations) // name, language, territory, ...
      //generateCompositionContent(opt.definition.archetypeId)
      fillLocatable(c)
      builder.language {
         serializeCodePhrase(c.language)
      }
      builder.territory {
         serializeCodePhrase(c.territory)
      }
      builder.category {
         serializeDvCodedText(c.category)
      }
     
      method = this.method(c.composer)
      builder.composer('xsi:type': this.openEhrType(c.composer)) {
         this."$method"(c.composer)
      }
      
      if (c.context)
      {
        builder.context() {
           serializeEventContext(c.context)
        }
      }
       
      c.content.each { content_item ->
       
         method = this.method(content_item)
         builder.content('xsi:type': this.openEhrType(content_item), archetype_node_id: content_item.archetype_node_id) {
            this."$method"(content_item)
         }
      }
   }
   
   void serializeComposition(Composition c)
   {
      builder.composition(xmlns:'http://schemas.openehr.org/v1',
         'xmlns:xsi':'http://www.w3.org/2001/XMLSchema-instance',
         'xsi:type':'COMPOSITION',
         archetype_node_id: c.archetype_node_id)
      {
         this.serializeCompositionInternals(c)
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
      
      method = this.method(o.performer)
      builder.performer('xsi:type': this.openEhrType(o.performer)) {
         this."$method"(o.performer)
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
   }
   
   private void serializeAuditDetails(AuditDetails a)
   {
      builder.system_id(a.system_id)
      
      String method = this.method(a.committer)
      builder.committer('xsi:type': openEhrType(a.committer)) {
         this."$method"(a.committer)
      }
      
      builder.time_committed {
         this.serializeDvDateTime(a.time_committed)
      }
      
      builder.change_type {
         this.serializeDvCodedText(a.change_type)
      }
      
      if (a.description)
      {
         method = this.method(a.description)
         builder.description('xsi:type': openEhrType(a.description)) {
            this."$method"(a.description)
         }
      }
   }
   
   private void serializeAttestation(Attestation a)
   {
      // AuditDetails fields
      builder.system_id(a.system_id)
      
      def method
      method = this.method(a.committer)
      builder.committer('xsi:type': openEhrType(a.committer)) {
         this."$method"(a.committer)
      }
      
      builder.time_committed {
         this.serializeDvDateTime(a.time_committed)
      }
      
      builder.change_type {
         this.serializeDvCodedText(a.change_type)
      }
      
      if (a.description)
      {
         method = this.method(a.description)
         builder.description('xsi:type': openEhrType(a.description)) {
            this."$method"(a.description)
         }
      }
      
      // Attestation fields
      // TODO:
   }

   private void fillPartyProxy(PartyProxy o)
   {
      if (o.external_ref)
      {
         def method = this.method(o.external_ref)
         builder.external_ref() { //'xsi:type': openEhrType(o.external_ref)) {
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
      def method = this.method(o.id)
      builder.id('xsi:type': openEhrType(o.id)) {
         this."$method"(o.id)
      }
      
      builder.namespace(o.namespace)

      builder.type(o.type)
   }

   private void serializePartyRef(PartyRef o)
   {
      def method = this.method(o.id)
      builder.id('xsi:type': openEhrType(o.id)) {
         this."$method"(o.id)
      }
      
      builder.namespace(o.namespace)

      builder.type(o.type)
   }

   private void serializeLocatableRef(LocatableRef o)
   {
      def method = this.method(o.id)
      builder.id('xsi:type': openEhrType(o.id)) {
         this."$method"(o.id)
      }
      
      builder.namespace(o.namespace)

      builder.type(o.type)

      if (o.path)
      {
         builder.path(o.path)
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

      builder.scheme(o.scheme)
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
         builder.protocol('xsi:type': this.openEhrType(o.protocol), archetype_node_id: o.protocol.archetype_node_id) {
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
      builder.data('xsi:type': this.openEhrType(o.data), archetype_node_id: o.data.archetype_node_id) {
         this."$method"(o.data)
      }
      
      if (o.state)
      {
         method = this.method(o.state)
         builder.state('xsi:type': this.openEhrType(o.state), archetype_node_id: o.state.archetype_node_id) {
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
   
   
   
   void serializeItemTree(ItemTree o)
   {
      this.fillLocatable(o)
      
      String method
      o.items.each { item ->
         method = this.method(item)
         builder.items('xsi:type': this.openEhrType(item), archetype_node_id: item.archetype_node_id) {
            this."$method"(item)
         }
      }
   }
   
   void serializeItemList(ItemList o)
   {
      this.fillLocatable(o)

      o.items.each { item ->
         builder.items('xsi:type': 'ELEMENT', archetype_node_id: item.archetype_node_id) {
            this.serializeElement(item)
         }
      }
   }
   
   void serializeItemSingle(ItemSingle o)
   {
      this.fillLocatable(o)
      
      builder.item(archetype_node_id: o.item.archetype_node_id) {
         this.serializeElement(o.item)
      }
   }
   
   void serializeItemTable(ItemTable o)
   {
      this.fillLocatable(o)
      
      o.rows.each { cluster ->
         builder.rows(archetype_node_id: cluster.archetype_node_id) {
            this.serializeCluster(cluster)
         }
      }
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
         builder.items('xsi:type': this.openEhrType(item), archetype_node_id: item.archetype_node_id) {
            this."$method"(item)
         }
      }
   }
   
   void serializeSection(Section s)
   {
      this.fillLocatable(s)
      
      s.items.each { content_item ->
         
         method = this.method(content_item)
         items('xsi:type': this.openEhrType(content_item), archetype_node_id: content_item.archetype_node_id) {
            this."$method"(content_item)
         }
      }
   }

   void serializeObservation(Observation o)
   {
      this.fillLocatable(o)
      this.fillCareEntry(o)
      
      builder.data(archetype_node_id: o.data.archetype_node_id) {
         this.serializeHistory(o.data)
      }
      
      if (o.state)
      {
         builder.state(archetype_node_id: o.state.archetype_node_id) {
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
      
      String method

      if (o.summary)
      {
         method = this.method(o.summary)
         builder.summary('xsi:type': this.openEhrType(o.summary), archetype_node_id: o.summary.archetype_node_id) {
            this."$method"(o.summary)
         }
      }
      
      o.events.each { event ->
         method = this.method(event)
         builder.events('xsi:type': this.openEhrType(event), archetype_node_id: event.archetype_node_id) {
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
      builder.data('xsi:type': this.openEhrType(o.data), archetype_node_id: o.archetype_node_id) {
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
         builder.activities(archetype_node_id: activity.archetype_node_id) {
            this.serializeActivity(activity)
         }
      }
   }
   
   void serializeActivity(Activity o)
   {
      this.fillLocatable(o)
      
      String method = this.method(o.description)
      builder.description('xsi:type': this.openEhrType(o.description), archetype_node_id: o.description.archetype_node_id) {
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
      
      builder.time {
         this.serializeDvDateTime(o.time)
      }
      
      String method = this.method(o.description)
      builder.description('xsi:type': this.openEhrType(o.description), archetype_node_id: o.description.archetype_node_id) {
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
      builder.data('xsi:type': this.openEhrType(o.data), archetype_node_id: o.data.archetype_node_id) {
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
      
      builder.defining_code() {
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
      builder.terminology_id() {
         this.serializeTerminologyId(o.terminology_id)
      }
      
      builder.code_string(o.code_string)
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
   
   void serializeDvCount(DvCount o)
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

   void fillEncapsulated(DvEncapsulated o)
   {
      if (o.charset)
      {
         builder.charset {
            this.serializeCodePhrase(o.charset)
         }
      }

      if (o.language)
      {
         builder.language {
            this.serializeCodePhrase(o.language)
         }
      }

      // size is not in the XSD
      //builder.size(o.size)
   }
   
   void serializeDvParsable(DvParsable o)
   {
      this.fillEncapsulated(o)

      builder.value {
         mkp.yield(o.value) // escapes control characters
      }

      builder.formalism(o.formalism)
   }
   
   void serializeDvMultimedia(DvMultimedia o)
   {
      this.fillEncapsulated(o)
      
      if (o.alternate_text)
      {
         builder.alternate_text(o.alternate_text)
      }
      
      if (o.uri)
      {
         builder.uri {
            this.serlializeDvUri(o.uri)
         }
      }
      
      if (o.data)
      {
         builder.data(new String(o.data)) //o.data.encodeBase64().toString()) no need to reencode because the data is stored encoded
      }
      
      builder.media_type {
         this.serializeCodePhrase(o.media_type)
      }
      
      if (o.compression_algorithm)
      {
         builder.compression_algorithm {
            this.serializeCodePhrase(o.compression_algorithm)
         }
      }
      
      if (o.integrity_check)
      {
         builder.integrity_check(o.integrity_check.encodeBase64().toString())
      }

      if (o.integrity_check_algorithm)
      {
         builder.integrity_check_algorithm {
            this.serializeCodePhrase(o.integrity_check_algorithm)
         }
      }
      
      builder.size(o.size)

      if (o.thumbnail)
      {
         builder.thumbnail {
            this.serializeDvMultimedia(o.thumbnail)
         }
      }
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
      String method = this.method(o.lower)
      builder.lower('xsi:type': this.openEhrType(o.lower)) {
         this."$method"(o.lower)
      }
      method = this.method(o.upper)
      builder.upper('xsi:type': this.openEhrType(o.upper)) {
         this."$method"(o.upper)
      }

      // _included are optional in the XSD
      if (o.lower_included != null)
      {
         builder.lower_included(o.lower_included)
      }

      if (o.upper_included != null)
      {
         builder.upper_included(o.upper_included)
      }

      // _unbounded are mandatory in the XSD
      builder.lower_unbounded(o.lower_unbounded)
      
      builder.upper_unbounded(o.upper_unbounded)
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
