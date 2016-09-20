package com.cabolabs.openehr.opt.ui_generator

import com.cabolabs.openehr.opt.model.AttributeNode
import com.cabolabs.openehr.opt.model.ObjectNode
import com.cabolabs.openehr.opt.model.OperationalTemplate
import com.cabolabs.openehr.terminology.TerminologyParser
import groovy.xml.MarkupBuilder

class OptUiGenerator {

   OperationalTemplate opt
   TerminologyParser terminology
   
   private static List datavalues = [
      'DV_TEXT', 'DV_CODED_TEXT', 'DV_QUANTITY', 'DV_COUNT',
      'DV_ORDINAL', 'DV_DATE', 'DV_DATE_TIME', 'DV_PROPORTION',
      'DV_DURATION']
   
   static String PS = File.separator
   
   String generate(OperationalTemplate opt)
   {
      this.opt = opt
      
      // loads the openehr terminolgy
      // FIXME: THIS IS THE EN TERMINOLOGY, if the OPT is in another language, it needs to load that terminology
      this.terminology = new TerminologyParser()
      this.terminology.parseTerms(new File("resources"+ PS +"terminology"+ PS +"openehr_terminology_en.xml")) // TODO: parameter
      this.terminology.parseTerms(new File("resources"+ PS +"terminology"+ PS +"openehr_terminology_es.xml"))
      
      def writer = new StringWriter()
      def builder = new MarkupBuilder(writer)
      builder.setDoubleQuotes(true) // Use double quotes on attributes
      
      // Generates HTML while traversing the archetype tree
      builder.html(lang: opt.langCode) {
        head() {
          meta(name: "viewport", content: "width=device-width, initial-scale=1")
          
          mkp.comment('simple style')
          link(rel:"stylesheet", href:"style.css")
          
          mkp.comment('boostrap style')
          link(rel:"stylesheet", href:"bootstrap/css/bootstrap.min.css")
        }
        body() {
          div(class: "container") {
            h1(opt.concept)
            generate(opt.definition, builder, opt.definition.archetypeId)
          }
        }
      }
      
      return "<!DOCTYPE html>\n" + writer.toString()
   }
   
   void generate(ObjectNode o, MarkupBuilder b, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id
      
      // support for non ELEMENT.value fields that are in the OPT
      // TODO: support for IM fields that are not in the OPT like INSTRUCTION.narrative
      if (datavalues.contains(o.rmTypeName))
      {
         generateFields(o, b, parent_arch_id)
         return
      }
      
      if (o.rmTypeName == "ELEMENT")
      {
         // constraints for ELEMENT.name and ELEMENT.value, can be null
         // uses the first alternative (these are single attributes and can have alternative constraints)
         def name = o.attributes.find { it.rmAttributeName == 'name' }?.children?.getAt(0)
         def value = o.attributes.find { it.rmAttributeName == 'value' }?.children?.getAt(0)

         //println "element name "+ opt.getTerm(parent_arch_id, o.nodeId)
         
         b.div(class:o.rmTypeName) {
            
            if (name) generateFields(name, b, parent_arch_id)
            else
            {
               label( opt.getTerm(parent_arch_id, o.nodeId) )
            }
            
            if (value) generateFields(value, b, parent_arch_id)
         }

         return
      }
      
      if (o.type == "ARCHETYPE_SLOT")
      {
         b.div(class: o.rmTypeName) {
            label("ARCHETYPE_SLOT is not supported yet")
         }
         return // Generator do not support slots on OPTs
      }
      
      // ***********************************************************************
      //
      // Here we need to avoid processing attributes that should be set
      // internally (not by a user) like ACTIVITY.action_archetype_id or
      // INSTRUCTION_DETAILS.instruction_id or INSTRUCTION_DETAILS.activity_id
      // and expose openEHR IM attributes that are not in the OPT, like
      // ACTION.time or INSTRUCTION.expiry_tyime or INSTRUCTION.narrative
      //
      // ***********************************************************************
      
      // Process all non-ELEMENTs
      b.div(class: o.rmTypeName) {
         
         // label for intermediate nodes
         label( opt.getTerm(parent_arch_id, o.nodeId) )
         
         //println o.path
         
         o.attributes.each { attr ->
            
            // Sample avoid ACTIVITY.action_archetype_id
            // This can be done in a generic way by adding a mapping rmTypeName -> rmAttributeNames
            if (o.rmTypeName == 'ACTIVITY' && attr.rmAttributeName == 'action_archetype_id') return
            if (o.rmTypeName == 'COMPOSITION' && attr.rmAttributeName == 'category') return
            if (o.rmTypeName == 'ACTION' && attr.rmAttributeName == 'ism_transition') return
            
            generate(attr, b, parent_arch_id)
         }
      }
   }
   
   void generate(AttributeNode a, MarkupBuilder b, String parent_arch_id)
   {
      a.children.each {
         generate(it, b, parent_arch_id)
      }
   }
   
   void generateFields(ObjectNode node, MarkupBuilder builder, String parent_arch_id)
   {
      switch (node.rmTypeName)
      {
        case 'DV_TEXT':
           builder.input(type:'text', class: node.rmTypeName +' form-control', name:node.path)
        break
        case 'DV_CODED_TEXT':

           //println "DV_CODED_TEXT "+ node.xmlNode
           
           def constraint = node.attributes.find{ it.rmAttributeName == 'defining_code' }.children[0]
           
           /*
           println constraint // ObjectNode
           println constraint.xmlNode
           constraint.xmlNode.children().each { println it.name() }
           */
          
           if (constraint.rmTypeName == "CODE_PHRASE")
           {
              // is a ConstraintRef?
              if (constraint.terminologyRef)
              {
                 builder.div(class: 'input-group') {
                    input(type:'text', name: constraint.path, class: node.rmTypeName +' form-control')
                    i(class:'input-group-addon glyphicon glyphicon-search', '')
                 }
              }
              else // constraint is CCodePhrase
              {
                 builder.select(name: constraint.path, class: node.rmTypeName +' form-control') {
                 
                    //println "terminolgy id "+ constraint.xmlNode.terminology_id.value.text()
                    
                    option(value:'', '')
                    
                    if (constraint.xmlNode.terminology_id.value.text() == 'local')
                    {
                       constraint.xmlNode.code_list.each { code_node ->
                       
                          option(value:code_node.text(), opt.getTerm(parent_arch_id, code_node.text()))
                       }
                       
                       if (constraint.xmlNode.code_list.isEmpty()) println "Empty DV_CODED_TEXT.defining_code constraint "+ parent_arch_id +"/"+ constraint.path
                    }
                    else // terminology openehr
                    {
                       constraint.xmlNode.code_list.each { code_node ->
                          
                          option(value:code_node.text(), terminology.getRubric(opt.langCode, code_node.text()))
                       }
                    }
                 }
              }
           }
           else throw Exception("coded text constraint not supported "+ constraint.rmTypeName)
           
           
           /*
           if (constraint instanceof ConstraintRef)
           {
              def term = a.ontology.constraintDefinition(locale, constraint.reference)
              builder.input(type:'text', name:constraint.path)
              i(class:'search', '')
           }
           */
           
        break
        case 'DV_QUANTITY':
           builder.input(type:'text', name:node.path+'/magnitude', class: node.rmTypeName +' form-control')
           
           builder.select(name:node.path+'/units', class: node.rmTypeName +' form-control') {
              
              node.xmlNode.list.units.each { u ->
              
                 option(value:u.text(), u.text())
              }
           }
        break
        case 'DV_COUNT':
           builder.input(type:'number', class: node.rmTypeName +' form-control', name:node.path)
        break
        case 'DV_ORDINAL':
           
           // ordinal.value // int
           // ordinal.symbol // DvCodedText
           // ordinal.symbol.codeString
           // ordinal.symbol.terminologyId
           
           builder.select(name:node.path, class: node.rmTypeName +' form-control') {
              
              option(value:'', '')
              
              node.xmlNode.list.each { ord ->
              
                 option(value:ord.value.text(), opt.getTerm(parent_arch_id, ord.symbol.defining_code.code_string.text()))
              }
           }
        break
        case 'DV_DATE':
           builder.input(type:'date', name:node.path, class: node.rmTypeName +' form-control')
        break
        case 'DV_DATE_TIME':
           builder.input(type:'datetime-local', name:node.path, class: node.rmTypeName +' form-control')
        break
        case 'DV_BOOLEAN':
           builder.input(type:'checkbox', name:node.path, class: node.rmTypeName)
        break
        case 'DV_DURATION':
           builder.label('D') {
             input(type:'number', name:node.path+'/D', class:'small '+ node.rmTypeName +' form-control')
           }
           builder.label('H') {
             input(type:'number', name:node.path+'/H', class:'small '+ node.rmTypeName +' form-control')
           }
           builder.label('M') {
             input(type:'number', name:node.path+'/M', class:'small '+ node.rmTypeName +' form-control')
           }
           builder.label('S') {
             input(type:'number', name:node.path+'/S', class:'small '+ node.rmTypeName +' form-control')
           }
        break
        case 'DV_PROPORTION':
           builder.label('numerator') {
             input(type:'number', name:node.path+'/numerator', class:'small '+ node.rmTypeName +' form-control')
           }
           builder.label('denominator') {
             input(type:'number', name:node.path+'/denominator', class:'small '+ node.rmTypeName +' form-control')
           }
        break
        default: // TODO: generar campos para los DV_INTERVAL
           println "Datatype "+ node.rmTypeName +" not supported yet"
      }
   }
}
