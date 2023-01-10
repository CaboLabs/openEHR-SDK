package com.cabolabs.openehr.formats

import com.cabolabs.openehr.rm_1_0_2.common.change_control.Version
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable

import com.cabolabs.openehr.dto_1_0_2.ehr.EhrDto

import com.cabolabs.openehr.rm_1_0_2.data_types.basic.DataValue

import com.cabolabs.openehr.rm_1_0_2.data_types.text.*
import com.cabolabs.openehr.rm_1_0_2.data_types.basic.*
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.*
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.*
import com.cabolabs.openehr.rm_1_0_2.data_types.encapsulated.*
import com.cabolabs.openehr.rm_1_0_2.data_types.uri.*

import groovy.json.*
import groovy.xml.*

class FlatMapSerializer {

   private Map serialized = [:]

   // transforms a Java type into the correspondent openEHR type name
   // EventContext => EVENT_CONTEXT
   private String openEhrType(Object o)
   {
      String clazz = o.getClass().getSimpleName()
      if (clazz == "Organization") clazz = "Organisation" // alias of UK based RM!
      clazz.replaceAll("[A-Z]", '_$0').toUpperCase().replaceAll( /^_/, '')
   }

   private String method(Object obj)
   {
      String type = obj.getClass().getSimpleName()
      String method = 'serialize'+ type
      return method
   }

   Map getSerialized()
   {
      return this.serialized
   }

   void serialize(Locatable o)
   {
      //String method = this.method(o)
      //def out = this."$method"(o)
      this.traverse(o)
   }

   void serialize(Version v)
   {

   }

   void serialize(EhrDto ehr)
   {

   }

/*
   String get(String format = 'json', boolean pretty = false)
   {
      if (!['xml', 'json'].contains(format))
      {
         throw new Exception("Invalid format, should be 'xml' or 'json' and it's '$format'")
      }

      if (format == 'json')
      {
         if (pretty)
         {
            return JsonOutput.prettyPrint(JsonOutput.toJson(this.serialized))
         }

         return JsonOutput.toJson(this.serialized)
      }
      else
      {
         def builder
         def writer = new StringWriter()

         if (pretty)
         {
            builder = new MarkupBuilder(writer)
         }
         else
         {
            builder = new MarkupBuilder(new IndentPrinter(new PrintWriter(writer), "", false))
         }

         builder.setDoubleQuotes(true)

         // <flat_map>
         //   <node path="path">value</node>
         //   <node path="path">value</node>
         //   <node path="path">value</node>
         // </flat_map>

         builder.flat_map {

            this.serialized.each { path, value ->

               node(path: path, value)
            }
         }

         return writer.toString()
      }
   }
   */

   void traverse(Object o)
   {
      String path
      o.properties.each { prop, val ->

         // _null is considered a property in Element because of the method is_null()
         if (['class', 'parent', '_null'].contains(prop)) return

         if (val == null) return


         // check if property is synthetic, but need to go up in the hierarcht to the class that declared the prop
         /*
         def _continue = true
         def currentClass = o.class
         while (_continue)
         {
            try
            {
               if (currentClass.getDeclaredField(prop).synthetic)
               {
                  println "prop ${prop} is synthetic"
               }

               _continue = false
            }
            catch (Exception e)
            {
               currentClass = currentClass.superclass

               if (!currentClass)
               {
                  println "can't get prop ${prop} from ${o.class}, trying superclass ${currentClass}"
                  _continue = false
               }
            }
         }
         */


         if (val instanceof Locatable)
         {
            println "prop ${prop} Loc"
            traverse(val)
         }
         else if (val instanceof Pathable)
         {
            println "prop ${prop} Pat"
            traverse(val)
         }
         else if (val instanceof DataValue)
         {
            println "prop ${prop} DV"

            if (o instanceof Pathable)
            {
               path = o.dataPath == '/' ? '/'+ prop : o.dataPath +'/'+ prop

               process_dv(val, path) // adds the attribute name to the path
            }
            else
            {
               println "DV parent not Pat ${o.class}"
            }
         }
         else if (val instanceof Collection)
         {
            println "prop ${prop} coll"
            //println val.size()
            val.each { single_value ->

               traverse(single_value)
               //println "single value ${single_value.getClass()}"
            }
         }
         else if (val instanceof CodePhrase)
         {
            if (o instanceof Pathable)
            {
               path = o.dataPath == '/' ? '/'+ prop : o.dataPath +'/'+ prop

               process_cp(val, path) // adds the attribute name to the path
            }
            else
            {
               println "CP parent not Pat ${o.class}"
            }
         }
         else
         {
            println "prop ${prop} ${val.getClass()}"
         }
      }
   }

   void process_cp(CodePhrase cp, String parentPath)
   {
      this.serialized[parentPath +'/code_string'] = cp.code_string
      this.serialized[parentPath +'/terminology_id/value'] = cp.terminology_id.value
   }

   void process_dv(DataValue dv, String parentPath)
   {
      println "DV parent ${parentPath}"

      String path

      dv.properties.each { prop, val ->

         // _null is considered a property in Element because of the method is_null()
         if (['class', 'parent', '_null'].contains(prop)) return

         if (val == null) return

         if (val instanceof DataValue)
         {
            println "DV prop ${prop} DV"

            if (o instanceof Pathable)
            {
               path = o.dataPath == '/' ? '/'+ prop : o.dataPath +'/'+ prop
               process_dv(val, path)
            }
         }
         else
         {
            println "DV prop ${prop} ${val.getClass()}"
         }
      }
   }

   void process(DvText dv, String parentPath)
   {

   }

   void process(DvCodedText dv, String parentPath)
   {

   }

   void process(CodePhrase dv, String parentPath)
   {

   }

   void process(DvDate dv, String parentPath)
   {

   }

   void process(DvTime dv, String parentPath)
   {

   }

   void process(DvDateTime dv, String parentPath)
   {

   }

   void process(DvDuration dv, String parentPath)
   {

   }

   void process(DvBoolean dv, String parentPath)
   {

   }

   void process(DvIdentifier dv, String parentPath)
   {

   }

   void process(DvCount dv, String parentPath)
   {

   }

   void process(DvQuantity dv, String parentPath)
   {

   }

   void process(DvProportion dv, String parentPath)
   {

   }

   void process(DvOrdinal dv, String parentPath)
   {

   }

   void process(DvUri dv, String parentPath)
   {

   }

   void process(DvEhrUri dv, String parentPath)
   {

   }

   void process(DvMultimedia dv, String parentPath)
   {

   }

   void process(DvParsable dv, String parentPath)
   {

   }
}