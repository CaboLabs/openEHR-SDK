package com.cabolabs.openehr.formats

import groovy.util.GroovyTestCase
import com.cabolabs.openehr.formats.OpenEhrJsonSerializer

import com.cabolabs.openehr.rm_1_0_2.composition.Composition

import groovy.json.*

import com.cedarsoftware.util.io.JsonWriter

class FlatMapSerializerTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")

   void testFlatMap()
   {
      String path = PS +"canonical_json"+ PS +"lab_results.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParser()
      Composition c = (Composition)parser.parseJson(json)

      def flat = new FlatMapSerializer()
      flat.serialize(c)

      println flat.getSerializedString('json', true)

      //def out = JsonWriter.objectToJson(c.content, [(JsonWriter.PRETTY_PRINT): true])
      //println out
   }
}