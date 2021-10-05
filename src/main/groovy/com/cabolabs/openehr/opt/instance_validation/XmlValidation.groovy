package com.cabolabs.openehr.opt.instance_validation

import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory
import javax.xml.validation.Validator
import javax.xml.XMLConstants

import org.xml.sax.ErrorHandler
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException
import java.io.InputStream

import groovy.util.slurpersupport.GPathResult

class XmlValidation {

   def errors = []
   def xsdPath
   def xsdStream
   
   Schema schema
   
   def XmlValidation(String path_to_xsd)
   {
      xsdPath = path_to_xsd
      init()
   }
   
   def XmlValidation(InputStream xsd_as_stream)
   {
      xsdStream = xsd_as_stream
      init()
   }
   
   private void init()
   {
      SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
      
      if (this.xsdPath) this.schema = schemaFactory.newSchema( [ new StreamSource( this.xsdPath ) ] as Source[] )
      else this.schema = schemaFactory.newSchema( [ new StreamSource( this.xsdStream ) ] as Source[] ) // stream cant be read twice, so it should be initialized once in the init to support many calls to validate.
   }
   
   public boolean validate(GPathResult xml, Map namespaces)
   {
      //xml.'@xmlns' = 'http://schemas.openehr.org/v1'
      //xml.'@xmlns:xsi' = 'http://www.w3.org/2001/XMLSchema-instance'
      namespaces.each { ns, val ->
         xml."@$ns" = val
      }
      def xmlStr = groovy.xml.XmlUtil.serialize( xml )
      
      return this._validate(xmlStr)
   }
   
   public boolean validate(String xml)
   {
      return this._validate(xml)
   }
   
   public List<String> getErrors()
   {
      return this.errors
   }
   
   private boolean _validate(String xml)
   {
      this.errors = [] // Reset the errors for reuse
      
      // Validate with validator
      Validator validator = this.schema.newValidator()
      ErrorHandler errorHandler = new SimpleErrorHandler(xml)
      validator.setErrorHandler(errorHandler)
      
      try
      {
         validator.validate(new StreamSource(new StringReader(xml)))
      }
      catch (org.xml.sax.SAXParseException e) // XML not valid
      {
         errorHandler.exceptions << e
      }
      
      this.errors = errorHandler.getErrors()
      
      return !errorHandler.hasErrors() // If validates is false, then the user can .getErrors()
   }
   
   
   private class SimpleErrorHandler implements ErrorHandler {
     
      def exceptions = []
      def xml_lines
      
      public SimpleErrorHandler(String xml)
      {
         this.xml_lines = xml.readLines()
      }
   
      public void warning(SAXParseException e) throws SAXException
      {
         this.exceptions << e
      }
   
      public void error(SAXParseException e) throws SAXException
      {
         this.exceptions << e
      }
   
      // if a fatal error occurs, then this stop validating
      public void fatalError(SAXParseException e) throws SAXException
      {
         this.exceptions << e
      }
      
      public boolean hasErrors()
      {
         return this.exceptions.size() > 0
      }
      
      public List<String> getErrors()
      {
         def ret = []
         this.exceptions.each { e ->
            
            ret << "ERROR "+ e.getMessage() +"\nline #: "+ e.getLineNumber() +"\n>>> "+
                   this.xml_lines[e.getLineNumber()-1].trim() // line of the problem in the XML
            //        (e.getColumnNumber()-1).times{ print " " } // marks the column
            //        println "^"
         }
         return ret
      }
   }
}
