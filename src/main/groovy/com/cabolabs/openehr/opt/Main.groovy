package com.cabolabs.openehr.opt

import com.cabolabs.openehr.opt.instance_validation.XmlInstanceValidation
import com.cabolabs.openehr.opt.ui_generator.OptUiGenerator
import com.cabolabs.openehr.opt.instance_generator.*
import com.cabolabs.openehr.opt.parser.*
import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.opt.instance_validation.JsonInstanceValidation

class Main {

   private static String PS = System.getProperty("file.separator")

   /*
    * uigen // generador de ui
    * ingen // generador de instancias
    * inval // validador de instancias con XSD
    */
   static void main(String[] args)
   {
      if (args.size() == 0 || args[0] == 'help')
      {
         println 'usage: opt command [options]'
         println 'command: [uigen, ingen, inval]'
         println 'uigen: user interface generation from an OPT'
         println 'ingen: XML instance generation from an OPT'
         println 'inval: XML instance validator'
         System.exit(0)
      }

      switch (args[0])
      {
         case 'uigen':
            if (args.size() < 3)
            {
               println 'usage: opt uigen path_to_opt dest_folder'
               System.exit(0)
            }

            def path = args[1] //"resources"+ PS +"opts"+ PS +"Encuentro.opt"
            def opt = loadAndParse(path)
            def gen = new OptUiGenerator()
            def ui = gen.generate(opt)

            def destination_path = args[2]
            new File( destination_path + PS + new java.text.SimpleDateFormat("'"+ opt.concept +"_'yyyyMMddhhmmss'_"+ opt.langCode +".html'").format(new Date()) ) << ui

         break
         case 'ingen':

            println "ingen args "+ args.size() +" "+ args // DEBUG

            if (args.size() < 3)
            {
               println 'usage: opt ingen path_to_opt dest_folder [amount] [version|composition|version_committer|tagged|json_version|json_composition] [withParticipations]'
               System.exit(0)
            }

            int count = 1
            if (args.size() > 3)
            {
               count = args[3].toInteger() // TOOD: check type conversion

               if (count <= 0)
               {
                  println "amount should be greater than 0"
                  System.exit(0)
               }
            }

            def path = args[1] //"resources"+ PS +"opts"+ PS +"Referral.opt"
            def opt = loadAndParse(path)

            // test
            /*
            opt.nodes.sort{ it.key }.each { p, o -> println p +' '+ o.getClass().getSimpleName() +' '+ ((o.getClass().getSimpleName() == 'AttributeNode') ? o.children.size() : '') }
            println opt.getNode('/content[archetype_id=openEHR-EHR-ACTION.test_action_multiple_occurence_node.v1]/ism_transition')
            println opt.getNode('/content[archetype_id=openEHR-EHR-ACTION.test_action_multiple_occurence_node.v1]/ism_transition[at0004]')
            */

            def destination_path = args[2]
            if (!new File(destination_path).exists())
            {
               println "destination_path $destination_path doesn't exist"
               System.exit(0)
            }

            def generate = 'version'
            if (args.size() > 4)
            {
               if (!['version', 'composition', 'version_committer', 'tagged', 'json_version', 'json_composition'].contains(args[4]))
               {
                  println "result type should be one of 'version', 'composition', 'version_committer', 'tagged', 'json_version', 'json_composition'"
                  System.exit(0)
               }

               generate = args[4]
            }

            def withParticipations = args.contains('withParticipations')
            //println withParticipations

            def igen, ins, out, printer, ext = 'xml'
            for (i in 1..count)
            {
               if (generate == 'composition')
               {
                  igen = new XmlInstanceGenerator()
                  ins = igen.generateXMLCompositionStringFromOPT(opt, withParticipations)
               }
               else if (generate == 'version')
               {
                  igen = new XmlInstanceGenerator()
                  ins = igen.generateXMLVersionStringFromOPT(opt, withParticipations)
               }
               else if (generate == 'tagged')
               {
                  igen = new XmlInstanceGeneratorTagged()
                  ins = igen.generateXMLVersionStringFromOPT(opt)
               }
               else if (generate == 'json_version')
               {
                  igen = new JsonInstanceCanonicalGenerator2()
                  ins = igen.generateJSONVersionStringFromOPT(opt, withParticipations, true)
                  ext = 'json'
               }
               else if (generate == 'json_composition')
               {
                  igen = new JsonInstanceCanonicalGenerator2()
                  ins = igen.generateJSONCompositionStringFromOPT(opt, withParticipations, true)
                  ext = 'json'
               }
               else
               {
                  igen = new XmlInstanceGeneratorForCommitter()
                  ins = igen.generateXMLVersionStringFromOPT(opt)
               }

               out = new File( destination_path + PS + new java.text.SimpleDateFormat("'"+ opt.concept.replaceAll(' ', '_') +"_'yyyyMMddhhmmss'_"+ i +"."+ ext +"'").format(new Date()) )

               // Generates UTF-8 XML output
               printer = new java.io.PrintWriter(out, 'UTF-8')
               printer.write(ins)
               printer.flush()
               printer.close()


               println "Instance generated: "+ out.absolutePath
            }
         break
         case 'inval':

            // Read XSD from JAR as a resource
            def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
            def validator = new XmlInstanceValidation(inputStream)

            // JSON Schema validation, loads the schema internally
            def jsonValidator = new JsonInstanceValidation()

            if (args.size() < 2)
            {
               println 'usage: opt inval path_to_xml_or_json_instance'
               println 'usage: opt inval path_to_folder_with_xml_or_json_instances'
               System.exit(0)
            }

            def path = args[1]
            def f = new File(path)
            if (!f.exists())
            {
               println path +" doesn't exist"
               System.exit(0)
            }

            if (f.isDirectory()) // validate all the XMLs in the folder
            {
               f.eachFileMatch(~/.*.xml/) { xml ->

                 validateXMLInstance(validator, xml)
               }

               f.eachFileMatch(~/.*.json/) { json ->

                 validateJSONInstance(jsonValidator, json)
               }
            }
            else // Validate the XML or JSON instance referenced by the file
            {
               String ext = fileExtension(path)
               if (ext == 'json')
               {
                  validateJSONInstance(jsonValidator, f)
               }
               else if (ext == 'xml')
               {
                  validateXMLInstance(validator, f)
               }
               else
               {
                  println "File extension ${ext} is not supported, only json and xml are supported"
                  System.exit(0)
               }
            }

         break
         default:
            println "command "+ args[0] +" not recognized"
      }
   }

   static String fileExtension(String path)
   {
      path.lastIndexOf('.').with {it != -1 ? path.substring(it+1):''}
   }

   static void validateJSONInstance(validator, file)
   {
      def validationMessages = validator.validate(file.text)
      if (validationMessages.size() == 0)
      {
         println file.name +' VALID'
      }
      else
      {
         println file.name +' NOT VALID'
         println '====================================='
         validationMessages.each {
            println it.message
         }
         println '====================================='
      }

      println ""
   }

   static void validateXMLInstance(validator, file)
   {
      if (!validator.validate( file.text ))
      {
         println file.name +' NOT VALID'
         println '====================================='
         validator.errors.each {
            println it
         }
         println '====================================='
      }
      else
      {
         println file.name +' VALID'
      }
      
      println ""
   }

   static OperationalTemplate loadAndParse(String path)
   {
      def optFile = new File( path )

      if (!optFile.exists()) throw new java.io.FileNotFoundException(path)

      def text = optFile.getText()

      assert text != null
      assert text != ''

      def parser = new OperationalTemplateParser()
      return parser.parse( text )
   }
}
