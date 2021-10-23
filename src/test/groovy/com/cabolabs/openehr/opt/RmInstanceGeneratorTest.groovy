package com.cabolabs.openehr.opt

import com.cabolabs.openehr.formats.OpenEhrJsonSerializer
import com.cabolabs.openehr.formats.OpenEhrXmlParser
import com.cabolabs.openehr.formats.OpenEhrXmlSerializer
import com.cabolabs.openehr.opt.instance_generator.RmInstanceGenerator
import com.cabolabs.openehr.opt.manager.OptManager
import com.cabolabs.openehr.opt.manager.OptRepository
import com.cabolabs.openehr.opt.manager.OptRepositoryFSImpl
import com.cabolabs.openehr.opt.parser.OperationalTemplateParser
import com.cabolabs.openehr.rm_1_0_2.common.change_control.Contribution
import com.cabolabs.openehr.rm_1_0_2.common.change_control.Version
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import com.cabolabs.openehr.validation.RmValidationReport
import com.cabolabs.openehr.validation.RmValidator

class RmInstanceGeneratorTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")

   void testRmIngenVersionEval()
   {
      String path = PS +"opts"+ PS +"com.cabolabs.openehr_opt.namespaces.default"+ PS +"minimal_evaluation_en_v1.opt"
      File file = new File(getClass().getResource(path).toURI())
      String xml = file.text
      def parser = new OperationalTemplateParser()
      def opt = parser.parse(xml)

      def ingen = new RmInstanceGenerator()
      def version = ingen.generateVersionFromOPT(opt, true)

      assert version != null

      // TODO: check data at paths

      // Serializing to check the contents, this could be removed from the test
      def serial = new OpenEhrJsonSerializer()
      println serial.serialize(version)

      // Validation of the composition against the OPT
      String opt_repo_path = PS + "opts"
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(opt_repo_path).toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)
      //opt_manager.loadAll()

      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(version.data, OptManager.DEFAULT_NAMESPACE)

      assert report.errors.size() == 0
   }

   void testRmIngenVersionPatDetail()
   {
      String path = PS +"opts"+ PS +"com.cabolabs.openehr_opt.namespaces.default"+ PS +"patient_detail.opt"
      File file = new File(getClass().getResource(path).toURI())
      String xml = file.text
      def parser = new OperationalTemplateParser()
      def opt = parser.parse(xml)

      def ingen = new RmInstanceGenerator()
      def version = ingen.generateVersionFromOPT(opt, true)

      assert version != null

      // TODO: check data at paths

      // Serializing to check the contents, this could be removed from the test
      def serial = new OpenEhrJsonSerializer()
      println serial.serialize(version)

      // Validation of the composition against the OPT
      String opt_repo_path = PS + "opts"
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(opt_repo_path).toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)
      //opt_manager.loadAll()

      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(version.data, OptManager.DEFAULT_NAMESPACE)

      assert report.errors.size() == 0
   }

   void testRmIngenVersionAboriginal()
   {
      String path = PS +"opts"+ PS +"com.cabolabs.openehr_opt.namespaces.default"+ PS +"aboriginal_and_torres_strait_islander_health_check_master.opt"
      File file = new File(getClass().getResource(path).toURI())
      String xml = file.text
      def parser = new OperationalTemplateParser()
      def opt = parser.parse(xml)

      def ingen = new RmInstanceGenerator()
      def version = ingen.generateVersionFromOPT(opt, true)

      assert version != null

      // TODO: check data at paths

      // Serializing to check the contents, this could be removed from the test
      def serial = new OpenEhrJsonSerializer()
      println serial.serialize(version)

      // Validation of the composition against the OPT
      String opt_repo_path = PS + "opts"
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(opt_repo_path).toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)
      //opt_manager.loadAll()

      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(version.data, OptManager.DEFAULT_NAMESPACE)

      assert report.errors.size() == 0
   }

   void testRmIngenVersionRipple()
   {
      String path = PS +"opts"+ PS +"com.cabolabs.openehr_opt.namespaces.default"+ PS +"ripple_conformance_test_template.opt"
      File file = new File(getClass().getResource(path).toURI())
      String xml = file.text
      def parser = new OperationalTemplateParser()
      def opt = parser.parse(xml)

      def ingen = new RmInstanceGenerator()
      def version = ingen.generateVersionFromOPT(opt, true)

      assert version != null

      // TODO: check data at paths

      // Serializing to check the contents, this could be removed from the test
      def serial = new OpenEhrJsonSerializer()
      println serial.serialize(version)

      // Validation of the composition against the OPT
      String opt_repo_path = PS + "opts"
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(opt_repo_path).toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)
      //opt_manager.loadAll()

      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(version.data, OptManager.DEFAULT_NAMESPACE)

      assert report.errors.size() == 0
   }




   /*
   void testXmlParserAndMarshallerVerasion()
   {
      String path = PS +"canonical_xml"+ PS +"test_all_datatypes.version.en.xml"
      File file = new File(getClass().getResource(path).toURI())
      String xml = file.text
      def parser = new OpenEhrXmlParser()
      Version v = (Version)parser.parseVersionXml(xml)

      OpenEhrXmlSerializer marshal = new OpenEhrXmlSerializer()
      String xml2 = marshal.serialize(v)

      //println xml.replaceAll(">\\s+<", "><").replaceAll("[\n\r]", "")
      //println xml2

      // original xml, when parsed and serialized again, are exactly the same as strings (without indentation and new lines)
      assert xml.replaceAll(">\\s+<", "><").replaceAll("[\n\r]", "") == xml2


      assert v.data.context.path == '/context'
      assert v.data.context.dataPath == '/context'

      assert v.data.content[0].path == '/content'
      assert v.data.content[0].dataPath == '/content[0]'

      assert v.data.content[0].data.path == '/content/data'
      assert v.data.content[0].data.dataPath == '/content[0]/data'

      assert v.data.content[0].data.events[0].path == '/content/data/events'
      assert v.data.content[0].data.events[0].dataPath == '/content[0]/data/events[0]'
   }
   */

}