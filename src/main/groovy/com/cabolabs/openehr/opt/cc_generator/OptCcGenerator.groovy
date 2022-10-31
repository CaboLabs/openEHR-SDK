package com.cabolabs.openehr.opt.cc_generator

import com.cabolabs.openehr.opt.model.AttributeNode
import com.cabolabs.openehr.opt.model.ObjectNode
import com.cabolabs.openehr.opt.model.OperationalTemplate
import com.cabolabs.openehr.terminology.TerminologyParser
import groovy.xml.MarkupBuilder

import java.util.jar.JarFile

class OptCcGenerator {

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
      this.terminology = TerminologyParser.getInstance()

      terminology.parseTerms(getClass().getResourceAsStream(PS +"terminology"+ PS +"openehr_terminology_en.xml")) // this works to load the resource from the jar
      terminology.parseTerms(getClass().getResourceAsStream(PS +"terminology"+ PS +"openehr_terminology_es.xml"))
      terminology.parseTerms(getClass().getResourceAsStream(PS +"terminology"+ PS +"openehr_terminology_pt.xml"))


      def writer = new StringWriter()
      def builder = new MarkupBuilder(writer)
      builder.setDoubleQuotes(true) // Use double quotes on attributes


      // cCube form parameters
      def pgWidth = 600
      def pgHeight = 600
      def frmId = 'FRM056' //Doesnt matter but valid and will be reassigned by cCube if already used
      def pgId = 'PG23269' //Doesnt matter but valid and will be reassigned by cCube if already used

      // Generates HTML while traversing the archetype tree
      builder.EFORMV3(){
         TBLFORMS() {
            FORMID (frmId) 
            TEMPLATEID {}
            FORMNAME (opt.concept)
            CREATEDBY {}
            CREATEDDATE (new java.text.SimpleDateFormat("dd/MM/yyyy").format(new Date()))
            MODIFYDATE (new java.text.SimpleDateFormat("dd/MM/yyyy").format(new Date()))
            FONTNAME_F ('Arial')
            FONTSIZE_F ('9')
            PAGEWIDTH (pgWidth) // Need to calculate these based on number of controls in final version
            PAGEHEIGHT (pgHeight) // Need to calculate these based on number of controls in final version
            FORECOLOUR ('16777216')
            BACKCOLOUR ('-1')
            AUTORETAIN_F ('true')
            AUTOSAVE ('false')
            FORMDSN {}
            SETFORNEWUSE {}
            SETFOREXISTUSER {}
            INVALIDUSER {}
            PUBLISHPATH {}
            PDFFILENAME {}
            PDFLOCATION {}
         }
         TBLPAGES() {
            PAGEID (pgId)
            FORMID (frmId)
            PAGENAME(frmId + '_page')
            HELPTEXT {}
            FORECOLOUR ('16777216')
            BACKCOLOUR ('-1')
            FONTNAME ('Arial')
            FONTSIZE ('9')
            WIDTH (pgWidth) // Need to calculate these based on number of controls in final version
            HEIGHT (pgHeight) // Need to calculate these based on number of controls in final version
            PAGESLNO ('1')
            BACKGROUNDURL {}
            NOTES {}
            SHOWTEMPLATE ('true')
            SETIMPORTID {}
            PAGETYPE ('General')
            AUTORETAIN ('true')
            PAGESIZETYPE ('Custom')
            READONLY ('false')
            BACKSHADING ('Clear')
            SHADINGCOLOUR ('-16777216')
         }
         // TBLPAGECONTROLCOMMONPROPERTIES - one per control including seperate ones for labels
         // Need a way of passing back need for validation elements
         generate(opt.definition, builder, opt.definition.archetypeId, 2064, pgId, frmId)
      }

      return writer.toString()
   }

   void generate(ObjectNode o, MarkupBuilder b, String parent_arch_id, optId, String pgId, String frmId)
   {
      //println "generate Called"
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id

      // support for non ELEMENT.value fields that are in the OPT
      // TODO: support for IM fields that are not in the OPT like INSTRUCTION.narrative
      // if (datavalues.contains(o.rmTypeName))
      // {
      //    generateFields(o, b, parent_arch_id)
      //    return
      // }

      // Calculated position controls
      def normOptId = optId - 2064
   	def ctrlX = 13
      def ctrlY = 9 + (normOptId * 50)


      if (o.rmTypeName == "ELEMENT")
      {
         // constraints for ELEMENT.name and ELEMENT.value, can be null
         // uses the first alternative (these are single attributes and can have alternative constraints)
         def name = o.attributes.find { it.rmAttributeName == 'name' }?.children?.getAt(0)
         def value = o.attributes.find { it.rmAttributeName == 'value' }?.children?.getAt(0)

         println "element name "+ opt.getTerm(parent_arch_id, o.nodeId)
         println "name variable = " + name
         println "value variable = " + value

         

         if (name) {
            //generateFields(name, b, parent_arch_id, optId, pgId, frmId)
         }
         else
         {
            b.TBLPAGECONTROLCOMMONPROPERTIES() {
               // Label
               OPTIONID (optId)
               PAGEID (pgId)
               TEMPLATEID {}
               FORMID (frmId)
               CONTROLID('Label' + (optId-2063))
               CONTROLTYPE ('8')
               PARENTCONTROLID (pgId)
               TEXT(opt.getTerm(parent_arch_id, o.nodeId))
               XPOSITION (ctrlX)
               YPOSITION (ctrlY)
               FONTNAME ('Arial')
               FONTSIZE ('9')
               BOLD ('false')
               ITALIC('false')
               UNDERLINE('false')
               STRIKEOUT('false')
               FORECOLOUR ('16777216')
               BACKCOLOUR ('-1')
               BORDERCOLOUR ('-16777216')
               BORDERSTYLE('NotSet')
               BORDERWIDTH('0')
               HEIGHT('23')
               WIDTH('120') // will need changing depending on text size
               HOLDVALUE('false')
               READONLY ('false')
               TABINDEX(normOptId)
               VISIBLE ('true')
               DISPLAY ('true')
               HELPTEXT {}
               TOOLTIP {}
               CSSSTYLE('eformLabel ')
               BACKSHADING('Clear')
               SHADINGCOLOUR ('-16777216')
               ZINDEX('0')
            }

            if (value) {
               b.TBLPAGECONTROLCOMMONPROPERTIES() {
                  generateFields(value, b, parent_arch_id, optId, pgId, frmId)
               }
            }
         }

         return
      }

      if (o.type == "ARCHETYPE_SLOT")
      {
         // b.div(class: o.rmTypeName +'  form-item') {
         //    label("ARCHETYPE_SLOT is not supported yet, found at "+ o.path)
         // }
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
      

      // label for intermediate nodes
      def term = opt.getTerm(parent_arch_id, o.nodeId)

      //println o.path

      o.attributes.each { attr ->

         // Sample avoid ACTIVITY.action_archetype_id
         // This can be done in a generic way by adding a mapping rmTypeName -> rmAttributeNames
         if (o.rmTypeName == 'ACTIVITY' && attr.rmAttributeName == 'action_archetype_id') return
         if (o.rmTypeName == 'COMPOSITION' && attr.rmAttributeName == 'category') return
         if (o.rmTypeName == 'ACTION' && attr.rmAttributeName == 'ism_transition') return

         generate(attr, b, parent_arch_id, optId, pgId, frmId)
      }
   }

   void generate(AttributeNode a, MarkupBuilder b, String parent_arch_id, optionId, String pgId, String frmId)
   {
      a.children.each {
         generate(it, b, parent_arch_id, optionId++, pgId, frmId)
      }
   }

   // TODO: refactor in different functions
   void generateFields(ObjectNode node, MarkupBuilder b, String parent_arch_id, optId, String pgId, String frmId)
   {
      switch (node.rmTypeName)
      {
         case 'DV_TEXT':
            //builder.textarea(class: node.rmTypeName +' form-control', name:node.path, '')
            println "DV_TEXT"
            b() {
               OPTIONID (optId)
               PAGEID (pgId)
               TEMPLATEID {}
            }
         break
         // case 'DV_CODED_TEXT':

         //    def constraint = node.attributes.find{ it.rmAttributeName == 'defining_code' }.children[0]

         //    if (constraint.rmTypeName == "CODE_PHRASE")
         //    {
         //       // is a ConstraintRef?
         //       if (constraint.terminologyRef)
         //       {
         //          builder.div(class: 'input-group') {
         //             input(type:'text', name: constraint.path, class: node.rmTypeName +' form-control')
         //             i(class:'input-group-addon glyphicon glyphicon-search', '')
         //          }
         //       }
         //       else // constraint is CCodePhrase
         //       {
         //          builder.select(name: constraint.path, class: node.rmTypeName +' form-control') {

         //             option(value:'', '')

         //             if (constraint.terminologyId == 'local')
         //             {
         //                constraint.codeList.each { code_node ->
         //                   option(value:code_node, opt.getTerm(parent_arch_id, code_node))
         //                }

         //                // FIXME: constraint can be by code list or by terminology reference. For term ref we should have a search control, not a select
         //                if (constraint.codeList.size() == 0) println "Empty DV_CODED_TEXT.defining_code constraint "+ parent_arch_id + constraint.path
         //             }
         //             else // terminology openehr
         //             {
         //                constraint.codeList.each { code_node ->
         //                   option(value:code_node, terminology.getRubric(opt.langCode, code_node))
         //                }
         //             }
         //          }
         //       }
         //    }
         //    else throw Exception("coded text constraint not supported "+ constraint.rmTypeName)

         // break
         // case 'DV_QUANTITY':

         //    builder.div(class:'col-md-5')
         //    {
         //       input(type:'number', name:node.path+'/magnitude', class: node.rmTypeName +' form-control')
         //    }
         //    builder.div(class:'col-md-5')
         //    {
         //       if (node.list.size() == 0)
         //       {
         //          input(type:'text', name:node.path+'/units', class: node.rmTypeName +' form-control')
         //       }
         //       else
         //       {
         //          select(name:node.path+'/units', class: node.rmTypeName +' form-control') {

         //             option(value:'', '')
         //             node.list.units.each { u ->

         //                option(value:u, u)
         //             }
         //          }
         //       }
         //    }
         // break
         // case 'DV_COUNT':
         //    builder.input(type:'number', class: node.rmTypeName +' form-control', name:node.path)
         // break
         // case 'DV_ORDINAL':

         //   // ordinal.value // int
         //   // ordinal.symbol // DvCodedText
         //   // ordinal.symbol.codeString
         //   // ordinal.symbol.terminologyId

         //   builder.select(name:node.path, class: node.rmTypeName +' form-control') {

         //      option(value:'', '')

         //      node.list.each { ord ->

         //         option(value:ord.value, opt.getTerm(parent_arch_id, ord.symbol.codeString))
         //      }
         //   }
         // break
         // case 'DV_TIME':
         //    builder.input(type:'time', name:node.path, class: node.rmTypeName +' form-control')
         // break
         // case 'DV_DATE':
         //    builder.input(type:'date', name:node.path, class: node.rmTypeName +' form-control')
         // break
         // case 'DV_DATE_TIME':
         //    builder.input(type:'datetime-local', name:node.path, class: node.rmTypeName +' form-control')
         // break
         // case 'DV_BOOLEAN':
         //    builder.input(type:'checkbox', name:node.path, class: node.rmTypeName)
         // break
         // case 'DV_DURATION':
         //    builder.label('D') {
         //       input(type:'number', name:node.path+'/D', class:'small '+ node.rmTypeName +' form-control')
         //    }
         //    builder.label('H') {
         //       input(type:'number', name:node.path+'/H', class:'small '+ node.rmTypeName +' form-control')
         //    }
         //    builder.label('M') {
         //       input(type:'number', name:node.path+'/M', class:'small '+ node.rmTypeName +' form-control')
         //    }
         //    builder.label('S') {
         //       input(type:'number', name:node.path+'/S', class:'small '+ node.rmTypeName +' form-control')
         //    }
         // break
         // case 'DV_PROPORTION':
         //    builder.label('numerator') {
         //       input(type:'number', name:node.path+'/numerator', class:'small '+ node.rmTypeName +' form-control')
         //    }
         //    builder.label('denominator') {
         //       input(type:'number', name:node.path+'/denominator', class:'small '+ node.rmTypeName +' form-control')
         //    }
         // break
         // case 'DV_IDENTIFIER':
         //    builder.label('issuer') {
         //       input(type:'text', name:node.path+'/issuer', class:'small '+ node.rmTypeName +' form-control')
         //    }
         //    builder.label('assigner') {
         //       input(type:'text', name:node.path+'/assigner', class:'small '+ node.rmTypeName +' form-control')
         //    }
         //    builder.label('id') {
         //       input(type:'text', name:node.path+'/id', class:'small '+ node.rmTypeName +' form-control')
         //    }
         //    builder.label('type') {
         //       input(type:'text', name:node.path+'/type', class:'small '+ node.rmTypeName +' form-control')
         //    }
         // break
         // case 'DV_MULTIMEDIA':
         //    builder.input(type: 'file', name: node.path, class: node.rmTypeName)
         // break
         // case 'DV_PARSABLE':
         //    builder.textarea(class: node.rmTypeName +' form-control', name:node.path, '') 
         // break
         // case 'DV_URI':
         //    builder.input(type: 'text', class: node.rmTypeName +' form-control', name:node.path)
         // break
         default: // TODO: generar campos para los DV_INTERVAL
            println "Datatype "+ node.rmTypeName +" not supported yet"
      }
   }
}
