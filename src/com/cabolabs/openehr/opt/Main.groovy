package com.cabolabs.openehr.opt

import com.cabolabs.openehr.opt.instance_validation.XmlInstanceValidation
import com.cabolabs.openehr.opt.ui_generator.OptUiGenerator
import com.cabolabs.openehr.opt.instance_generator.*
import com.cabolabs.openehr.opt.parser.*
import com.cabolabs.openehr.opt.model.*

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
         println 'uigen: user interface generationt from an OPT'
         println 'ingen: XML instance generationt from an OPT'
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
            if (args.size() < 3)
            {
               println 'usage: opt ingen path_to_opt dest_folder'
               System.exit(0)
            }
         
            def path = args[1] //"resources"+ PS +"opts"+ PS +"Referral.opt"
            def opt = loadAndParse(path)
            def igen = new XmlInstanceGenerator()
            def ins = igen.generateXMLCompositionStringFromOPT(opt)
            
            def destination_path = args[2]
            def out = new File( destination_path + PS + new java.text.SimpleDateFormat("'"+ opt.concept+"_'yyyyMMddhhmmss'.xml'").format(new Date()) )
            
            // Generates UTF-8 XML output
            def printer = new java.io.PrintWriter(out, 'UTF-8')
            printer.write(ins)
            printer.flush()
            printer.close()
            
            println "Instance generated: "+ out.absolutePath
         break
         case 'inval':
            
            // TODO: add the xsd to the JAR and access it through
            //       http://stackoverflow.com/questions/8258244/accessing-a-file-inside-a-jar-file
            /*
            def validator = new XmlInstanceValidation('xsd'+ File.separator + 'Version.xsd')
            new File('documents' + File.separator).eachFileMatch(~/.*.xml/) { xml ->

              if (!validator.validate( xml.text ))
              {
                 println xml.name +' NO VALIDA'
                 println '====================================='
                 validator.errors.each {
                    println it
                 }
                 println '====================================='
              }
              else
                 println xml.name +' VALIDA'
            }
            */
         break
      }
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

