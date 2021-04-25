package com.cabolabs.openehr.opt.instance_validation

import com.networknt.schema.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

class JsonInstanceValidation {

   def schema

   JsonInstanceValidation(String uri)
   {
      this.schema = getJsonSchemaFromUrl(uri)
   }

   JsonSchema getJsonSchemaFromUrl(String uri) throws URISyntaxException
   {
      JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
      return factory.getSchema(new URI(uri));
   }

   //java.util.LinkedHashSet
   /*List<ValidationMessage>*/ def validate(JsonNode json)
   {
      this.schema.validate(json)
   }
}