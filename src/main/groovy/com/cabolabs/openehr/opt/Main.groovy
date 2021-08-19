package com.cabolabs.openehr.opt

import com.cabolabs.openehr.opt.instance_validation.XmlInstanceValidation
import com.cabolabs.openehr.opt.ui_generator.OptUiGenerator
import com.cabolabs.openehr.opt.instance_generator.*
import com.cabolabs.openehr.opt.parser.*
import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.opt.instance_validation.JsonInstanceValidation
import com.cabolabs.openehr.opt.serializer.JsonSerializer
import com.cabolabs.openehr.formats.*
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import groovy.json.JsonOutput

class Main {

   private static String PS = System.getProperty("file.separator")

   /*
    * uigen // generador de ui
    * ingen // generador de instancias
    * inval // validador de instancias con XSD
    */
   static void main(String[] args)
   {
      def out, printer

      if (args.size() == 0 || args[0] == 'help')
      {
         println 'usage: opt command [options]'
         println 'command: [uigen, ingen, inval]'
         println 'uigen: user interface generation from an OPT'
         println 'ingen: XML/JSON instance generation from an OPT'
         println 'inval: XML/JSON instance validator'
         println 'trans: transforms an OPT in XML to JSON, or a XML/JSON composition to JSON/XML respectively'
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

            //println "ingen args "+ args.size() +" "+ args // DEBUG

            if (args.size() < 3)
            {
               println 'usage: opt ingen [path_to_opt|path_to_opt_folder] dest_folder [amount] [version|composition|version_committer|tagged|json_version|json_composition|json_compo_with_errors] [withParticipations]'
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
            def opts = []

            // check of path points to a file or folder
            def source = new File(path)
            if (source.isDirectory())
            {
               source.eachFileMatch(~/.*.opt/) { opt_file ->

                 opts << loadAndParse(opt_file.path)
               }
            }
            else
            {
               opts << loadAndParse(path)
            }

            

            // test
            /*
            opt.nodes.sort{ it.key }.each { p, o -> println p +' '+ o.getClass().getSimpleName() +' '+ ((o.getClass().getSimpleName() == 'AttributeNode') ? o.children.size() : '') }
            println opt.getNode('/content[archetype_id=openEHR-EHR-ACTION.test_action_multiple_occurence_node.v1]/ism_transition')
            println opt.getNode('/content[archetype_id=openEHR-EHR-ACTION.test_action_multiple_occurence_node.v1]/ism_transition[at0004]')
            */

            def destination_path = args[2]
            verifyFolder(destination_path)

            def generate = 'version'
            if (args.size() > 4)
            {
               if (!['version', 'composition', 'version_committer', 'tagged', 'json_version', 'json_composition', 'json_compo_with_errors'].contains(args[4]))
               {
                  println "result type should be one of 'version', 'composition', 'version_committer', 'tagged', 'json_version', 'json_composition', 'json_compo_with_errors'"
                  System.exit(0)
               }

               generate = args[4]
            }

            def with_participations = args.contains('withParticipations')

            generateInstances(opts, destination_path, with_participations, count, generate)
            
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
         case 'trans':

            def ext

            if (args.size() < 4)
            {
               println "Usage: opt.sh trans opt path_to_opt destination_folder                \t--Transforms XML OPT into JSON"
               println "Usage: opt.sh trans composition path_to_composition destination_folder\t--Transforms a XML or JSON COMPOSITION into JSON or XML"
               System.exit(0)
            }

            // ['trans', 'opt', source_opt, dest_folder]
            // tansform opt xml to json
            switch (args[1])
            {
               case "opt":

                  String path = args[2] // OPT 
                  File f = new File(path)
                  if (!f.exists() || f.isDirectory())
                  { 
                     println "Path to OPT $path doesn't exist or is not a file"
                     System.exit(0)
                  }

                  def dpath = args[3]
                  File df = new File(dpath);
                  verifyFolder(dpath)

                  ext = 'json'

                  def opt = loadAndParse(path)
                  def toJson = new JsonSerializer()
                  toJson.serialize(opt)

                  out = new File(dpath + PS + new java.text.SimpleDateFormat("'"+ opt.concept.replaceAll(' ', '_') +"_'yyyyMMddhhmmss_opt'."+ ext +"'").format(new Date()) )

                  // Generates UTF-8 output
                  printer = new java.io.PrintWriter(out, 'UTF-8')
                  printer.write(toJson.get(true))
                  printer.flush()
                  printer.close()
               break
               case "composition":

                  String path = args[2] // composition
                  File f = new File(path)
                  if (!f.exists() || f.isDirectory())
                  { 
                     println "Path to composition $path doesn't exist or is not a file"
                     System.exit(0)
                  }

                  def dpath = args[3]
                  verifyFolder(dpath)


                  ext = fileExtension(path)
                  if (ext == 'xml') // XML to JSON
                  {
                     // Parse XML
                     String xml = f.text
                     def parser = new OpenEhrXmlParser()
                     Composition c = (Composition)parser.parseXml(xml)

                     // debug
                     // out = JsonOutput.toJson(c)
                     // out = JsonOutput.prettyPrint(out)
                     // println out

                     // Serialize to JSON
                     def serializer = new OpenEhrJsonSerializer()
                     String json = serializer.serialize(c)

                     // Output
                     ext = 'json'
                     out = new File(dpath + PS + new java.text.SimpleDateFormat("'"+ (f.name.replaceAll(' ', '_') -'.xml') +"_'yyyyMMddhhmmss'."+ ext +"'").format(new Date()) )

                     // Generates UTF-8 output
                     printer = new java.io.PrintWriter(out, 'UTF-8')
                     printer.write(json)
                     printer.flush()
                     printer.close()

                     println "Created "+ out.name
                     println ""
                  }
                  else if (ext == 'json') // JSON to XML
                  {
                     // Parse JSON
                     String json = f.text
                     def parser = new OpenEhrJsonParser()
                     Composition c = (Composition)parser.parseJson(json)

                     // Serialize to XML
                     def serializer = new OpenEhrXmlSerializer()
                     String xml = serializer.serialize(c)

                     // Output
                     ext = 'xml'
                     out = new File(dpath + PS + new java.text.SimpleDateFormat("'"+ (f.name.replaceAll(' ', '_') -'.json') +"_'yyyyMMddhhmmss'."+ ext +"'").format(new Date()) )

                     // Generates UTF-8 output
                     printer = new java.io.PrintWriter(out, 'UTF-8')
                     printer.write(xml)
                     printer.flush()
                     printer.close()

                     println "Created "+ out.name
                     println ""
                  }
                  else
                  {
                     println "Extension $ext not supported, the COMPOSITION file should be .json or .xml"
                     System.exit(0)
                  }
               break
            }

            // [trans, composition, source, dest]
            // transforms the composition in the format it is, xml or json, into the other format


         break
         default:
            println "command "+ args[0] +" not recognized"
      }
   }

   // path to folder exists or exit
   static void verifyFolder(String path)
   {
      File df = new File(path);
      if (!df.exists() || !df.isDirectory())
      { 
         println "Path to destination $path doesn't exist or is not a folder"
         System.exit(0)
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
      def optFile = new File(path)

      if (!optFile.exists()) throw new java.io.FileNotFoundException(path)

      def text = optFile.getText()

      assert text != null
      assert text != ''

      def parser = new OperationalTemplateParser()
      return parser.parse( text )
   }

   static def generateInstances(List opts, String destination_path, boolean withParticipations, int count, String generate)
   {
      def out, printer, igen, ins, ext = 'xml', file_number = 1

      opts.each { opt ->

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
            else if (generate == 'json_compo_with_errors')
            {
               igen = new JsonInstanceCanonicalGeneratorCardinalityErrors()
               ins = igen.generateJSONCompositionStringFromOPT(opt, withParticipations, true)
               ext = 'json'
            }
            else
            {
               igen = new XmlInstanceGeneratorForCommitter()
               ins = igen.generateXMLVersionStringFromOPT(opt)
            }

            out = new File(destination_path + PS + (opt.templateId.replaceAll(' ', '_') +"_"+ file_number.toString().padLeft(6, '0') +'_'+ i +'.'+ ext))

            // Generates UTF-8 XML output
            printer = new java.io.PrintWriter(out, 'UTF-8')
            printer.write(ins)
            printer.flush()
            printer.close()

            file_number++


            println "Instance generated: "+ out.absolutePath
         }
      }
   }
}
