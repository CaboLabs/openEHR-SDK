package com.cabolabs.openehr.formats

import groovy.util.GroovyTestCase
import com.cabolabs.openehr.formats.OpenEhrJsonSerializer

import com.cabolabs.openehr.rm_1_0_2.composition.Composition

import groovy.json.*

import com.cedarsoftware.util.io.JsonWriter

import com.cabolabs.openehr.comms.protobuf.OpenEhrProtobufMessage
import java.io.FileOutputStream

import com.google.protobuf.Value

class FlatMapSerializerTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")

   void testFlatMap()
   {
      String path = PS +"canonical_json"+ PS +"lab_results_simplified.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParser()
      Composition c = (Composition)parser.parseJson(json)

      // 0. transform openehr_rm_object to openehr_flat_map

      def flat = new FlatMapSerializer()
      flat.serialize(c)

      // 1. transform openehr_flat_map to openehr_protobuf_message

      //println flat.getSerializedString('json', true)

      def msg_map = [:]
      flat.getSerializedMap().each { k, v ->
         //println k
         switch (v)
         {
            case {it instanceof Number}:
               msg_map[k] = Value.newBuilder().setNumberValue(v).build()
            break
            case {it instanceof String}:
               msg_map[k] = Value.newBuilder().setStringValue(v).build()
            break
            case {it instanceof Boolean}:
               msg_map[k] = Value.newBuilder().setBoolValue(v).build()
            break
            default:
               println "can't map ${v} ${v.getClass()}"
         }
      }

      OpenEhrProtobufMessage msg = OpenEhrProtobufMessage.newBuilder()
         .putAllFields(msg_map)
         .build()

      try
      {
         //FileOutputStream output = new FileOutputStream('proto.out')
         //msg.writeTo(output)
         //output.close()

         // 2. transform openehr_protobuf_message to byte[]

         // encoded message in stream
         java.io.ByteArrayOutputStream out = new ByteArrayOutputStream()
         msg.writeTo(out)

         // 3. transform byte[] to openehr_protobuf_message

         // decode
         java.io.ByteArrayInputStream instrm = new ByteArrayInputStream(out.toByteArray())
         byte[] encoded = instrm.getBytes()

         OpenEhrProtobufMessage inmsg = OpenEhrProtobufMessage.parseFrom(encoded)

         assert inmsg.getFieldsCount() == flat.getSerializedMap().size()

         inmsg.getFieldsMap().each { decoded_path, pf_value ->

            //println decoded_path
            // switch (pf_value.getKindCase())
            // {
            //    case com.google.protobuf.Value.KindCase.NUMBER_VALUE:
            //       println pf_value.getNumberValue()
            //    break
            //    case com.google.protobuf.Value.KindCase.STRING_VALUE:
            //       println pf_value.getStringValue()
            //    break
            //    case com.google.protobuf.Value.KindCase.BOOL_VALUE:
            //       println pf_value.getBoolValue()
            //    break
            //    default:
            //       println "Value kind not supported ${it}"
            // }
         }

         // 4. TODO: transform openehr_protobuf_message to openehr_flat_map

         // 5. TODO: transform openehr_flat_map to openehr_rm_object

         println ""
      }
      catch (Exception e)
      {
         e.printStackTrace()
         println e.message
      }



      def tmp_flat_map = flat.getSerializedMap()

      def flat_parser = new FlatMapParser()

      def rm_compo = (Composition)flat_parser.parse(tmp_flat_map)

      //def json_serializer = new OpenEhrJsonSerializer()

      //println json_serializer.serialize(rm_compo, true)

      println JsonOutput.prettyPrint(JsonOutput.toJson(rm_compo))
   }
}