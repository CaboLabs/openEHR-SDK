package com.cabolabs.openehr.formats

import com.cabolabs.openehr.rm_1_0_2.ehr.*
import com.cabolabs.openehr.rm_1_0_2.common.generic.*
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Archetyped
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable
import com.cabolabs.openehr.rm_1_0_2.common.change_control.Contribution
import com.cabolabs.openehr.rm_1_0_2.common.change_control.OriginalVersion
import com.cabolabs.openehr.rm_1_0_2.common.change_control.Version
import com.cabolabs.openehr.rm_1_0_2.common.directory.Folder
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
import com.cabolabs.openehr.rm_1_0_2.support.identification.*
import com.cabolabs.openehr.rm_1_0_2.demographic.*
import com.cabolabs.openehr.dto_1_0_2.ehr.EhrDto
import com.cabolabs.openehr.dto_1_0_2.common.change_control.ContributionDto
import com.cabolabs.openehr.dto_1_0_2.demographic.*
import com.cabolabs.openehr.opt.instance_validation.JsonInstanceValidation
import groovy.json.JsonSlurper

@groovy.util.logging.Slf4j
class OpenEhrJsonParser {

   def jsonValidator

   def schemaValidate
   def schemaFlavor = "rm" // rm | api

   // https://javadoc.io/doc/com.networknt/json-schema-validator/1.0.51/com/networknt/schema/ValidationMessage.html
   // Set<ValidationMessage>
   def jsonValidationErrors

   // if @schemaValidate is true, runs the schema validator before trying to parse
   def OpenEhrJsonParser(boolean schemaValidate = false)
   {
      this.schemaValidate = schemaValidate
   }

   def setSchemaFlavorAPI()
   {
      this.schemaFlavor = "api"
   }

   def setSchemaFlavorRM()
   {
      this.schemaFlavor = "rm"
   }

   def getJsonValidationErrors()
   {
      if (!jsonValidator)
      {
         throw new Exception("schemaValidate option wasn't set to true")
      }

      return this.jsonValidationErrors
   }

   // ========= ENTRY POINTS =========

   /**
    * Used to parse an RM EHR payload. To parse an API EHR JSON, use the parseEhrDto() method.
    * Here the ehr_status is OBJECT_REF.
    */
   Ehr parseEhr(String json)
   {
      def slurper = new JsonSlurper()
      def map = slurper.parseText(json)

      // FIXME: EHR is not LOCATABLE and doesn't have info about the rm_version!
      // For the API EHR we could access the EHR_STATUS.archetype_details.rm_version, but for RM EHRs, ehr_status is an OBJECT_REF that should be resolved.
      if (this.schemaValidate)
      {
         // independently from schemaFlavor, this will always be "rm" since it is asking to parse an RM Ehr
         // for an "api" Ehr the parseEhrDto() method should be used
         this.jsonValidator = new JsonInstanceValidation("rm") // FIXME: this uses the default 1.0.2 version schema!

         def errors = jsonValidator.validate(json)
         if (errors)
         {
            this.jsonValidationErrors = errors
            return
         }
      }

      def ehr = new Ehr()

      ehr.system_id = this.parseHIER_OBJECT_ID(map.system_id)

      ehr.ehr_id = this.parseHIER_OBJECT_ID(map.ehr_id)

      ehr.time_created = this.parseDV_DATE_TIME(map.time_created)

      //log.warn("Not parsed EHR_STATUS: this parser is based on the RM and the model is based on the REST API model. The status should be parsed separatelly")
      ehr.ehr_status = this.parseOBJECT_REF(map.ehr_status)

      // the references to versioned objects are not parsed, for instance, this is the right parsing for a rest EHR response

      return ehr
   }

   /**
    * Used to parse an API EHR payload. To parse an RM EHR JSON, use the parseEhr() method.
    */
   EhrDto parseEhrDto(String json)
   {
      def slurper = new JsonSlurper()
      def map = slurper.parseText(json)

      // FIXME: EHR is not LOCATABLE and doesn't have info about the rm_version!
      // For the API EHR we could access the EHR_STATUS.archetype_details.rm_version, but for RM EHRs, ehr_status is an OBJECT_REF that should be resolved.
      if (this.schemaValidate)
      {
         // independently from schemaFlavor, this will always be "api" since it is asking to parse an API EhrDto
         // for an "rm" Ehr the parseEhr() method should be used
         this.jsonValidator = new JsonInstanceValidation("api") // FIXME: this uses the default 1.0.2 version schema!

         def errors = jsonValidator.validate(json)
         if (errors)
         {
            this.jsonValidationErrors = errors
            return
         }
      }

      def ehr = new EhrDto()

      ehr.system_id = this.parseHIER_OBJECT_ID(map.system_id)

      ehr.ehr_id = this.parseHIER_OBJECT_ID(map.ehr_id)

      ehr.time_created = this.parseDV_DATE_TIME(map.time_created)

      ehr.ehr_status = this.parseEHR_STATUS(map.ehr_status)

      if (map.ehr_access)
      {
         ehr.ehr_access = this.parseEHR_ACCESS(map.ehr_access)
      }

      return ehr
   }

   // FIXME: this should be parseLocatable
   // used to parse compositions and other descendant from Locatable
   // TODO: FOLDER and EHR_STATUS are above, we might need to use this one instead
   Locatable parseJson(String json)
   {
      def slurper = new JsonSlurper()
      def map = slurper.parseText(json)

      if (this.schemaValidate)
      {
         if (!map.archetype_details || !map.archetype_details.rm_version) // rm version aware
         {
            throw new Exception("archetype_details.rm_version is required for the root of any archetypable class")
         }

         // TODO: the schema flavor should be a parameter: rm | api
         this.jsonValidator = new JsonInstanceValidation(this.schemaFlavor, map.archetype_details.rm_version)

         def errors = jsonValidator.validate(json)
         if (errors)
         {
            this.jsonValidationErrors = errors
            return
         }
      }

      String type = map._type

      if (!type)
      {
         throw new JsonParseException("Can't parse JSON if root node doesn't have a value for _type")
      }

      def method = 'parse'+ type
      Locatable out
      try
      {
         out = this."$method"(map)
      }
      catch (Exception e)
      {
         throw new JsonParseException("Can't parse JSON, check ${type} is a LOCATABLE type. If you tried to parse a VERSION, use the parseVersionJson method", e)
      }
      return out
   }

   // used to parse versions because is not Locatable
   Version parseVersionJson(String json)
   {
      def slurper = new JsonSlurper()
      def map = slurper.parseText(json)

      if (this.schemaValidate)
      {
         if (!map.archetype_details || !map.archetype_details.rm_version) // rm version aware
         {
            throw new Exception("archetype_details.rm_version is required for the root of any archetypable class")
         }

         this.jsonValidator = new JsonInstanceValidation(this.schemaFlavor, map.archetype_details.rm_version)

         def errors = jsonValidator.validate(json)
         if (errors)
         {
            this.jsonValidationErrors = errors
            return
         }
      }

      String type = map._type // This is required to know if this is an ORIGINAL_VERSION or IMPORTED_VERSION

      if (!type)
      {
         throw new JsonParseException("Can't parse JSON if root node doesn't have a value for _type")
      }

      def method = 'parse'+ type
      Version out
      try
      {
         out = this."$method"(map)
      }
      catch (Exception e)
      {
         throw new JsonParseException("Can't parse JSON, check ${type} is a VERSION type. If you tried to parse a LOCATABLE, use the parseJson method", e)
      }
      return out
   }

   // Used to parse the payload for POST /contributon of the openEHR REST API
   // NOTE: the payload contains audit, which is ignored by this parser, since could be generated by the server, and there is no schema for that specific object (is a CONTRBUTION with missing uid for instance).
   List<Version> parseVersionList(String json)
   {
      // FIXME: can this be validated against the schema? maybe each individual JSON node

      /*
      {
         "versions": [
            {
               ... a version
            },
            {
               ... a version
            },
            ....
         ]
      }
      */
      def slurper   = new JsonSlurper()
      def map       = slurper.parseText(json)
      String type, method

      List versions = []

      map.versions.each { version_map ->

         type = version_map._type // This is required to know if this is an ORIGINAL_VERSION or IMPORTED_VERSION

         if (!type)
         {
            throw new JsonParseException("Can't parse JSON if root node doesn't have a value for _type")
         }

         method = 'parse'+ type
         versions << this."$method"(version_map)
      }

      return versions
   }

   // TODO: this should be validated in 'api' flavor
   ContributionDto parseContributionDto(String json)
   {
      // TODO: schema validation
      /*
      {
         "versions": [
            {
               ... a version
            },
            {
               ... a version
            },
            ....
         ],
         "audit": {

         }
      }
      */
      def slurper = new JsonSlurper()
      def map = slurper.parseText(json)

      if (this.schemaValidate)
      {
         // if (!map.archetype_details || !map.archetype_details.rm_version) // rm version aware
         // {
         //    throw new Exception("archetype_details.rm_version is required for the root of any archetypable class")
         // }

         // NOTE: since this is DTO, the schema flavor will always be api
         this.jsonValidator = new JsonInstanceValidation('api', '1.0.2') // FIXME: hardcoded rm_version

         def errors = jsonValidator.validate(json)
         if (errors)
         {
            this.jsonValidationErrors = errors
            return
         }
      }

      String type, method
      Version version

      def contribution = new ContributionDto(versions: [])

      // in the DTO the uid is optional
      if (map.uid)
      {
         contribution.uid = this.parseHIER_OBJECT_ID(map.uid)
      }

      map.versions.each { version_map ->

         type = version_map._type // This is required to know if this is an ORIGINAL_VERSION or IMPORTED_VERSION

         if (!type)
         {
            throw new JsonParseException("Can't parse JSON if root node doesn't have a value for _type")
         }

         method = 'parse'+ type
         contribution.versions << this."$method"(version_map) // NOTE: the version dto doesn't require uid while the RM object requires it!
      }

      contribution.audit = this.parseAUDIT_DETAILS(map.audit)

      return contribution
   }

   // TODO: json validation
   Contribution parseContribution(String json)
   {
      // TODO: schema validation
      /*
      {
         "versions": [
            {
               ... an object ref
            },
            {
               ... an object ref
            },
            ....
         ],
         "audit": {

         }
      }
      */
      def slurper = new JsonSlurper()
      def map = slurper.parseText(json)
      String type, method

      def contribution = new Contribution(versions: new HashSet())

      // note for the RM the uid is mandatory, in the DTO the uid is optional
      contribution.uid = this.parseHIER_OBJECT_ID(map.uid)

      map.versions.each { version_map ->

         contribution.versions << this.parseOBJECT_REF(version_map) // contribiution has objectrefs
      }

      contribution.audit = this.parseAUDIT_DETAILS(map.audit)

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


   // ========= UTILS TO CALCULATE PATHS AND DATA PATHS

   private String attr_archetype_path(Map parent, String attr, String parent_path)
   {
      // FIXME: parent[attr] is coming as an array, there is a wrong call
      if (parent[attr].archetype_node_id)
      {
         String path_node_id = (parent[attr].archetype_node_id.startsWith('at') ? parent[attr].archetype_node_id : 'archetype_id='+ parent[attr].archetype_node_id)
         return parent_path != '/' ? parent_path +'/'+ attr +'['+ path_node_id +']' : '/'+ attr +'['+ path_node_id +']'
      }

      return parent_path != '/' ? parent_path +'/'+ attr : '/'+ attr
   }

   // similar to attr_archetype_path but for multiple attributes, in which uses the i to get the right archetype_node_id from the parent collection container
   private String multi_attr_archetype_path(Map parent, String attr, int i, String parent_path)
   {
      if (parent[attr][i].archetype_node_id)
      {
         String path_node_id = (parent[attr][i].archetype_node_id.startsWith('at') ? parent[attr][i].archetype_node_id : 'archetype_id='+ parent[attr][i].archetype_node_id)
         return parent_path != '/' ? parent_path +'/'+ attr +'['+ path_node_id +']' : '/'+ attr +'['+ path_node_id +']'
      }

      return parent_path != '/' ? parent_path +'/'+ attr : '/'+ attr
   }


   private String attr_archetype_path_multiple(Map parent, String attr, int i, String parent_path)
   {
      if (parent[attr][i].archetype_node_id)
      {
         String path_node_id = (parent[attr][i].archetype_node_id.startsWith('at') ? parent[attr][i].archetype_node_id : 'archetype_id='+ parent[attr][i].archetype_node_id)
         return parent_path != '/' ? parent_path +'/'+ attr +'['+ path_node_id +']('+ i +')' : '/'+ attr +'['+ path_node_id +']('+ i +')'
      }

      return parent_path != '/' ? parent_path +'/'+ attr +'('+ i +')' : '/'+ attr +'('+ i +')'
   }

   /**
    * The path and data path for single attributes is the same (just the part that is added)
    */
   private String attr_path(Map parent, String attr, String parent_path)
   {
      // FIXME: parent[attr] is coming as an array, there is a wrong call

      return parent_path != '/' ? parent_path +'/'+ attr : '/'+ attr
   }

   /**
    * The path and data path for multiple attributes is different, the part that is added in the data path contains the index.
    * This method adds the index, is for the data path.
    */
   private String attr_path_multiple(Map parent, String attr, int i, String parent_path)
   {
      return parent_path != '/' ? parent_path +'/'+ attr +'('+ i +')' : '/'+ attr +'('+ i +')'
   }



   // ========= FIll METHODS =========

   private void fillPATHABLE(Pathable p, Pathable parent, String path, String dataPath)
   {
      p.parent   = parent
      p.path     = path
      p.dataPath = dataPath
   }

   private void fillLOCATABLE(Locatable l, Map json, Pathable parent, String path, String dataPath)
   {
      // name can be text or coded
      String type = json.name._type
      if (!type) type = 'DV_TEXT'
      String method = 'parse'+ type

      l.name = this."$method"(json.name)

      l.archetype_node_id = json.archetype_node_id // can be null

      if (json.uid)
      {
         type = json.uid._type
         if (!type)
         {
            throw new JsonParseException("_type required for "+ dataPath +".uid")
         }
         method = 'parse'+ type
         l.uid = this."$method"(json.uid)
      }

      if (json.archetype_details)
         l.archetype_details = this.parseARCHETYPED(json.archetype_details)

      this.fillPATHABLE(l, parent, path, dataPath)
   }

   private void fillENTRY(Entry e, Map json, Pathable parent, String path, String dataPath)
   {
      String type, method

      this.fillLOCATABLE(e, json, parent, path, dataPath)

      e.encoding = this.parseCODE_PHRASE(json.encoding)
      e.language = this.parseCODE_PHRASE(json.language)


      type = json.subject._type
      if (!type)
      {
         throw new JsonParseException("_type required for "+ dataPath +".subject")
      }
      method = 'parse'+ type
      e.subject = this."$method"(json.subject)


      if (json.provider)
      {
         type = json.provider._type
         if (!type)
         {
            throw new JsonParseException("_type required for "+ dataPath +".provider")
         }
         method = 'parse'+ type
         e.provider = this."$method"(json.provider)
      }


      if (json.other_participations)
      {
         def participation
         json.other_participations.each { _participation ->

            participation = this.parsePARTICIPATION(_participation)
            e.otherParticipations.add(participation)
         }
      }


      if (json.workflow_id)
      {
         e.workflowId = this.parseOBJECT_REF(json.workflow_id)
      }
   }

   private void fillCARE_ENTRY(CareEntry c, Map json, Pathable parent, String path, String dataPath)
   {
      if (json.protocol)
      {
         String type = json.protocol._type
         if (!type)
         {
            throw new JsonParseException("_type required for "+ dataPath +".protocol")
         }
         //String path_node_id = (json.protocol.archetype_node_id.startsWith('at') ? json.protocol.archetype_node_id : 'archetype_id='+ json.protocol.archetype_node_id)
         String method = 'parse'+ type
         c.protocol = this."$method"(json.protocol, parent,
                                     //(path != '/' ? path +'/protocol['+ path_node_id +']' : '/protocol['+ path_node_id +']'),
                                     //(dataPath != '/' ? dataPath +'/protocol['+ path_node_id +']' : '/protocol['+ path_node_id +']')
                                     this.attr_archetype_path(json, 'protocol', path),
                                     this.attr_path(json, 'protocol', dataPath)
                                    )
      }

      if (json.guideline_id)
      {
         c.guideline_id = this.parseOBJECT_REF(json.guideline_id)
      }

      this.fillENTRY(c, json, parent, path, dataPath)
   }

   private void fillPARTY(Party p, Map json, Pathable parent, String path, String dataPath)
   {
      this.fillLOCATABLE(p, json, parent, path, dataPath)

      String path_node_id

      if (json.details)
      {
         def type = json.details._type

         if (!type)
         {
            throw new JsonParseException("_type required for "+ dataPath +".details")
         }

         def method = 'parse'+ type

         path_node_id = (json.details.archetype_node_id.startsWith('at') ? json.details.archetype_node_id : 'archetype_id='+ json.details.archetype_node_id)

         this."$method"(json.details, p,
                        (path != '/' ? path +'/details['+ path_node_id +']' : '/details['+ path_node_id +']'),
                        (dataPath != '/' ? dataPath +'/details['+ path_node_id +']' : '/details['+ path_node_id +']')
                       )
      }

      if (json.contacts)
      {
         json.contacts.eachWithIndex { contact, i ->

            path_node_id = (contact.archetype_node_id.startsWith('at') ? contact.archetype_node_id : 'archetype_id='+ contact.archetype_node_id)

            p.contacts << this.parseCONTACT(contact, p,
                        (path != '/' ? path +'/contacts['+ path_node_id +']' : '/contacts['+ path_node_id +']'),
                        (dataPath != '/' ? dataPath +'/contacts['+ path_node_id +']['+ i +']' : '/contacts['+ path_node_id +']['+ i +']'))
         }
      }

      json.identities.eachWithIndex { party_identity, i ->

         path_node_id = (party_identity.archetype_node_id.startsWith('at') ? party_identity.archetype_node_id : 'archetype_id='+ party_identity.archetype_node_id)

         p.identities << this.parsePARTY_IDENTITY(party_identity, p,
                        (path != '/' ? path +'/identities['+ path_node_id +']' : '/identities['+ path_node_id +']'),
                        (dataPath != '/' ? dataPath +'/identities['+ path_node_id +']['+ i +']' : '/identities['+ path_node_id +']['+ i +']'))
      }
   }

   private void fillACTOR(Actor a, Map json, Pathable parent, String path, String dataPath)
   {
      this.fillPARTY(a, json, parent, path, dataPath)

      def type, method

      if (json.languages)
      {
         json.languages.each { dvtext ->

            type = dvtext._type
            if (!type) type = 'DV_TEXT'
            method = 'parse'+ type

            a.languages << this."$method"(json.name)
         }
      }

      // NOTE: if this is an ACTOR DTO, the ROLEs will be included directly not via PARTY_REF
      if (json.roles)
      {
         json.roles.each { party_ref ->

            a.roles << this.parsePARTY_REF(party_ref)
         }
      }
   }

   private void fillPartyDto(PartyDto p, Map json, Pathable parent, String path, String dataPath)
   {
      this.fillLOCATABLE(p, json, parent, path, dataPath)

      String path_node_id

      if (json.details)
      {
         def type = json.details._type

         if (!type)
         {
            throw new JsonParseException("_type required for "+ dataPath +".details")
         }

         def method = 'parse'+ type

         path_node_id = (json.details.archetype_node_id.startsWith('at') ? json.details.archetype_node_id : 'archetype_id='+ json.details.archetype_node_id)

         this."$method"(json.details, p,
                        (path != '/' ? path +'/details['+ path_node_id +']' : '/details['+ path_node_id +']'),
                        (dataPath != '/' ? dataPath +'/details['+ path_node_id +']' : '/details['+ path_node_id +']')
                       )
      }

      if (json.contacts)
      {
         json.contacts.eachWithIndex { contact, i ->

            path_node_id = (contact.archetype_node_id.startsWith('at') ? contact.archetype_node_id : 'archetype_id='+ contact.archetype_node_id)

            p.contacts << this.parseCONTACT(contact, p,
                        (path != '/' ? path +'/contacts['+ path_node_id +']' : '/contacts['+ path_node_id +']'),
                        (dataPath != '/' ? dataPath +'/contacts['+ path_node_id +']['+ i +']' : '/contacts['+ path_node_id +']['+ i +']'))
         }
      }

      json.identities.eachWithIndex { party_identity, i ->

         path_node_id = (party_identity.archetype_node_id.startsWith('at') ? party_identity.archetype_node_id : 'archetype_id='+ party_identity.archetype_node_id)

         p.identities << this.parsePARTY_IDENTITY(party_identity, p,
                        (path != '/' ? path +'/identities['+ path_node_id +']' : '/identities['+ path_node_id +']'),
                        (dataPath != '/' ? dataPath +'/identities['+ path_node_id +']['+ i +']' : '/identities['+ path_node_id +']['+ i +']'))
      }
   }

   private void fillActorDto(ActorDto a, Map json, Pathable parent, String path, String dataPath)
   {
      this.fillPartyDto(a, json, parent, path, dataPath)

      def type, method

      if (json.languages)
      {
         json.languages.each { dvtext ->

            type = dvtext._type
            if (!type) type = 'DV_TEXT'
            method = 'parse'+ type

            a.languages << this."$method"(json.name)
         }
      }

      // NOTE: this ACTOR DTO, the ROLEs are included directly not via PARTY_REF
      if (json.roles)
      {
         json.roles.each { role ->

            a.roles << this.parseROLE(role)
         }
      }
   }

   // ========= PARSE METHODS =========
   private PersonDto parsePersonDto(Map map)
   {
      def person = new PersonDto()

      this.fillActorDto(person, map, null, '/', '/')

      return person
   }

   private Person parsePERSON(Map map)
   {
      def person = new Person()

      this.fillACTOR(person, map, null, '/', '/')

      return person
   }

   private Agent parseAGENT(Map map)
   {
      def agent = new Agent()

      this.fillACTOR(agent, map, null, '/', '/')

      return agent
   }

   private Group parseGROUP(Map map)
   {
      def group = new Group()

      this.fillACTOR(group, map, null, '/', '/')

      return group
   }

   private Organization parseORGANISATION(Map map)
   {
      def organization = new Organization()

      this.fillACTOR(organization, map, null, '/', '/')

      return organization
   }

   private Role parseROLE(Map map)
   {
      def role = new Role()

      this.fillPARTY(role, map, null, '/', '/')

      if (map.time_validity)
      {
         role.time_validity = this.parseDV_INTERVAL(map.time_validity)
      }

      role.performer = this.parsePARTY_REF(map.performer)

      if (map.capabilities)
      {
         role.capabilities = []
         map.capabilities.eachWithIndex { capability, i ->
            role.capabilities << this.parseCAPABILITY(capability, role, '/capabilities', "/capabilities($i)")
         }
      }

      return role
   }


   private Contact parseCONTACT(Map map, Pathable parent, String path, String dataPath)
   {
      def contact = new Contact()

      this.fillLOCATABLE(contact, map, parent)

      if (map.time_validity)
      {
         contact.time_validity = this.parseDV_INTERVAL(map.time_validity)
      }

      if (map.addresses)
      {
         contact.addresses = []
         map.addresses.each { address ->
            contact.addresses << this.parseADDRESS(address, contact,
               this.multi_attr_archetype_path(map, 'addresses', i, path),
               this.attr_path_multiple(map, 'addresses', i, dataPath)
            )
         }
      }

      return contact
   }

   private Address parseADDRESS(Map map, Pathable parent, String path, String dataPath)
   {
      def address = new Address()

      this.fillLOCATABLE(address, map, parent)

      if (map.details)
      {
         String method = 'parse'+ map.details._type
         address.details = this."$method"(
            map.details, address,
            this.attr_archetype_path(map, 'details', path),
            this.attr_path(map, 'details', dataPath)
         )
      }

      return address
   }

   private Capability parseCAPABILITY(Map map, Pathable parent, String path, String dataPath)
   {
      def capability = new Capability()

      this.fillLOCATABLE(capability, map, parent, path, dataPath)

      return capability
   }

   private PartyIdentity parsePARTY_IDENTITY(Map map, Pathable parent, String path, String dataPath)
   {
      def pi = new PartyIdentity()

      this.fillLOCATABLE(pi, map, parent, path, dataPath)

      String path_node_id

      if (map.details)
      {
         def type = map.details._type

         if (!type)
         {
            throw new JsonParseException("_type required for "+ dataPath +".details")
         }

         def method = 'parse'+ type

         path_node_id = (map.details.archetype_node_id.startsWith('at') ? map.details.archetype_node_id : 'archetype_id='+ map.details.archetype_node_id)

         this."$method"(map.details, pi,
                        (path != '/' ? path +'/details['+ path_node_id +']' : '/details['+ path_node_id +']'),
                        (dataPath != '/' ? dataPath +'/details['+ path_node_id +']' : '/details['+ path_node_id +']')
                       )
      }

      return pi
   }

   // NOTE: parseEHR_STATUS is a top level class so it doesn't have a Pathable parent and the path is /
   private EhrStatus parseEHR_STATUS(Map map)
   {
      def status = new EhrStatus()

      this.fillLOCATABLE(status, map, null, '/', '/')

      // subject is mandatory, is the ref inside that might be optional
      // if (map.subject)
      // {
         status.subject = this.parsePARTY_SELF(map.subject)
      // }

      status.is_modifiable = map.is_modifiable
      status.is_queryable = map.is_queryable

      if (map.other_details)
      {
         String path_node_id = (map.other_details.archetype_node_id.startsWith('at') ? map.other_details.archetype_node_id : 'archetype_id='+ map.other_details.archetype_node_id)

         String method = 'parse'+ map.other_details._type
         status.other_details = this."$method"(map.other_details, status, '/other_details['+ path_node_id +']', '/other_details['+ path_node_id +']')
      }

      return status
   }


   // this is not used anymore
   // private EhrStatus parseEHR_STATUS(Map json, Pathable parent, String path, String dataPath)
   // {
   //    def status = new EhrStatus()

   //    this.fillLOCATABLE(status, json, parent, path, dataPath)

   //    if (json.subject)
   //    {
   //       status.subject = this.parsePARTY_SELF(json.subject)
   //    }

   //    status.is_modifiable = json.is_modifiable
   //    status.is_queryable = json.is_queryable

   //    if (json.other_details)
   //    {
   //       String method = 'parse'+ json.other_details._type

   //       status.other_details = this."$method"(
   //          json.other_details, status,
   //          this.attr_archetype_path(json, 'other_details', '/'),
   //          this.attr_path(json, 'other_details', '/')
   //       )
   //    }

   //    return status
   // }



   /**
    * This method is here for completeness, most implementations don't even have support for EHR_ACCESS internally.
    */
   private EhrAccess parseEHR_ACCESS(Map map)
   {
      def access = new EhrAccess()

      this.fillLOCATABLE(access, map, null, '/', '/')

      if (map.settings)
      {
         log.warn("EHR_ACCESS.settings has a value but was not parsed")
         // Since ACCESS_CONTROL_SETTINGS is abstract and doesn't have any concrete
         // subclasses, we can't parse it.
         //access.settings = this.parseACCESS_CONTROL_SETTINGS(map.access)
      }

      return access
   }

   private Composition parseCOMPOSITION(Map json)
   {
      Composition compo = new Composition()

      this.fillLOCATABLE(compo, json, null, '/', '/')

      compo.language  = this.parseCODE_PHRASE(json.language)
      compo.territory = this.parseCODE_PHRASE(json.territory)
      compo.category  = this.parseDV_CODED_TEXT(json.category)

      String type, method

      type = json.composer._type // party proxy or descendants
      if (!type)
      {
         throw new JsonParseException("_type required for /composer")
      }

      method = 'parse'+ type
      compo.composer = this."$method"(json.composer)

      compo.context = parseEVENT_CONTEXT(
         json.context, compo,
         this.attr_archetype_path(json, 'context', '/'),
         this.attr_path(json, 'context', '/')
      )

      def content = []

      json.content.eachWithIndex { content_item, i ->
         type = content_item._type
         if (!type)
         {
            throw new JsonParseException("_type required for /content[$i]")
         }
         method = 'parse'+ type

         //if (!compo.content) compo.content = []

         compo.content.add(
            this."$method"(
               content_item, compo,
               this.multi_attr_archetype_path(json, 'content', i, '/'),
               this.attr_path_multiple(json, 'content', i, '/')
            )
         )
      }

      return compo
   }

   // This will parse the top level directory that doesn't have a LOCATABLE parent
   private Folder parseFOLDER(Map json)
   {
      def folder = new Folder()

      this.fillLOCATABLE(folder, json, null, '/', '/')

      if (json.items)
      {
         folder.items = []
         json.items.eachWithIndex { item, i ->
            folder.items << this.parseOBJECT_REF(item)
         }
      }

      if (json.folders)
      {
         folder.folders = []
         json.folders.eachWithIndex { subfolder, i ->

            folder.folders << this.parseFOLDER(
               subfolder, folder,
               this.multi_attr_archetype_path(json, 'folders', i, '/'),
               this.attr_path_multiple(json, 'folders', i, '/')
            )
         }
      }

      return folder
   }

   // This will parse subfolders with a parent Locatable folder
   private Folder parseFOLDER(Map json, Locatable parent, String path, String dataPath)
   {
      def folder = new Folder()

      this.fillLOCATABLE(folder, json, parent, path, dataPath)

      if (json.items)
      {
         folder.items = []
         json.items.eachWithIndex { item, i ->
            folder.items << this.parseOBJECT_REF(item)
         }
      }

      if (json.folders)
      {
         folder.folders = []
         json.folders.eachWithIndex { subfolder, i ->

            folder.folders << this.parseFOLDER(
               subfolder, folder,
               this.multi_attr_archetype_path(json, 'folders', i, path),
               this.attr_path_multiple(json, 'folders', i, dataPath)
            )
         }
      }

      return folder
   }

   // NOTE: uid is mandatory for RM but optional for API, so this parser doesn't require the uid to be able to parse for API too
   private OriginalVersion parseORIGINAL_VERSION(Map json)
   {
      OriginalVersion ov = new OriginalVersion()

      if (json.uid)
      {
         ov.uid = this.parseOBJECT_VERSION_ID(json.uid)
      }

      if (json.signature)
      {
         ov.signature = json.signature
      }

      if (json.preceding_version_uid)
      {
         ov.preceding_version_uid = this.parseOBJECT_VERSION_ID(json.preceding_version_uid)
      }

      // TODO: other_input_version_uids

      ov.lifecycle_state = this.parseDV_CODED_TEXT(json.lifecycle_state)

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
         if (!type)
         {
            throw new JsonParseException("_type required for "+ dataPath +".data")
         }
         def method = 'parse'+ type
         ov.data = this."$method"(json.data)
      }

      return ov
   }

   private AuditDetails parseAUDIT_DETAILS(Map json)
   {
      AuditDetails ad = new AuditDetails()

      ad.system_id   = json.system_id
      ad.change_type = this.parseDV_CODED_TEXT(json.change_type)

      // NOTE: for API DTOs the time_committed is optional, so it will be set by the server
      //       we make it optional here to support both RM and API flavors
      if (json.time_committed)
      {
         ad.time_committed = this.parseDV_DATE_TIME(json.time_committed)
      }

      if (json.description)
      {
         ad.description = this.parseDV_TEXT(json.description)
      }

      def type = json.committer._type
      if (!type)
      {
         throw new JsonParseException("_type required for "+ dataPath +".committer")
      }
      def method = 'parse'+ type
      ad.committer = this."$method"(json.committer)

      return ad
   }

   private Attestation parseATTESTATION(Map json)
   {
      Attestation at = new Attestation()

      // AuditDetails fields
      at.system_id   = json.system_id
      at.change_type = this.parseDV_CODED_TEXT(json.change_type)

      // NOTE: for API DTOs the time_committed is optional, so it will be set by the server
      //       we make it optional here to support both RM and API flavors
      if (json.time_committed)
      {
         at.time_committed = this.parseDV_DATE_TIME(json.time_committed)
      }

      if (json.description)
      {
         at.description = this.parseDV_TEXT(json.description)
      }

      def type = json.committer._type
      if (!type)
      {
         throw new JsonParseException("_type required for "+ dataPath +".committer")
      }
      def method = 'parse'+ type
      at.committer = this."$method"(json.committer)

      // Attestation fields
      if (json.attested_view)
      {
         at.attested_view = this.parseDV_MULTIMEDIA(json.attested_view)
      }

      if (json.proof)
      {
         at.proof = json.proof
      }

      // TODO: json.items

      type = json.reason._type // text or coded
      if (!type)
      {
         throw new JsonParseException("_type required for "+ dataPath +".reason")
      }
      method = 'parse'+ type
      at.reason = this."$method"(json.reason)

      // TODO: test if this is parsed as a boolean or as a string
      at.is_pending = json.is_pending.toBoolean()

      return at
   }

   private ReferenceRange parseREFERENCE_RANGE(Map json)
   {
      ReferenceRange rr = new ReferenceRange()

      rr.meaning = this.parseDV_TEXT(json.meaning)

      rr.range = this.parseDV_INTERVAL(json.range)

      return rr
   }

   private void fillDV_ORDERED(DvOrdered d, Map json)
   {
      if (json.normal_status)
      {
         d.normal_status = this.parseCODE_PHRASE(json.normal_status)
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

   private ArchetypeId parseARCHETYPE_ID(Map json)
   {
      new ArchetypeId(value: json.value)
   }

   private TemplateId parseTEMPLATE_ID(Map json)
   {
      new TemplateId(value: json.value)
   }

   private Archetyped parseARCHETYPED(Map json)
   {
      Archetyped a = new Archetyped()
      a.archetype_id = this.parseARCHETYPE_ID(json.archetype_id)
      a.template_id = this.parseTEMPLATE_ID(json.template_id)
      a.rm_version = json.rm_version
      return a
   }

   private PartySelf parsePARTY_SELF(Map json)
   {
      PartySelf p = new PartySelf()

      if (json.external_ref)
      {
         p.external_ref = this.parsePARTY_REF(json.external_ref)
      }

      return p
   }

   private PartyIdentified parsePARTY_IDENTIFIED(Map json)
   {
      PartyIdentified p = new PartyIdentified()

      if (json.name)
      {
         p.name = json.name
      }

      json.identifiers.each { identifier ->

         p.identifiers.add(this.parseDV_IDENTIFIER(identifier))
      }

      if (json.external_ref)
      {
         p.external_ref = this.parsePARTY_REF(json.external_ref)
      }

      return p
   }

   private PartyRelated parsePARTY_RELATED(Map json)
   {
      PartyRelated p = new PartyRelated()

      if (json.name)
      {
         p.name = json.name
      }

      json.identifiers.each { identifier ->

         p.identifiers.add(this.parseDV_IDENTIFIER(identifier))
      }

      if (json.external_ref)
      {
         p.external_ref = this.parsePARTY_REF(json.external_ref)
      }

      if (json.relationship)
      {
         p.relationship = this.parseDV_CODED_TEXT(json.relationship)
      }

      return p
   }


   private ObjectRef parseOBJECT_REF(Map json)
   {
      ObjectRef o   = new ObjectRef()

      o.namespace   = json.namespace
      o.type        = json.type

      String type   = json.id._type
      if (!type)
      {
         throw new JsonParseException("_type required for OBJECT_REF.id")
      }
      String method = 'parse'+ type
      o.id          = this."$method"(json.id)

      return o
   }

   private PartyRef parsePARTY_REF(Map json)
   {
      PartyRef p = new PartyRef()

      p.namespace = json.namespace
      p.type = json.type

      String type = json.id._type
      if (!type)
      {
         throw new JsonParseException("_type required for PARTY_REF.id")
      }
      String method = 'parse'+ type
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
      if (!type)
      {
         throw new JsonParseException("_type required for LOCATABLE_REF.id")
      }
      String method = 'parse'+ type
      o.id = this."$method"(json.id)

      return o
   }


   private DvIdentifier parseDV_IDENTIFIER(Map json)
   {
      DvIdentifier i = new DvIdentifier()

      i.issuer = json.issuer
      i.assigner = json.assigner
      i.id = json.id
      i.type = json.type

      return i
   }

   private EventContext parseEVENT_CONTEXT(Map json, Pathable parent, String path, String dataPath)
   {
      EventContext e = new EventContext()

      this.fillPATHABLE(e, parent, path, dataPath)

      e.start_time = this.parseDV_DATE_TIME(json.start_time)

      if (e.end_time)
         e.end_time = this.parseDV_DATE_TIME(json.end_time)

      e.location = json.location

      e.setting = this.parseDV_CODED_TEXT(json.setting)

      if (json.other_context)
      {
         String type, method

         type = json.other_context._type

         if (!type)
         {
            throw new JsonParseException("_type required for "+ dataPath +".other_context")
         }

         method = 'parse'+ type

         e.other_context = this."$method"(
            json.other_context, e,
            this.attr_archetype_path(json, 'other_context', path),
            this.attr_path(json, 'other_context', dataPath)
         )
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

      p.function = this.parseDV_TEXT(json.function)

      if (json.time)
      {
         p.time = this.parseDV_INTERVAL(json.time)
      }

      p.mode = this.parseDV_CODED_TEXT(json.mode)

      String type = json.performer._type
      if (!type)
      {
         throw new JsonParseException("_type required for PARTICIPATION.performer")
      }
      String method = 'parse'+ type
      p.performer = this."$method"(json.performer)

      return p
   }

   private Section parseSECTION(Map json, Pathable parent, String path, String dataPath)
   {
      Section section = new Section()

      this.fillLOCATABLE(section, json, parent, path, dataPath)

      String type, method

      json.items.eachWithIndex { content_item, i ->

         type = content_item._type

         if (!type)
         {
            throw new JsonParseException("_type required for "+ dataPath +".items[$i]")
         }

         method = 'parse'+ type

         //if (!section.items) section.items = []

         section.items.add(
            this."$method"(
               content_item, section,
               this.multi_attr_archetype_path(json, 'items', i, path),
               this.attr_path_multiple(json, 'items', i, dataPath)
            )
         )
      }

      return section
   }

   private AdminEntry parseADMIN_ENTRY(Map json, Pathable parent, String path, String dataPath)
   {
      AdminEntry a = new AdminEntry()

      this.fillENTRY(a, json, parent, path, dataPath)

      String type = json.data._type

      if (!type)
      {
         throw new JsonParseException("_type required for "+ dataPath +".data")
      }

      String method = 'parse'+ type

      a.data = this."$method"(
         json.data, a,
         this.attr_archetype_path(json, 'data', path),
         this.attr_path(json, 'data', dataPath)
      )

      return a
   }

   private Observation parseOBSERVATION(Map json, Pathable parent, String path, String dataPath)
   {
      Observation o = new Observation()

      this.fillCARE_ENTRY(o, json, parent, path, dataPath)

      if (json.data)
      {
         o.data = this.parseHISTORY(
            json.data, o,
            this.attr_archetype_path(json, 'data', path),
            this.attr_path(json, 'data', dataPath)
         )
      }

      if (json.state)
      {
         o.state = this.parseHISTORY(
            json.state, o,
            this.attr_archetype_path(json, 'state', path),
            this.attr_path(json, 'state', dataPath)
         )
      }

      return o
   }

   private History parseHISTORY(Map json, Pathable parent, String path, String dataPath)
   {
      History h = new History()

      this.fillLOCATABLE(h, json, parent, path, dataPath)

      h.origin = this.parseDV_DATE_TIME(json.origin)

      if (json.period)
      {
         h.period = this.parseDV_DURATION(json.period)
      }

      if (json.duration)
      {
         h.duration = this.parseDV_DURATION(json.duration)
      }

      String type, method

      json.events.eachWithIndex { event, i ->

         type = event._type
         method = 'parse'+ type

         //if (!h.events) h.events = []

         h.events.add(
            this."$method"(
               event, h,
               this.multi_attr_archetype_path(json, 'events', i, path),
               this.attr_path_multiple(json, 'events', i, dataPath)
            )
         )
      }

      return h
   }

   private PointEvent parsePOINT_EVENT(Map json, Pathable parent, String path, String dataPath)
   {
      PointEvent e = new PointEvent()

      this.fillLOCATABLE(e, json, parent, path, dataPath)

      e.time = this.parseDV_DATE_TIME(json.time)

      String type, method

      if (json.data)
      {
         type = json.data._type
         if (!type)
         {
            throw new JsonParseException("_type required for "+ dataPath +".data")
         }
         method = 'parse'+ type

         e.data = this."$method"(
            json.data, e,
            this.attr_archetype_path(json, 'data', path),
            this.attr_path(json, 'data', dataPath)
         )
      }

      if (json.state)
      {
         type = json.state._type
         if (!type)
         {
            throw new JsonParseException("_type required for "+ dataPath +".state")
         }
         method = 'parse'+ type

         e.state = this."$method"(
            json.state, e,
            this.attr_archetype_path(json, 'state', path),
            this.attr_path(json, 'state', dataPath)
         )
      }

      return e
   }

   private IntervalEvent parseINTERVAL_EVENT(Map json, Pathable parent, String path, String dataPath)
   {
      IntervalEvent e = new IntervalEvent()

      this.fillLOCATABLE(e, json, parent, path, dataPath)

      e.time = this.parseDV_DATE_TIME(json.time)

      String type, method

      if (json.data)
      {
         type = json.data._type
         if (!type)
         {
            throw new JsonParseException("_type required for "+ dataPath +".data")
         }
         method = 'parse'+ type

         e.data = this."$method"(
            json.data, e,
            this.attr_archetype_path(json, 'data', path),
            this.attr_path(json, 'data', dataPath)
         )
      }

      if (json.state)
      {
         type = json.state._type
         if (!type)
         {
            throw new JsonParseException("_type required for "+ dataPath +".state")
         }
         method = 'parse'+ type

         e.state = this."$method"(
            json.state, e,
            this.attr_archetype_path(json, 'state', path),
            this.attr_path(json, 'state', dataPath)
         )
      }

      e.width = this.parseDV_DURATION(json.width)

      e.math_function = this.parseDV_CODED_TEXT(json.math_function)

      if (json.sample_count != null)
      {
         e.sample_count = json.sample_count
      }

      return e
   }

   private Evaluation parseEVALUATION(Map json, Pathable parent, String path, String dataPath)
   {
      Evaluation e = new Evaluation()

      this.fillCARE_ENTRY(e, json, parent, path, dataPath)

      String type = json.data._type
      if (!type)
      {
         throw new JsonParseException("_type required for "+ dataPath +".data")
      }
      String method = 'parse'+ type

      e.data = this."$method"(
         json.data, e,
         this.attr_archetype_path(json, 'data', path),
         this.attr_path(json, 'data', dataPath)
      )

      return e
   }

   private Instruction parseINSTRUCTION(Map json, Pathable parent, String path, String dataPath)
   {
      Instruction ins = new Instruction()

      this.fillCARE_ENTRY(ins, json, parent, path, dataPath)

      String type, method


      type = json.narrative._type
      if (!type) type = 'DV_TEXT'
      method = 'parse'+ type
      ins.narrative = this."$method"(json.narrative)


      if (json.expiry_time)
         ins.expiry_time = this.parseDV_DATE_TIME(json.expiry_time)


      if (json.wf_definition)
         ins.wf_definition = this.parseDV_PARSABLE(json.wf_definition)


      json.activities.eachWithIndex { js_activity, i ->

         if (!ins.activities) ins.activities = []

         ins.activities.add(
            this.parseACTIVITY(
               js_activity, ins,
               this.multi_attr_archetype_path(json, 'activities', i, path),
               this.attr_path_multiple(json, 'activities', i, dataPath)
            )
         )
      }

      return ins
   }

   private Action parseACTION(Map json, Pathable parent, String path, String dataPath)
   {
      Action a = new Action()

      this.fillCARE_ENTRY(a, json, parent, path, dataPath)

      String type = json.description._type
      if (!type)
      {
         throw new JsonParseException("_type required for "+ dataPath +".description")
      }
      String method = 'parse'+ type

      a.description = this."$method"(
         json.description, a,
         this.attr_archetype_path(json, 'description', path),
         this.attr_path(json, 'description', dataPath)
      )

      a.time = this.parseDV_DATE_TIME(json.time)

      a.ism_transition = this.parseISM_TRANSITION(
         json.ism_transition, a,
         this.attr_archetype_path(json, 'ism_transition', path),
         this.attr_path(json, 'ism_transition', dataPath)
      )

      if (json.instruction_details)
      {
         a.instruction_details = this.parseINSTRUCTION_DETAILS(
            json.instruction_details, a,
            this.attr_archetype_path(json, 'instruction_details', path),
            this.attr_path(json, 'instruction_details', dataPath)
         )
      }

      return a
   }

   private IsmTransition parseISM_TRANSITION(Map json, Pathable parent, String path, String dataPath)
   {
      IsmTransition i = new IsmTransition()

      this.fillPATHABLE(i, parent, path, dataPath)

      i.current_state = this.parseDV_CODED_TEXT(json.current_state)

      if (json.transition)
      {
         i.transition = this.parseDV_CODED_TEXT(json.transition)
      }

      if (json.careflow_step)
      {
         i.careflow_step = this.parseDV_CODED_TEXT(json.careflow_step)
      }

      return i
   }

   private InstructionDetails parseINSTRUCTION_DETAILS(Map json, Pathable parent, String path, String dataPath)
   {
      InstructionDetails i = new InstructionDetails()

      this.fillPATHABLE(i, parent, path, dataPath)

      i.instruction_id = this.parseLOCATABLE_REF(json.instruction_id)

      i.activity_id = json.activity_id

      if (json.wf_details)
      {
         String type = json.wf_details._type
         if (!type)
         {
            throw new JsonParseException("_type required for "+ dataPath +".wf_details")
         }
         String method = 'parse'+ type
         i.wf_details = this."$method"(json.wf_details)
      }

      return i
   }

   private Activity parseACTIVITY(Map json, Pathable parent, String path, String dataPath)
   {
      String type = json.description._type
      if (!type)
      {
         throw new JsonParseException("_type required for "+ dataPath +".description")
      }
      String method = 'parse'+ type

      Activity a = new Activity(
         action_archetype_id: json.action_archetype_id
      )

      a.description = this."$method"(
         json.description, a,
         this.attr_archetype_path(json, 'description', path),
         this.attr_path(json, 'description', dataPath)
      )

      if (json.timing)
      {
         a.timing = this.parseDV_PARSABLE(json.timing)
      }

      this.fillLOCATABLE(a, json, parent, path, dataPath)

      return a
   }


   private ItemTree parseITEM_TREE(Map json, Pathable parent, String path, String dataPath)
   {
      ItemTree t = new ItemTree()

      this.fillLOCATABLE(t, json, parent, path, dataPath)

      String type, method

      json.items.eachWithIndex { item, i ->

         type = item._type
         if (!type)
         {
            throw new JsonParseException("_type required for "+ dataPath +".items[$i]")
         }
         method = 'parse'+ type

         if (!t.items) t.items = []

         t.items.add(
            this."$method"(
               item, t,
               this.multi_attr_archetype_path(json, 'items', i, path),
               this.attr_path_multiple(json, 'items', i, dataPath)
            )
         )
      }

      return t
   }

   private ItemList parseITEM_LIST(Map json, Pathable parent, String path, String dataPath)
   {
      ItemList l = new ItemList()

      this.fillLOCATABLE(l, json, parent, path, dataPath)

      json.items.eachWithIndex { element, i ->

         if (!l.items) l.items = []

         l.items.add(
            this.parseELEMENT(
               element, l,
               this.multi_attr_archetype_path(json, 'items', i, path),
               this.attr_path_multiple(json, 'items', i, dataPath)
            )
         )
      }

      return l
   }

   private ItemTable parseITEM_TABLE(Map json, Pathable parent, String path, String dataPath)
   {
      ItemTable t = new ItemTable()

      this.fillLOCATABLE(t, json, parent, path, dataPath)

      String type, method

      // FIXME: rows are CLUSTERS, we don't need to get the dynamic method
      json.rows.each { item ->
         type = item._type
         method = 'parse'+ type

         if (!t.rows) t.rows = []

         t.rows.add(
            this."$method"(
               item, t,
               this.multi_attr_archetype_path(json, 'rows', i, path),
               this.attr_path_multiple(json, 'rows', i, dataPath)
            )
         )
      }

      return t
   }

   private ItemSingle parseITEM_SINGLE(Map json, Pathable parent, String path, String dataPath)
   {
      ItemSingle s = new ItemSingle()

      this.fillLOCATABLE(s, json, parent, path, dataPath)

      s.item = this.parseELEMENT(
         json.item, s,
         this.attr_archetype_path(json, 'item', path),
         this.attr_path(json, 'item', dataPath)
      )

      return s
   }

   private Cluster parseCLUSTER(Map json, Pathable parent, String path, String dataPath)
   {
      Cluster c = new Cluster()

      this.fillLOCATABLE(c, json, parent, path, dataPath)

      String type, method

      json.items.eachWithIndex { item, i ->
         type = item._type
         if (!type)
         {
            throw new JsonParseException("_type required for "+ dataPath +".items[$i]")
         }
         method = 'parse'+ type

         if (!c.items) c.items = []

         c.items.add(
            this."$method"(
               item, c,
               this.multi_attr_archetype_path(json, 'items', i, path),
               this.attr_path_multiple(json, 'items', i, dataPath)
            )
         )
      }

      return c
   }

   private Element parseELEMENT(Map json, Pathable parent, String path, String dataPath)
   {
      Element e = new Element()

      this.fillLOCATABLE(e, json, parent, path, dataPath)

      if (json.value)
      {
         String type = json.value._type
         if (!type)
         {
            throw new JsonParseException("_type required for "+ dataPath +".value")
         }
         String method = 'parse'+ type
         e.value = this."$method"(json.value)
      }

      if (json.null_flavour)
      {
         e.null_flavour = this.parseDV_CODED_TEXT(json.null_flavour)
      }

      return e
   }



   private TerminologyId parseTERMINOLOGY_ID(Map json)
   {
      new TerminologyId(
         value: json.value
      )
   }

   private GenericId parseGENERIC_ID(Map json)
   {
      new GenericId(
         scheme: json.scheme,
         value: json.value
      )
   }

   private CodePhrase parseCODE_PHRASE(Map json)
   {
      new CodePhrase(
         codeString: json.code_string,
         terminologyId: this.parseTERMINOLOGY_ID(json.terminology_id)
      )
   }

   private HierObjectId parseHIER_OBJECT_ID(Map json)
   {
      new HierObjectId(
         value: json.value
      )
   }

   private ObjectVersionId parseOBJECT_VERSION_ID(Map json)
   {
      new ObjectVersionId(
         value: json.value
      )
   }

   private VersionTreeId parseVERSION_TREE_ID(Map json)
   {
      new VersionTreeId(
         value: json.value
      )
   }


   private DvText parseDV_TEXT(Map json)
   {
      new DvText(value: json.value)
   }

   private DvCodedText parseDV_CODED_TEXT(Map json)
   {
      new DvCodedText(
         value: json.value,
         defining_code: this.parseCODE_PHRASE(json.defining_code)
      )
   }

   private TermMapping parseTERM_MAPPING(Map json)
   {
      new TermMapping(
         match: json.match,
         purpose: this.parseDV_CODED_TEXT(json.purpose),
         target: this.parseCODE_PHRASE(json.target)
      )
   }

   private DvDateTime parseDV_DATE_TIME(Map json)
   {
      // TODO: DvAbsoluteQuantity
      new DvDateTime(value: json.value)
   }

   private DvDate parseDV_DATE(Map json)
   {
      // TODO: DvAbsoluteQuantity
      new DvDate(value: json.value)
   }

   private DvTime parseDV_TIME(Map json)
   {
      // TODO: DvAbsoluteQuantity
      new DvTime(value: json.value)
   }

   private DvDuration parseDV_DURATION(Map json)
   {
      DvDuration d = new DvDuration()

      d.value = json.value

      this.fillDV_AMOUNT(d, json)

      return d
   }

   private DvQuantity parseDV_QUANTITY(Map json)
   {
      DvQuantity q = new DvQuantity()

      q.magnitude = json.magnitude

      q.units = json.units

      q.precision = json.precision

      this.fillDV_AMOUNT(q, json)

      return q
   }

   private DvCount parseDV_COUNT(Map json)
   {
      DvCount c = new DvCount()

      c.magnitude = json.magnitude

      this.fillDV_AMOUNT(c, json)

      return c
   }

   private DvProportion parseDV_PROPORTION(Map json)
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

   private DvOrdinal parseDV_ORDINAL(Map json)
   {
      DvOrdinal d = new DvOrdinal(
         value: json.value,
         symbol: this.parseDV_CODED_TEXT(json.symbol)
      )

      this.fillDV_ORDERED(d, json)

      return d
   }

   private DvParsable parseDV_PARSABLE(Map json)
   {
      DvParsable p = new DvParsable(
         value: json.value,
         formalism: json.formalism,
         size: json.value.size()
      )

      if (json.charset)
      {
         p.charset = this.parseCODE_PHRASE(json.charset)
      }

      if (json.language)
      {
         p.language = this.parseCODE_PHRASE(json.language)
      }

      return p
   }

   private DvMultimedia parseDV_MULTIMEDIA(Map json)
   {
      DvMultimedia d = new DvMultimedia()

      if (json.charset)
      {
         p.charset = this.parseCODE_PHRASE(json.charset)
      }

      if (json.language)
      {
         p.language = this.parseCODE_PHRASE(json.language)
      }

      d.alternate_text = json.alternate_text

      if (json.uri)
      {
         d.uri = this.parseDV_URI(json.uri)
      }

      d.data = json.data.getBytes()

      d.media_type = this.parseCODE_PHRASE(json.media_type)

      if (json.compression_algorithm)
      {
         d.compression_algorithm = this.parseCODE_PHRASE(json.compression_algorithm)
      }

      d.size = json.size

      // TODO: integrity_check, integrity_check_algorithm, thumbnail

      return d
   }

   private DvUri parseDV_URI(Map json)
   {
      new DvUri(
         value: json.value
      )
   }

   private DvEhrUri parseDV_EHR_URI(Map json)
   {
      new DvEhrUri(
         value: json.value
      )
   }

   private DvBoolean parseDV_BOOLEAN(Map json)
   {
      new DvBoolean(
         value: json.value
      )
   }


   private DvInterval parseDV_INTERVAL(Map json)
   {
      DvInterval i = new DvInterval()

      String type, method

      // if there is no type, there is no lower or upper
      type = json.lower ? json.lower._type : json.upper._type

      method = 'parse'+ type

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
