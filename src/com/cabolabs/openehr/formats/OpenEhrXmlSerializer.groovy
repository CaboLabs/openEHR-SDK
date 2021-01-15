package com.cabolabs.openehr.formats

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Archetyped
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import com.cabolabs.openehr.rm_1_0_2.composition.EventContext
import com.cabolabs.openehr.rm_1_0_2.composition.content.navigation.Section
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime
import com.cabolabs.openehr.rm_1_0_2.data_types.text.CodePhrase
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvCodedText
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvText
import com.cabolabs.openehr.rm_1_0_2.support.identification.ArchetypeId
import com.cabolabs.openehr.rm_1_0_2.support.identification.HierObjectId
import com.cabolabs.openehr.rm_1_0_2.support.identification.ObjectVersionId
import com.cabolabs.openehr.rm_1_0_2.support.identification.TemplateId
import com.cabolabs.openehr.rm_1_0_2.support.identification.TerminologyId
import groovy.xml.MarkupBuilder
import java.text.SimpleDateFormat

class OpenEhrXmlSerializer {
   
   def writer
   def builder
   
   private String method(Object obj)
   {
      String type = obj.getClass().getSimpleName()
      String method = 'serialize'+ type
      return method
   }
   
   private void fillLocatable(Locatable o)
   {
      String method = this.method(o.name) // text or coded
      builder.name() {
         this."$method"(o.name)
      }
      
      method = this.method(o.uid)
            builder.uid('xsi:type': openEhrType(o.uid)) {
         this."$method"(o.uid)
      }
      
      // TODO: links
      
      serializeArchetyped(o.archetype_details)
      
      // this should be an attribute
      //builder.archetype_node_id(o.archetype_node_id)
      
      // TODO: feeder audit
   }
   
   private void serializeArchetyped(Archetyped o)
   {
      builder.archetype_details {
         serializeArchetypeId(o.archetype_id)
         serializeTemplateId(o.template_id)
         rm_version(o.rm_version)
      }
   }
   
   private void serializeArchetypeId(ArchetypeId o)
   {
      builder.archetype_id() {
         value(o.value)
      }
   }
   
   private void serializeTemplateId(TemplateId o)
   {
      builder.template_id() {
         value(o.value)
      }
   }
   
   // transforms a Java type into the correspondent openEHR type name
   // EventContext => EVENT_CONTEXT
   private String openEhrType(Object o)
   {
      o.getClass().getSimpleName().replaceAll("[A-Z]", '_$0').toUpperCase().replaceAll( /^_/, '')
   }
   
   String serialize(Locatable o)
   {
      writer = new StringWriter()
      builder = new MarkupBuilder(writer)
      builder.setDoubleQuotes(true)
      
      String method = this.method(o)
      this."$method"(o)
      
      return writer.toString()
   }
   
   void serializeComposition(Composition c)
   {
      builder.composition(xmlns:'http://schemas.openehr.org/v1',
         'xmlns:xsi':'http://www.w3.org/2001/XMLSchema-instance',
         archetype_node_id: c.archetype_node_id)
      {
         //generateCompositionHeader(addParticipations) // name, language, territory, ...
         //generateCompositionContent(opt.definition.archetypeId)
         fillLocatable(c)
         language {
            serializeCodePhrase(c.language)
         }
         territory {
            serializeCodePhrase(c.territory)
         }
         category {
            serializeDvCodedText(c.category)
         }
         composer {
            
         }
         context() {
            serializeEventContext(c.context)
         }
         // TODO: content
      }
   }
   
   void serializeEventContext(EventContext e)
   {
      builder.start_time() {
         serializeDvDateTime(e.start_time)
      }
      if (e.end_time)
      {
         builder.end_time() {
            serializeDvDateTime(e.end_time)
         }
      }
      
      if (e.location)
      {
         builder.location(e.location)
      }
      
      builder.setting() {
         serializeDvCodedText(e.setting)
      }
      
      // TODO
   
   }
   
   void serializeSection(Section s)
   {
      
   }
   
   void serializeDvDateTime(DvDateTime o)
   {
      // TODO: accuracy, ...
      builder.value(o.value)
   }
   
   void serializeDvText(DvText o)
   {
      builder.value(o.value)
      
      // TODO: mappings
   }
   
   void serializeDvCodedText(DvCodedText o)
   {
      builder.value(o.value)
      
      builder.defining_code()
      {
         this.serializeCodePhrase(o.defining_code)
      }
   }
   
   void serializeCodePhrase(CodePhrase o)
   {
      builder.code_string(o.code_string)
      
      builder.terminology_id() {
         this.serializeTerminologyId(o.terminology_id)
      }
   }
   
   void serializeTerminologyId(TerminologyId o)
   {
      builder.value(o.value)
   }
   
   void serializeHierObjectId(HierObjectId o)
   {
      builder.value(o.value)
   }
   
   void serializeObjectVersionId(ObjectVersionId o)
   {
      builder.value(o.value)
   }
}
