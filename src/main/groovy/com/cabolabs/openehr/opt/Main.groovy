package com.cabolabs.openehr.opt

import com.cabolabs.openehr.opt.instance_validation.XmlValidation
import com.cabolabs.openehr.opt.manager.OptManager
import com.cabolabs.openehr.opt.manager.OptRepository
import com.cabolabs.openehr.opt.manager.OptRepositoryFSImpl
import com.cabolabs.openehr.opt.ui_generator.OptUiGenerator
import com.cabolabs.openehr.opt.instance_generator.*
import com.cabolabs.openehr.opt.parser.*
import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.opt.instance_validation.JsonInstanceValidation
import com.cabolabs.openehr.opt.serializer.JsonSerializer
import com.cabolabs.openehr.formats.*
import com.cabolabs.openehr.rm_1_0_2.common.change_control.Version
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.validation.RmValidationReport
import com.cabolabs.openehr.validation.RmValidator2
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
         println 'trans: transforms an OPT in XML to JSON, or a XML/JSON locatable to JSON/XML respectively'
         System.exit(0)
      }

      switch (args[0])
      {
         case 'uigen':
            if (args.size() < 3)
            {
               println 'usage: opt uigen path_to_opt dest_folder [bootstrap] [generate]'
               println 'bootstrap=bs4|bs5 bs4 is the bootstrap 4 style, bs5 is the bootstrap 5 style'
               println 'generate=full|form full generates the whole html page, form generates just the form'
               System.exit(0)
            }

            def path = args[1] //"resources"+ PS +"opts"+ PS +"Encuentro.opt"
            def opt = loadAndParse(path)


            // options:
            boolean full = (args.contains('form') ? false : true)
            int bootstrap = (args.contains('bs4') ? 4 : 5)

            def gen = new OptUiGenerator(full, bootstrap)
            def ui = gen.generate(opt)

            def destination_path = args[2]
            def fname = destination_path + PS + new java.text.SimpleDateFormat("'"+ opt.concept +"_'yyyyMMddhhmmss'_"+ opt.langCode +".html'").format(new Date())
            new File(fname) << ui
            println "Generated: $fname"

         break
         case 'ingen':

            //println "ingen args "+ args.size() +" "+ args // DEBUG

            if (args.size() < 3)
            {
               println 'usage: opt ingen [path_to_opt|path_to_opt_folder] dest_folder [amount] [json|xml] [version|locatable] [withParticipations] [rm|api]'
               println 'withParticipations is only for COMPOSITION templates'
               println 'rm|api is the flavour of the data structure "rm" is default, "api" has resolved OBJECT_REFs'
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

            def format = 'json'
            def generate = 'locatable'

            if (args.size() > 4)
            {
               if (!['xml', 'json'].contains(args[4]))
               {
                  println "format should be one of 'json' or 'xml', '"+ args[4] +"' was specified"
                  System.exit(0)
               }

               format = args[4]
            }

            if (args.size() > 5)
            {
               if (!['version', 'locatable'].contains(args[5]))
               {
                  println "result type should be one of 'version' or 'locatable', '"+ args[5] +"' was specified"
                  System.exit(0)
               }

               generate = args[5]
            }

            def with_participations = args.contains('withParticipations')

            def flavor = (args.contains('api') ? 'api' : 'rm')

            generateInstances(opts, destination_path, with_participations, count, format, generate, flavor)

         break
         case 'optval':
            def inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream('xsd/OperationalTemplateExtra.xsd')
            def validator = new XmlValidation(inputStream)

            if (args.size() < 2)
            {
               println 'usage: opt optval path_to_opt'
               System.exit(0)
            }

            def path = args[1]
            def f = new File(path)
            if (!f.exists())
            {
               println path +" doesn't exist"
               System.exit(0)
            }

            validateXML(validator, f)
         break
         case 'inval':

            if (args.size() < 2)
            {
               println 'usage: opt inval path_to_xml_or_json_instance [rm|api] [semantic]'
               println 'usage: opt inval path_to_folder_with_xml_or_json_instances [rm|api] [semantic]'
               System.exit(0)
            }

            def path = args[1]


            def flavor = 'rm'

            if (args.size() > 2)
            {
               if (!['rm', 'api'].contains(args[2]))
               {
                  println 'usage: opt inval path_to_xml_or_json_instance [rm|api] [semantic]'
                  println 'usage: opt inval path_to_folder_with_xml_or_json_instances [rm|api] [semantic]'
                  println "flavor should be one of 'rm' or 'api', '"+ args[2] +"' was specified"
                  System.exit(0)
               }
               flavor = args[2]
            }


            def semantic = args.size() == 4 && args[3] == 'semantic'



            // Read XSD from JAR as a resource
            def inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream('xsd/Version.xsd')
            def validator = new XmlValidation(inputStream)

            // JSON Schema validation, loads the schema internally
            // TODO: get the rm version from the instances (might need to create a validator instance for each rm instance)
            def jsonValidator = new JsonInstanceValidation(flavor)



            def f = new File(path)
            if (!f.exists())
            {
               println path +" doesn't exist"
               System.exit(0)
            }

            // FIXME: if schema validation fails, don't validate against OPT

            if (f.isDirectory()) // validate all the XMLs in the folder
            {
               f.eachFileMatch(~/.*.xml/) { xml ->

                  if (validateXML(validator, xml) && semantic)
                  {
                     validateXMLWithOPT(xml)
                  }
               }

               f.eachFileMatch(~/.*.json/) { json ->

                  if (validateJSONInstance(jsonValidator, json) && semantic)
                  {
                     validateJSONWithOPT(json)
                  }
               }
            }
            else // Validate the XML or JSON instance referenced by the file
            {
               String ext = fileExtension(path)
               if (ext == 'json')
               {
                  if (validateJSONInstance(jsonValidator, f) && semantic)
                  {
                     validateJSONWithOPT(f)
                  }
               }
               else if (ext == 'xml')
               {
                  if (validateXML(validator, f) && semantic)
                  {
                     validateXMLWithOPT(f)
                  }
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
               println "Usage: opt.sh trans locatable path_to_locatable destination_folder\t--Transforms a XML or JSON LOCATABLE into JSON or XML"
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
               // TODO: trans FOLDER, EHR_STATUS, ACTOR, PARTY_RELATIONSHIP
               case "locatable":

                  String path = args[2] // locatable
                  File f = new File(path)
                  if (!f.exists() || f.isDirectory())
                  {
                     println "Path to locatable $path doesn't exist or is not a file"
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
                     Locatable c = parser.parseLocatable(xml)

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
                     Locatable c = parser.parseJson(json)

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
                     println "Extension $ext not supported, the LOCATABLE file should be .json or .xml"
                     System.exit(0)
                  }
               break
            }

            // [trans, locatable, source, dest]
            // transforms the locatable in the format it is, xml or json, into the other format


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

   static boolean validateJSONInstance(validator, file)
   {
      boolean isValid = true
      def validationMessages = validator.validate(file.text)
      if (validationMessages.size() == 0)
      {
         println file.name +' Schema VALID'
      }
      else
      {
         println file.name +' Schema NOT VALID'
         println '====================================='
         validationMessages.each {
            println it.message
         }
         println '====================================='
         isValid = false
      }

      println ""
      return isValid
   }

   static boolean validateXML(validator, file)
   {
      boolean isValid = true
      if (!validator.validate(file.text))
      {
         println file.name +' Schema NOT VALID'
         println '====================================='
         validator.errors.each {
            println it
         }
         println '====================================='
         isValid = false
      }
      else
      {
         println file.name +' Schema VALID'
      }

      println ""
      return isValid
   }

   static OperationalTemplate loadAndParse(String path)
   {
      def optFile = new File(path)

      if (!optFile.exists()) throw new java.io.FileNotFoundException(path)

      def inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream('xsd/OperationalTemplateExtra.xsd')
      def validator = new XmlValidation(inputStream)

      if (!validateXML(validator, optFile))
      {
         System.exit(1)
      }

      def text = optFile.getText()

      assert text != null
      assert text != ''

      // FIXME: validate OPT against schema

      def parser = new OperationalTemplateParser()
      return parser.parse( text )
   }

   static validateXMLWithOPT(File xml)
   {
      def parser = new OpenEhrXmlParser()
      def instance = parser.parseLocatable(xml.text) // xml should be a locatable
      validateCompositionWithOPT(instance)
   }

   static validateJSONWithOPT(File json)
   {
      def parser = new OpenEhrJsonParser()
      def instance = parser.parseJson(json.text)
      validateLocatableWithOPT(instance)
   }

   static validateLocatableWithOPT(Locatable compo)
   {
      String opt_repo_path = "src"+ PS +"main"+ PS +"resources"+ PS +"opts"
      OptRepository repo = new OptRepositoryFSImpl(opt_repo_path)
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)

      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(compo, OptManager.DEFAULT_NAMESPACE)

      if (report.hasErrors())
      {
         println "Semantic NOT VALID:"

         report.errors.each { error ->
            println error
         }
         println ""
      }
      else
      {
         println "Semantic VALID"
      }
   }

   static def generateInstances(List opts, String destination_path, boolean withParticipations, int count, String format, String generate, String flavor)
   {
      def out, printer, file_number = 1
      def ext = (format == 'json') ? 'json' : 'xml'
      def dt, path

      def generator = new RmInstanceGenerator()
      def instance

      def serializer, contents

      if (format == 'json')
      {
         serializer = new OpenEhrJsonSerializer(true)
      }
      else
      {
         serializer = new OpenEhrXmlSerializer(true)
      }

      opts.each { opt ->

         // println opt.definition.rmTypeName
         // println generate

         for (i in 1..count)
         {
            if (generate == 'version')
            {
               // Generates a version of the type specified in the OPT
               instance = generator.generateVersionFromOPT(opt, withParticipations, flavor)
            }
            else
            {
               switch (opt.definition.rmTypeName) // all possible archetype roots
               {
                  case 'COMPOSITION':
                     instance = generator.generateCompositionFromOPT(opt, withParticipations)
                  break
                  case 'PERSON':
                     if (flavor == 'rm')
                        instance = generator.generatePersonFromOPT(opt)
                     else
                        instance = generator.generatePersonDtoFromOPT(opt)
                  break
                  case 'ORGANISATION':
                     instance = generator.generateOrganizationFromOPT(opt)
                  break
                  case 'AGENT':
                     instance = generator.generateAgentFromOPT(opt)
                  break
                  case 'GROUP':
                     instance = generator.generateGroupFromOPT(opt)
                  break
                  case 'ROLE':
                     instance = generator.generateRoleFromOPT(opt)
                  break
                  case 'FOLDER':
                     instance = generator.generateFolderFromOPT(opt)
                  break
                  case 'EHR_STATUS':
                     instance = generator.generateEhrStatusFromOPT(opt)
                  break
                  default:
                     throw new Exception("Type ${opt.definition.rmTypeName} not supported yet")
                     // TODO: EHR_STATUS, FOLDER, AGENT, GROUP, ROLE
               }
            }

            // this is always pretty printed
            contents = serializer.serialize(instance)

            dt = new java.text.SimpleDateFormat("yyyyMMddhhmmss").format(new Date())
            path = destination_path + PS + (opt.templateId.replaceAll(' ', '_').toLowerCase() +"_"+ dt +"_"+ file_number.toString().padLeft(6, '0') +'_'+ i +'.'+ ext)
            out = new File(path)

            // Generates UTF-8 XML output
            printer = new java.io.PrintWriter(out, 'UTF-8')
            printer.write(contents)
            printer.flush()
            printer.close()

            file_number++

            println "Instance generated: "+ out.absolutePath
         }
      }
   }
}
