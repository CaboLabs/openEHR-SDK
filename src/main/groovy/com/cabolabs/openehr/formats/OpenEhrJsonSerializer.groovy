package com.cabolabs.openehr.formats

import java.text.SimpleDateFormat
import groovy.json.*

import com.cabolabs.openehr.rm_1_0_2.ehr.EhrStatus
import com.cabolabs.openehr.rm_1_0_2.common.generic.*
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Archetyped
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.rm_1_0_2.common.change_control.OriginalVersion
import com.cabolabs.openehr.rm_1_0_2.common.change_control.Version
import com.cabolabs.openehr.rm_1_0_2.common.directory.Folder
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import com.cabolabs.openehr.rm_1_0_2.composition.EventContext
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.*
import com.cabolabs.openehr.rm_1_0_2.composition.content.navigation.Section
import com.cabolabs.openehr.rm_1_0_2.data_structures.history.Event
import com.cabolabs.openehr.rm_1_0_2.data_structures.history.History
import com.cabolabs.openehr.rm_1_0_2.data_structures.history.PointEvent
import com.cabolabs.openehr.rm_1_0_2.data_structures.history.IntervalEvent
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.*
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.Cluster
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.Element
import com.cabolabs.openehr.rm_1_0_2.data_types.basic.DvBoolean
import com.cabolabs.openehr.rm_1_0_2.data_types.basic.DvIdentifier
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.*
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDate
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDuration
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvTime
import com.cabolabs.openehr.rm_1_0_2.data_types.encapsulated.*
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvText
import com.cabolabs.openehr.rm_1_0_2.data_types.text.CodePhrase
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvCodedText
import com.cabolabs.openehr.rm_1_0_2.data_types.uri.DvUri
import com.cabolabs.openehr.rm_1_0_2.data_types.uri.DvEhrUri
import com.cabolabs.openehr.rm_1_0_2.support.identification.*
import com.cabolabs.openehr.rm_1_0_2.demographic.*
import com.cabolabs.openehr.dto_1_0_2.ehr.EhrDto
import com.cabolabs.openehr.dto_1_0_2.demographic.*

class OpenEhrJsonSerializer {

   boolean pretty

   public OpenEhrJsonSerializer()
   {
      this(false)
   }

   public OpenEhrJsonSerializer(boolean pretty)
   {
      this.pretty = pretty
   }

   // TODO: comply with the JSON date format
   //def dateFormatter = new SimpleDateFormat("yyyyMMdd'T'HHmmss,SSSZ")

   // transforms a Java type into the correspondent openEHR type name
   // EventContext => EVENT_CONTEXT
   private String openEhrType(Object o)
   {
      String clazz = o.getClass().getSimpleName()
      if (clazz == "Organization") clazz = "Organisation" // alias of UK based RM!
      else if (clazz == "OrganizationDto") clazz = "Organisation" // alias of UK based RM!
      clazz.replaceAll("[A-Z]", '_$0').toUpperCase().replaceAll( /^_/, '') - '_DTO' // if the type is XXX_DTO, removes _DTO
   }

   private String method(Object obj)
   {
      String type = obj.getClass().getSimpleName()
      String method = 'serialize'+ type
      return method
   }

   private String encode(Map m)
   {
      if (this.pretty)
      {
         return JsonOutput.prettyPrint(JsonOutput.toJson(m))
      }

      return JsonOutput.toJson(m)
   }

   String serialize(Locatable o)
   {
      def out = this.toMap(o)
      this.encode(out)
   }

   Map toMap(Locatable o)
   {
      String method = this.method(o)
      return this."$method"(o)
   }

   String serialize(Version o)
   {
      def out = this.toMap(o)
      this.encode(out)
   }

   Map toMap(Version o)
   {
      String method = this.method(o)
      return this."$method"(o) // e.g. serializeOriginalVersion
   }

   String serialize(EhrDto ehr)
   {
      Map out = this.toMap(ehr)
      this.encode(out)
   }

   Map toMap(EhrDto ehr)
   {
      [
         _type: 'EHR',
         ehr_id: this.serializeHierObjectId(ehr.ehr_id),           // TODO: no need to serialize the _type
         system_id: this.serializeHierObjectId(ehr.system_id),     // TODO: no need to serialize the _type
         time_created: this.serializeDvDateTime(ehr.time_created),
         ehr_status: this.serializeEhrStatus(ehr.ehr_status)
      ]
   }

   private void fillLocatable(Locatable o, Map out)
   {
      String method = this.method(o.name) // text or coded

      // adds the JSON type
      out._type = this.openEhrType(o)

      // adds the type before the rest of the entries
      out.name = [_type: this.openEhrType(o.name)] + this."$method"(o.name)

      if (o.uid)
      {
         method = this.method(o.uid)
         out.uid = this."$method"(o.uid)
      }

      if (o.archetype_details)
      {
         out.archetype_details = this.serializeArchetyped(o.archetype_details)
      }

      if (o.archetype_node_id)
      {
         out.archetype_node_id = o.archetype_node_id
      }

      // TODO: feeder audit
   }

   private void fillPartyDto(PartyDto p, Map out)
   {
      this.fillLocatable(p, out)

      // optional
      if (p.details)
      {
         def method = this.method(p.details)
         out.details = this."$method"(p.details)
      }

      // optional
      if (p.contacts)
      {
         out.contacts = []
         p.contacts.each { contact ->

            out.contacts << this.serializeContact(contact)
         }
      }

      // mandatory, at least 1 object
      out.identities = []
      p.identities.each { identity ->

         out.identities << this.serializePartyIdentity(identity)
      }
   }

   private void fillActorDto(ActorDto a, Map out)
   {
      this.fillPartyDto(a, out)

      // optional
      if (a.languages)
      {
         out.languages = []
         a.languages.each { dvtext ->

            method = this.method(dvtext)
            out.languages << this."$method"(dvtext)
         }
      }

      // optional
      if (a.roles)
      {
         out.roles = []
         a.roles.each { role ->

            out.roles << this.serializeRoleDto(role)
         }
      }
   }

   private Map serializeRoleDto(Role r)
   {
      // for now this is the same as Role
      return this.serializeRole(r)
   }

   private void fillActor(Actor a, Map out)
   {
      this.fillParty(a, out)

      // optional
      if (a.languages)
      {
         out.languages = []
         a.languages.each { dvtext ->

            method = this.method(dvtext)
            out.languages << this."$method"(dvtext)
         }
      }

      // optional
      if (a.roles)
      {
         out.roles = []
         a.roles.each { party_ref ->

            out.roles << this.serializePartyRef(party_ref)
         }
      }
   }

   private void fillParty(Party p, Map out)
   {
      this.fillLocatable(p, out)

      // optional
      if (p.details)
      {
         def method = this.method(p.details)
         out.details = this."$method"(p.details)
      }

      // optional
      if (p.contacts)
      {
         out.contacts = []
         p.contacts.each { contact ->

            out.contacts << this.serializeContact(contact)
         }
      }

      // mandatory, at least 1 object
      out.identities = []
      p.identities.each { identity ->

         out.identities << this.serializePartyIdentity(identity)
      }

      // optional
      if (p.reverse_relationships)
      {
         out.reverse_relationships = []
         p.reverse_relationships.each { locatable_ref ->

            out.reverse_relationships << this.serializeLocatableRef(locatable_ref)
         }
      }
   }

   private Map serializePartyRelationship(PartyRelationship p)
   {
      def out = [:]

      out._type = 'PARTY_RELATIONSHIP'

      this.fillLocatable(p, out)

      // optional
      if (p.details)
      {
         def method = this.method(p.details)
         out.details = this."$method"(p.details)
      }

      if (p.time_validity)
      {
         out.time_validity = this.serializeDvInterval(p.time_validity)
      }

      out.source = this.serializePartyRef(p.source)
      out.target = this.serializePartyRef(p.target)

      return out
   }

   private Map serializeOriginalVersion(OriginalVersion o)
   {
      def out = [:]

      out._type = 'ORIGINAL_VERSION'

      out.contribution = this.serializeObjectRef(o.contribution)

      out.commit_audit = this.serializeAuditDetails(o.commit_audit)

      if (o.signature)
      {
         out.signature = o.signature
      }

      out.uid = this.serializeObjectVersionId(o.uid)

      if (o.data)
      {
         String method = this.method(o.data)
         out.data = this."$method"(o.data) // e.g. serializeComposition
      }

      if (o.preceding_version_uid)
      {
         out.preceding_version_uid = this.serializeObjectVersionId(o.preceding_version_uid)
      }

      if (o.attestations)
      {
         out.attestations = []
         o.attestations.each { attestation ->
            out.attestations << this.serializeAttestation(attestation)
         }
      }

      out.lifecycle_state = this.serializeDvCodedText(o.lifecycle_state)

      return out
   }

   public Map serializeEhrStatus(EhrStatus status)
   {
      def out = [:]

      out._type = 'EHR_STATUS' // TODO: not needed, there is only one use and it's known

      this.fillLocatable(status, out)

      // if the EHR_STATUS was created from an empty payload, it won't have a subject, even if the RM requires one...
      if (status.subject)
      {
         out.subject = this.serializePartySelf(status.subject)
      }

      out.is_modifiable = status.is_modifiable
      out.is_queryable = status.is_queryable

      if (status.other_details)
      {
         def method = this.method(status.other_details)
         out.other_details = this."$method"(status.other_details)
      }

      return out
   }

   public Map serializeFolder(Folder folder)
   {
      def out = [:]

      out._type = 'FOLDER'

      this.fillLocatable(folder, out)

      def method, _item

      out.items = []
      folder.items.each { object_ref ->

         method = this.method(object_ref)
         _item = [_type: this.openEhrType(object_ref)] + this."$method"(object_ref)
         out.items << _item
      }

      out.folders = []
      folder.folders.each { subfolder ->

         out.folders << this.serializeFolder(subfolder)
      }

      return out
   }

   private Map serializeComposition(Composition o)
   {
      def out = [:]

      out._type = 'COMPOSITION'

      this.fillLocatable(o, out)

      out.language = this.serializeCodePhrase(o.language)

      out.territory = this.serializeCodePhrase(o.territory)

      out.category = this.serializeDvCodedText(o.category)

      String method = this.method(o.composer)

      // adds the type before the rest of the entries
      out.composer = [_type: this.openEhrType(o.composer)] + this."$method"(o.composer)

      if (o.context)
      {
         out.context = this.serializeEventContext(o.context)
      }

      out.content = []
      o.content.each { content_item ->

         method = this.method(content_item)
         out.content << this."$method"(content_item)
      }

      return out
   }

   private Map serializePersonDto(PersonDto p)
   {
      def out = [:]

      out._type = 'PERSON'

      this.fillActorDto(p, out)

      return out
   }

   private Map serializeOrganizationDto(OrganizationDto p)
   {
      def out = [:]

      out._type = 'ORGANISATION'

      this.fillActorDto(p, out)

      return out
   }

   private Map serializeGroupDto(GroupDto p)
   {
      def out = [:]

      out._type = 'GROUP'

      this.fillActorDto(p, out)

      return out
   }

   private Map serializeAgentDto(AgentDto p)
   {
      def out = [:]

      out._type = 'AGENT'

      this.fillActorDto(p, out)

      return out
   }

   private Map serializePerson(Person p)
   {
      def out = [:]

      out._type = 'PERSON'

      this.fillActor(p, out)

      return out
   }

   private Map serializeOrganization(Organization o)
   {
      def out = [:]

      out._type = 'ORGANISATION'

      this.fillActor(o, out)

      return out
   }

   private Map serializeGroup(Group g)
   {
      def out = [:]

      out._type = 'GROUP'

      this.fillActor(g, out)

      return out
   }

   private Map serializeAgent(Agent a)
   {
      def out = [:]

      out._type = 'AGENT'

      this.fillActor(a, out)

      return out
   }

   private Map serializeRole(Role r)
   {
      def out = [:]

      out._type = 'ROLE'

      this.fillParty(r, out)

      if (r.time_validity)
         out.time_validity = this.serializeDvInterval(r.time_validity)

      // For API the role doesn't have performer, it's the Actor that contains
      // the Role
      if (r.performer)
      {
         out.performer = this.serializePartyRef(r.performer)
      }

      out.capabilities = []
      r.capabilities.each { capability ->
         out.capabilities << serializeCapability(capability)
      }

      return out
   }

   private Map serializeCapability(Capability c)
   {
      def out = [:]

      this.fillLocatable(c, out)

      String method = this.method(c.credentials) // ITEM_STRUCTURE
      out.credentials = this."$method"(c.credentials)

      if (c.time_validity)
         out.time_validity = this.serializeDvInterval(c.time_validity)

      return out
   }

   private Map serializeContact(Contact c)
   {
      def out = [:]

      this.fillLocatable(c, out)

      if (c.time_validity)
      {
         out.time_validity = this.serializeDvInterval(c.time_validity)
      }

      out.addresses = []
      c.addresses.each { address ->

         out.addresses << this.serializeAddress(address)
      }

      return out
   }

   private Map serializeAddress(Address ad)
   {
      def out = [:]

      this.fillLocatable(ad, out)

      def method = this.method(ad.details)
      out.details = this."$method"(ad.details)

      return out
   }

   private Map serializePartyIdentity(PartyIdentity pi)
   {
      def out = [:]

      this.fillLocatable(pi, out)

      def method = this.method(pi.details)
      out.details = this."$method"(pi.details)

      return out
   }

   private Map serializeEventContext(EventContext o)
   {
      Map out = [:]

      out.start_time = this.serializeDvDateTime(o.start_time)

      if (o.end_time)
      {
         out.end_time = this.erializeDvDateTime(o.end_time)
      }

      if (o.location)
      {
         out.location = o.location
      }

      out.setting = this.serializeDvCodedText(o.setting)

      if (o.other_context)
      {
         String method = this.method(o.other_context)
         out.other_context = this."$method"(o.other_context)
         out.other_context._type = this.openEhrType(o.other_context)
      }

      if (o.health_care_facility)
      {
         out.health_care_facility = this.serializePartyIdentified(o.health_care_facility)
      }

      if (o.participations)
      {
         out.participations = []
         o.participations.each { participation ->

            out.participations << this.serializeParticipation(participation)
         }
      }

      return out
   }

   private Map serializeParticipation(Participation o)
   {
      Map out = [:]

      String method = this.method(o.function) // text or coded text
      out.function = [_type: this.openEhrType(o.function)] + this."$method"(o.function)

      method = this.method(o.performer)
      out.performer = [_type: this.openEhrType(o.performer)] + this."$method"(o.performer)

      if (o.time)
      {
         out.time = this.serializeDvInterval(o.time)
      }

      out.mode = this.serializeDvCodedText(o.mode)

      return out
   }

   private Map serializeAuditDetails(AuditDetails a)
   {
      Map out = [:]

      out.system_id = a.system_id

      String method
      method = this.method(a.committer)
      out.committer = [_type: this.openEhrType(a.committer)] + this."$method"(a.committer)

      out.time_committed = this.serializeDvDateTime(a.time_committed)

      out.change_type = this.serializeDvCodedText(a.change_type)

      if (a.description)
      {
         method = this.method(a.description)
         out.description = [_type: this.openEhrType(a.description)] + this."$method"(a.description)
      }

      return out
   }

   private Map serializeAttestation(Attestation a)
   {
      Map out = [:]

      // AuditDetails fields
      out.system_id = a.system_id

      def method
      method = this.method(a.committer)
      out.committer = [_type: this.openEhrType(a.committer)] + this."$method"(a.committer)

      out.time_committed = this.serializeDvDateTime(a.time_committed)

      out.change_type = this.serializeDvCodedText(a.change_type)

      if (a.description)
      {
         method = this.method(a.description)
         out.description = [_type: this.openEhrType(a.description)] + this."$method"(a.description)
      }

      // Attestation fields
      // TODO:

      return out
   }

   private void fillPartyProxy(PartyProxy o, Map out)
   {
      if (o.external_ref)
      {
         def method = this.method(o.external_ref)
         out.external_ref = this."$method"(o.external_ref) // doesn't need a _type is always PARTY_REF
      }
   }

   private Map serializePartySelf(PartySelf o)
   {
      Map out = [:]

      this.fillPartyProxy(o, out)

      return out
   }

   private Map serializePartyIdentified(PartyIdentified o)
   {
      Map out = [:]

      this.fillPartyProxy(o, out)

      if (o.name)
      {
         out.name = o.name
      }

      if (o.identifiers)
      {
         out.identifiers = []
         o.identifiers.each { identifier ->
            out.identifiers << this.serializeDvIdentifier(identifier)
         }
      }

      return out
   }

   private Map serializePartyRelated(PartyRelated o)
   {
      Map out = [:]

      this.fillPartyProxy(o, out)

      if (o.name)
      {
         out.name = o.name
      }

      if (o.identifiers)
      {
         out.identifiers = []
         o.identifiers.each { identifier ->
            out.identifiers << this.serializeDvIdentifier(identifier)
         }
      }

      out.relationship = this.serializeDvCodedText(o.relationship)

      return out
   }

   private Map serializeObjectRef(ObjectRef o)
   {
      def out = [:]

      def method = this.method(o.id)

      out.id = [_type: this.openEhrType(o.id)] + this."$method"(o.id)

      out.namespace = o.namespace

      out.type = o.type

      return out
   }

   private Map serializePartyRef(PartyRef o)
   {
      return this.serializeObjectRef(o)
   }

   private Map serializeLocatableRef(LocatableRef o)
   {
      Map out = this.serializeObjectRef(o)

      if (o.path)
      {
         out.path = o.path
      }

      return out
   }

   private void fillObjectId(ObjectId o, Map out)
   {
      out.value = o.value
   }

   private Map serializeTerminologyId(TerminologyId o)
   {
      Map out = [_type: 'TERMINOLOGY_ID']

      this.fillObjectId(o, out)

      return out
   }

   private Map serializeGenericId(GenericId o)
   {
      Map out = [_type: 'GENERIC_ID']

      this.fillObjectId(o, out)

      out.scheme = o.scheme

      return out
   }

   private Map serializeArchetypeId(ArchetypeId o)
   {
      Map out = [:] //[_type: 'ARCHETYPE_ID'] // NOTE: all uses of ARCHETYPE_ID are known, type is optional

      this.fillObjectId(o, out)

      return out
   }

   private Map serializeObjectVersionId(ObjectVersionId o)
   {
      Map out = [_type: 'OBJECT_VERSION_ID']

      this.fillObjectId(o, out)

      return out
   }

   private Map serializeHierObjectId(HierObjectId o)
   {
      Map out = [_type: 'HIER_OBJECT_ID']

      this.fillObjectId(o, out)

      return out
   }

   private Map serializeTemplateId(TemplateId o)
   {
      Map out = [:] //[_type: 'TEMPLATE_ID'] // NOTE: all uses of TEMPLATE_ID are known, type is optional

      this.fillObjectId(o, out)

      return out
   }


   private void fillEntry(Entry o, Map out)
   {
      out.language = this.serializeCodePhrase(o.language)

      out.encoding = this.serializeCodePhrase(o.encoding)

      String method = this.method(o.subject)
      out.subject = [_type: this.openEhrType(o.subject)] + this."$method"(o.subject)

      if (o.provider)
      {
         method = this.method(o.provider)
         out.provider = [_type: this.openEhrType(o.provider)] + this."$method"(o.provider)
      }

      if (o.other_participations)
      {
         out.other_participations = []
         o.other_participations.each { participation ->

            out.other_participations << this.serializeParticipation(participation)
         }
      }

      if (o.workflow_id)
      {
         method = this.method(o.workflow_id)
         out.workflow_id = [_type: this.openEhrType(o.workflow_id)] + this."$method"(o.workflow_id)
      }
   }

   private void fillCareEntry(CareEntry o, Map out)
   {
      this.fillEntry(o, out)

      if (o.protocol)
      {
         String method = this.method(o.protocol)
         out.protocol = [_type: this.openEhrType(o.protocol)] + this."$method"(o.protocol)

         // TODO: check if this is needed
         //archetype_node_id: o.protocol.archetype_node_id)
      }

      if (o.guideline_id)
      {
         method = this.method(o.guideline_id)
         out.guideline_id = [_type: this.openEhrType(o.guideline_id)] + this."$method"(o.guideline_id)
      }
   }

   private void fillEvent(Event o, Map out)
   {
      this.fillLocatable(o, out)

      out.time = this.serializeDvDateTime(o.time)

      String method = this.method(o.data)
      out.data = [_type: this.openEhrType(o.data)] + this."$method"(o.data)

      // TODO: check if this is needed
      //archetype_node_id: o.data.archetype_node_id)

      if (o.state)
      {
         method = this.method(o.state)
         out.state = [_type: this.openEhrType(o.state)] + this."$method"(o.state)

         // TODO: check if this is needed
         //archetype_node_id: o.state.archetype_node_id)
      }
   }

   private Map serializeArchetyped(Archetyped o)
   {
      Map out = [:]

      out.archetype_id = this.serializeArchetypeId(o.archetype_id)
      out.template_id = this.serializeTemplateId(o.template_id)
      out.rm_version = o.rm_version

      return out
   }



   private Map serializeItemTree(ItemTree o)
   {
      def out = [:]

      this.fillLocatable(o, out)

      String method
      Map _item

      out.items = []
      o.items.each { item ->
         method = this.method(item)
         _item = [_type: this.openEhrType(item)] + this."$method"(item)
         out.items << _item
      }

      return out
   }

   private Map serializeItemList(ItemList o)
   {
      def out = [:]

      this.fillLocatable(o, out)

      Map _item

      out.items = []
      o.items.each { item ->
         _item = [_type: 'ELEMENT'] + this.serializeElement(item)
         out.items << _item
      }

      return out
   }

   private Map serializeItemSingle(ItemSingle o)
   {
      def out = [:]

      this.fillLocatable(o, out)

      out.item = this.serializeElement(o.item)

      return out
   }

   private Map serializeItemTable(ItemTable o)
   {
      def out = [:]

      this.fillLocatable(o, out)

      out.rows = []
      o.rows.each { cluster ->
         out.rows << this.serializeCluster(cluster)
      }

      return out
   }


   private Map serializeSection(Section s)
   {
      def out = [:]

      this.fillLocatable(s, out)

      Map _item
      def method

      out.items = []
      s.items.each { content_item ->

         method = this.method(content_item)
         _item = [_type: this.openEhrType(content_item)] + this."$method"(content_item)
         out.items << _item
      }

      return out
   }

   private Map serializeObservation(Observation o)
   {
      def out = [:]

      this.fillLocatable(o, out)
      this.fillCareEntry(o, out)

      out.data = this.serializeHistory(o.data)

      if (o.state)
      {
         out.state = this.serializeHistory(o.state)
      }

      return out
   }

   private Map serializeHistory(History o)
   {
      def out = [:]

      this.fillLocatable(o, out)

      out.origin = this.serializeDvDateTime(o.origin)

      if (o.period)
      {
         out.period = this.serializeDvDuration(o.period)
      }

      if (o.duration)
      {
         out.duration = this.serializeDvDuration(o.duration)
      }

      String method

      if (o.summary)
      {
         method = this.method(o.summary)
         out.summary = [_type: this.openEhrType(o.summary)] + this."$method"(o.summary)
      }

      Map _event

      out.events = []
      o.events.each { event ->

         method = this.method(event)
         _event = [_type: this.openEhrType(event)] + this."$method"(event)
         out.events << _event
      }

      return out
   }

   private Map serializePointEvent(PointEvent o)
   {
      def out = [:]

      this.fillEvent(o, out)

      return out
   }

   private Map serializeIntervalEvent(IntervalEvent o)
   {
      def out = [:]

      this.fillEvent(o, out)

      out.width = this.serializeDvDuration(o.width)

      out.math_function = this.serializeDvCodedText(o.math_function)

      if (o.sample_count)
      {
         out.sample_count = o.sample_count
      }

      return out
   }

   private Map serializeEvaluation(Evaluation o)
   {
      def out = [:]

      this.fillLocatable(o, out)
      this.fillCareEntry(o, out)

      String method = this.method(o.data)
      out.data = [_type: this.openEhrType(o.data)] + this."$method"(o.data)

      return out
   }

   private Map serializeInstruction(Instruction o)
   {
      def out = [:]

      this.fillLocatable(o, out)
      this.fillCareEntry(o, out)

      String method = this.method(o.narrative)
      out.narrative = [_type: this.openEhrType(o.narrative)] + this."$method"(o.narrative)

      if (o.expiry_time)
      {
         out.expirity_time = this.serializeDvDateTime(o.expiry_time)
      }

      if (o.wf_definition)
      {
         out.wf_definition = this.serializeDvParsable(o.wf_definition)
      }

      if (o.activities)
      {
         out.activities = []
         o.activities.each { activity ->
            out.activities << this.serializeActivity(activity)

            // TODO: check if needed
            // archetype_node_id: activity.archetype_node_id
         }
      }

      return out
   }

   private Map serializeActivity(Activity o)
   {
      def out = [:]

      this.fillLocatable(o, out)

      String method = this.method(o.description)
      out.description = [_type: this.openEhrType(o.description)] + this."$method"(o.description)

      out.timing = this.serializeDvParsable(o.timing)

      out.action_archetype_id = o.action_archetype_id

      return out
   }

   private Map serializeAction(Action o)
   {
      def out = [:]

      this.fillLocatable(o, out)
      this.fillCareEntry(o, out)

      out.time = this.serializeDvDateTime(o.time)

      String method = this.method(o.description)
      out.description = [_type: this.openEhrType(o.description)] + this."$method"(o.description)

      // TODO: check if needed
      //archetype_node_id: o.description.archetype_node_id) {

      out.ism_transition = this.serializeIsmTransition(o.ism_transition)

      if (o.instruction_details)
      {
         out.instruction_details = this.serializeInstructionDetails(o.instruction_details)
      }

      return out
   }

   private Map serializeIsmTransition(IsmTransition o)
   {
      def out = [:]

      out.current_state = this.serializeDvCodedText(o.current_state)

      if (o.transition)
      {
         out.transition = this.serializeDvCodedText(o.transition)
      }

      if (o.careflow_step)
      {
         out.careflow_step = this.serializeDvCodedText(o.careflow_step)
      }

      return out
   }

   private Map serializeInstructionDetails(InstructionDetails o)
   {
      def out = [:]

      out.instruction_id = this.serializeLocatableRef(o.instruction_id)

      out.activity_id = o.activity_id

      if (o.wf_details)
      {
         String method = this.method(o.wf_details)
         out.wf_details = [_type: this.openEhrType(o.wf_details)] + this."$method"(o.wf_details)
      }

      return out
   }

   private Map serializeAdminEntry(AdminEntry o)
   {
      def out = [:]

      this.fillLocatable(o, out)
      this.fillEntry(o, out)

      String method = this.method(o.data)
      out.data = [_type: this.openEhrType(o.data)] + this."$method"(o.data)

      // TODO: check if needed
      //archetype_node_id: o.data.archetype_node_id)

      return out
   }



   private Map serializeCluster(Cluster o)
   {
      def out = [:]

      this.fillLocatable(o, out)

      String method
      Map _item

      out.items = []
      o.items.each { item ->
         method = this.method(item)
         _item = [_type: this.openEhrType(item)] + this."$method"(item)
         out.items << _item
      }

      return out
   }

   private Map serializeElement(Element o)
   {
      def out = [:]

      this.fillLocatable(o, out)

      if (o.value)
      {
         String method = this.method(o.value)
         out.value = [_type: this.openEhrType(o.value)] + this."$method"(o.value)
      }

      if (o.null_flavour)
      {
         out.null_flavour = this.serializeDvCodedText(o.null_flavour)
      }

      return out
   }


   private Map serializeDvDateTime(DvDateTime o)
   {
      def out = [:]

      // TODO: accuracy, ...
      out.value = o.value

      return out
   }

   private Map serializeDvDate(DvDate o)
   {
      def out = [:]

      out.value = o.value

      return out
   }

   private Map serializeDvTime(DvTime o)
   {
      def out = [:]

      // TODO
      out.value = o.value

      return out
   }

   private Map serializeDvText(DvText o)
   {
      def out = [:]

      out.value = o.value

      // TODO: mappings

      return out
   }

   private Map serializeDvCodedText(DvCodedText o)
   {
      def out = [:]

      out.value = o.value
      out.defining_code = this.serializeCodePhrase(o.defining_code)

      return out
   }

   private Map serializeDvOrdinal(DvOrdinal o)
   {
      def out = [:]

      out.value = o.value
      out.symbol = this.serializeDvCodedText(o.symbol)

      return out
   }

   private Map serializeDvDuration(DvDuration o)
   {
      def out = [:]

     out.value = o.value

     // TODO: accuracy, magnitude_status, ... all attributes from superclasses

      return out
   }

   private Map serializeDvBoolean(DvBoolean o)
   {
      def out = [:]

      out.value = o.value

      return out
   }

   private Map serializeCodePhrase(CodePhrase o)
   {
      def out = [:]

      out.terminology_id = this.serializeTerminologyId(o.terminology_id)

      out.code_string = o.code_string

      return out
   }

   private Map serializeDvIdentifier(DvIdentifier o)
   {
      def out = [:]

      out.id = o.id
      if (o.issuer) out.issuer = o.issuer
      if (o.assigner) out.assigner = o.assigner
      if (o.type) out.type = o.type

      return out
   }

   private Map serializeDvQuantity(DvQuantity o)
   {
      def out = [:]

      // TODO: inherited attributes

      out.magnitude = o.magnitude
      out.units = o.units

      if (o.precision)
      {
         out.precision = o.precision
      }

      return out
   }

   private Map serializeDvCount(DvCount o)
   {
      def out = [:]

      // TODO: inherited attributes

      out.magnitude = o.magnitude

      return out
   }

   private Map serializeDvProportion(DvProportion o)
   {
      def out = [:]

      // TODO: inherited attributes

      out.numerator = o.numerator
      out.denominator = o.denominator
      out.type = o.type

      if (o.precision != null && o.precision >= 0)
      {
         out.precision = o.precision
      }

      return out
   }

   private Map fillEncapsulated(DvEncapsulated o, Map out)
   {
      if (o.charset)
      {
         out.charset = this.serializeCodePhrase(o.charset)
      }

      if (o.language)
      {
         out.language = this.serializeCodePhrase(o.language)
      }

      // TODO: check if size is an attribute in the schema
      // size is not in the XSD
      //out.size(o.size)
   }

   private Map serializeDvParsable(DvParsable o)
   {
      def out = [:]

      this.fillEncapsulated(o, out)

      // TODO: test if JSON encoding characters are escaped
      out.value = o.value

      out.formalism = o.formalism

      return out
   }

   private Map serializeDvMultimedia(DvMultimedia o)
   {
      def out = [:]

      this.fillEncapsulated(o, out)

      if (o.alternate_text)
      {
         out.alternate_text = o.alternate_text
      }

      if (o.uri)
      {
         out.uri = this.serlializeDvUri(o.uri)
      }

      if (o.data)
      {
         out.data = new String(o.data) //o.data.encodeBase64().toString()no need to reencode because the data is stored encoded
      }

      out.media_type = this.serializeCodePhrase(o.media_type)

      if (o.compression_algorithm)
      {
         out.compression_algorithm = this.serializeCodePhrase(o.compression_algorithm)
      }

      if (o.integrity_check)
      {
         out.integrity_check = o.integrity_check.encodeBase64().toString()
      }

      if (o.integrity_check_algorithm)
      {
         out.integrity_check_algorithm = this.serializeCodePhrase(o.integrity_check_algorithm)
      }

      out.size = o.size

      if (o.thumbnail)
      {
         out.thumbnail = this.serializeDvMultimedia(o.thumbnail)
      }

      return out
   }

   private Map serializeDvUri(DvUri o)
   {
      def out = [:]

      out.value = o.value

      return out
   }

   private Map serializeDvEhrUri(DvEhrUri o)
   {
      def out = [:]

      out.value = o.value

      return out
   }

   private Map serializeDvInterval(DvInterval o)
   {
      def out = [:]

      String method

      if (o.lower)
      {
         method = this.method(o.lower)
         out.lower = [_type: this.openEhrType(o.lower)] + this."$method"(o.lower)
      }

      if (o.upper)
      {
         method = this.method(o.upper)
         out.upper = [_type: this.openEhrType(o.upper)] + this."$method"(o.upper)
      }

      out.lower_included  = o.lower_included
      out.lower_unbounded = o.lower_unbounded
      out.upper_included  = o.upper_included
      out.upper_unbounded = o.upper_unbounded

      return out
   }
}