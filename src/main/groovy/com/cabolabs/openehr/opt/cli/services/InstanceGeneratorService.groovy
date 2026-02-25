package com.cabolabs.openehr.opt.cli.services

import com.cabolabs.openehr.opt.model.OperationalTemplate
import com.cabolabs.openehr.opt.instance_generator.RmInstanceGenerator
import com.cabolabs.openehr.formats.OpenEhrJsonSerializer
import com.cabolabs.openehr.formats.OpenEhrXmlSerializer

class InstanceGeneratorService {

   private static String PS = System.getProperty("file.separator")

   static void generateInstances(List<OperationalTemplate> opts, String destPath, 
                                  boolean withParticipations, int count, 
                                  String format, String generate, String flavor) {
      
      def generator = new RmInstanceGenerator()
      def serializer = (format == 'json') ? new OpenEhrJsonSerializer(true) : new OpenEhrXmlSerializer(true)
      def ext = (format == 'json') ? 'json' : 'xml'
      def fileNumber = 1

      opts.each { opt ->
         for (i in 1..count) {
            def instance = generateInstance(generator, opt, generate, withParticipations, flavor)
            def contents = serializer.serialize(instance)
            
            def dt = new java.text.SimpleDateFormat("yyyyMMddhhmmss").format(new Date())
            def path = destPath + PS + (opt.templateId.replaceAll(' ', '_').toLowerCase() + "_" + dt + "_" + fileNumber.toString().padLeft(6, '0') + '_' + i + '.' + ext)
            def out = new File(path)
            
            def printer = new java.io.PrintWriter(out, 'UTF-8')
            printer.write(contents)
            printer.flush()
            printer.close()
            
            fileNumber++
            println "Instance generated: " + out.absolutePath
         }
      }
   }

   private static generateInstance(generator, opt, generate, withParticipations, flavor) {
      if (generate == 'version') {
         return generator.generateVersionFromOPT(opt, withParticipations, flavor)
      }
      
      switch (opt.definition.rmTypeName) {
         case 'COMPOSITION':
            return generator.generateCompositionFromOPT(opt, withParticipations)
         case 'PERSON':
            return (flavor == 'rm') ? generator.generatePersonFromOPT(opt) : generator.generatePersonDtoFromOPT(opt)
         case 'ORGANISATION':
            return generator.generateOrganizationFromOPT(opt)
         case 'AGENT':
            return generator.generateAgentFromOPT(opt)
         case 'GROUP':
            return generator.generateGroupFromOPT(opt)
         case 'ROLE':
            return generator.generateRoleFromOPT(opt)
         case 'FOLDER':
            return generator.generateFolderFromOPT(opt)
         case 'EHR_STATUS':
            return generator.generateEhrStatusFromOPT(opt)
         case 'PARTY_RELATIONSHIP':
            return generator.generateRelationshipFromOPT(opt)
         default:
            throw new Exception("Type ${opt.definition.rmTypeName} not supported yet")
      }
   }
}
