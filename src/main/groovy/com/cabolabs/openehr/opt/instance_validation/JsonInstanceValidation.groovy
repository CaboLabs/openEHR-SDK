package com.cabolabs.openehr.opt.instance_validation

import com.networknt.schema.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

class JsonInstanceValidation {

   def schema

   // constructor using local schema copied from
   // https://gist.githubusercontent.com/pieterbos/81651d2d7a5041a130ecb21b0a852e39/raw/2f31b9c7067bccf192256358da868ee8fbc7239a/OpenEHR%2520RM%2520json%2520schema,%2520with%2520default%2520instances%2520of%2520objects%2520addedcorrectly.json
   JsonInstanceValidation()
   {
      JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
      InputStream ins = Thread.currentThread().getContextClassLoader().getResourceAsStream('json_schema/openehr_schema.json')
      this.schema = factory.getSchema(ins)
   }

   // constructor providing external schema
   JsonInstanceValidation(String uri)
   {
      JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
      this.schema = factory.getSchema(new URI(uri))
   }

   // validate parsed json
   // https://javadoc.io/doc/com.networknt/json-schema-validator/1.0.51/com/networknt/schema/ValidationMessage.html
   Set<ValidationMessage> validate(JsonNode json)
   {
      this.schema.validate(json)
   }

   // validate json file contents
   Set<ValidationMessage> validate(String jsonContents)
   {
      ObjectMapper mapper = new ObjectMapper()
      JsonNode json = mapper.readTree(jsonContents)
      this.schema.validate(json)
   }
}