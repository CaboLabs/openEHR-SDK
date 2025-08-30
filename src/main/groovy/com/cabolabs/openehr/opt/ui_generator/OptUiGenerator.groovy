package com.cabolabs.openehr.opt.ui_generator

import com.cabolabs.openehr.opt.model.AttributeNode
import com.cabolabs.openehr.opt.model.ObjectNode
import com.cabolabs.openehr.opt.model.OperationalTemplate
import com.cabolabs.openehr.terminology.TerminologyParser
import groovy.xml.MarkupBuilder

import java.util.jar.JarFile

@groovy.util.logging.Slf4j
class OptUiGenerator {

   OperationalTemplate opt
   TerminologyParser terminology

   // Options
   boolean fullPage = true // true=generates html, head and body, false=just the form
   int bootstrapVersion = 5 // only 4 or 5 bootstrap forms are supported

   private static List datavalues = [
      'DV_TEXT', 'DV_CODED_TEXT', 'DV_QUANTITY', 'DV_COUNT',
      'DV_ORDINAL', 'DV_DATE', 'DV_DATE_TIME', 'DV_PROPORTION',
      'DV_DURATION'
   ]

   private static List avoidDisplaying = [
      'COMPOSITION', 'HISTORY', 'ITEM_TREE', 'ITEM_LIST', 'ITEM_SINGLE'
   ]


   OptUiGenerator(boolean fullPage = false, int bootstrapVersion = 4)
   {
      this.fullPage = fullPage
      this.bootstrapVersion = bootstrapVersion
   }

   String generate(OperationalTemplate opt)
   {
      this.opt = opt
      this.terminology = TerminologyParser.getInstance()

      // FIXME: load all resources in folder
      // https://www.logicbig.com/how-to/java/list-all-files-in-resouce-folder.html
      // TODO: make the terminology loading dynamic: try parsing all the files in the terminology folder
      terminology.parseTerms(getClass().getResourceAsStream("/terminology/openehr_terminology_en.xml")) // this works to load the resource from the jar
      terminology.parseTerms(getClass().getResourceAsStream("/terminology/openehr_terminology_es.xml"))
      terminology.parseTerms(getClass().getResourceAsStream("/terminology/openehr_terminology_pt.xml"))


      def writer = new StringWriter()
      def builder = new MarkupBuilder(writer)
      builder.setDoubleQuotes(true) // Use double quotes on attributes


      // Generates HTML while traversing the archetype tree
      if (this.fullPage)
      {
         builder.mkp.yieldUnescaped '<!doctype html>\n'
         builder.html(lang: opt.langCode) {
            head() {
               meta(name: "viewport", content: "width=device-width, initial-scale=1")

               // mkp.comment('simple style')
               // link(rel:"stylesheet", href:"/static/style.css")

               style('''
               .form-item {
                  padding-left: 1em;
                  margin-bottom: 0.5em;
               }
               body {
                 padding: 1rem;
               }
               .small {
                  width: 48px;
               }
               .search {
                  background-image: url("http://files.softicons.com/download/system-icons/crystal-project-icons-by-everaldo-coelho/png/32x32/apps/search.png");
                  background-size: 16px 16px;
                  background-repeat: no-repeat;
                  width: 16px;
                  height: 16px;
                  display: inline-block;
               }
               .form-group {
                  border-left: 1px solid #ccc;
               }
               ''')

               mkp.comment('boostrap style')
               if (this.bootstrapVersion == 5)
               {
                  link(rel:"stylesheet", href:"https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css")
                  link(rel:"stylesheet", href:"https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css")
               }
               else
               {
                  link(rel:"stylesheet", href:"https://cdn.jsdelivr.net/npm/bootstrap@4.6.2/dist/css/bootstrap.min.css")
                  link(rel:"stylesheet", href:"https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css")
               }
            }
            body() {
               form(class: 'container') {
                  h1(class: 'h3', opt.getTerm(opt.definition.archetypeId, opt.definition.nodeId))
                  input(type: 'hidden', name: 'template_id', value: opt.templateId)
                  generate(opt.definition, builder, opt.definition.archetypeId)
               }
            }
         }
      }
      else
      {
         builder.div() {
            style('''
               .form-item {
                  padding-left: 1em;
                  margin-bottom: 0.5em;
               }
               form {
                 padding: 1rem;
               }
               .form-group {
                  border-left: 1px solid #ccc;
               }
            ''')
            form() {
               h1(class: 'h3', opt.getTerm(opt.definition.archetypeId, opt.definition.nodeId))
               input(type: 'hidden', name: 'template_id', value: opt.templateId)
               generate(opt.definition, builder, opt.definition.archetypeId)
            }
         }
      }

      return writer.toString()
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


      // classes form BS4
      String formGroupClass = 'form-group'
      String fieldLabelClass = ''
      String fieldClass = 'form-control'
      if (this.bootstrapVersion == 5)
      {
         formGroupClass = 'md-3'
         fieldLabelClass = 'form-label'
      }


      if (o.rmTypeName == "ELEMENT")
      {
         // constraints for ELEMENT.name and ELEMENT.value, can be null
         // uses the first alternative (these are single attributes and can have alternative constraints)
         def name = o.attributes.find { it.rmAttributeName == 'name' }?.children?.getAt(0)
         def value = o.attributes.find { it.rmAttributeName == 'value' }?.children?.getAt(0)

         //println "element name "+ opt.getTerm(parent_arch_id, o.nodeId)

         b.div(class: o.rmTypeName +' '+ formGroupClass +' form-item', 'data-tpath': o.templatePath) {

            if (name) generateFields(name, b, parent_arch_id)
            else
            {
               //label(class: fieldLabelClass, opt.getTerm(parent_arch_id, o.nodeId))
               label(class: fieldLabelClass, o.ownerArchetypeRoot.getText(o.nodeId))
            }

            if (value) generateFields(value, b, parent_arch_id)
         }

         return
      }

      if (o.type == "ARCHETYPE_SLOT")
      {
         b.div(class: o.rmTypeName +' form-item', 'data-tpath': o.templatePath) {
            label(class: fieldLabelClass, "ARCHETYPE_SLOT is not supported yet, found at "+ o.path)
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


      // TODO: support attributes of the RM that are not defined in the template like EVENT.time


      // Process all non-ELEMENTs
      if (avoidDisplaying.contains(o.rmTypeName))
      {
         // TODO: refactor below uses the same code
         o.attributes.each { attr ->

            // Sample avoid ACTIVITY.action_archetype_id
            // This can be done in a generic way by adding a mapping rmTypeName -> rmAttributeNames
            if (o.rmTypeName == 'ACTIVITY' && attr.rmAttributeName == 'action_archetype_id') return
            if (o.rmTypeName == 'COMPOSITION' && attr.rmAttributeName == 'category') return
            if (o.rmTypeName == 'ACTION' && attr.rmAttributeName == 'ism_transition') return

            generate(attr, b, parent_arch_id)
         }
      }
      else
      {
         b.div(class: o.rmTypeName +' form-item', 'data-tpath': o.templatePath) {

            // label for intermediate nodes
            //def term = opt.getTerm(parent_arch_id, o.nodeId)
            def term = o.ownerArchetypeRoot.getText(o.nodeId)

            if (term)
            {
               label(class: fieldLabelClass, term)
            }

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
   }

   void generate(AttributeNode a, MarkupBuilder b, String parent_arch_id)
   {
      a.children.each {
         generate(it, b, parent_arch_id)
      }
   }

   // TODO: refactor in different functions
   // Generates fields for an ELEMENT.value object node
   void generateFields(ObjectNode node, MarkupBuilder builder, String parent_arch_id)
   {
      // classes form BS4
      String formGroupClass = 'form-group'
      String fieldLabelClass = ''
      String fieldClass = 'form-control'
      if (this.bootstrapVersion == 5)
      {
         formGroupClass = 'md-3'
         fieldLabelClass = 'form-label'
      }


      builder.div(class: node.rmTypeName +' '+ formGroupClass +' form-item', 'data-tpath': node.templatePath) {

         switch (node.rmTypeName)
         {
            // adds the DV attribute names to the template path
            case 'DV_TEXT':
               def aValue = node.attributes.find{ it.rmAttributeName == 'value' }
               if (aValue)
               {
                  // TODO: support alternative constraints for the value
                  // TODO: all elements here are for bs4, need to check if DOM changes for bs5
                  if (aValue.children)
                  {
                     if (aValue.children[0].item.pattern)
                     {
                        // Tadd an input with a help that contains the pattern
                        input(
                           type:             'text',
                           class:            node.rmTypeName +' '+ fieldClass,
                           'data-tpath':     node.templatePath +'/value',
                           'data-archetype': node.getOwnerArchetypeId(),
                           'data-path':      node.path +'/value'
                        )
                        span(
                           class: 'form-text text-muted',
                           aValue.children[0].item.pattern
                        )
                     }
                     else if (aValue.children[0].item.list)
                     {
                        if (aValue.children[0].item.list.size() == 1)
                        {
                           input(
                              type:             'text',
                              class:            node.rmTypeName +' '+ fieldClass,
                              'data-tpath':     node.templatePath +'/value',
                              'data-archetype': node.getOwnerArchetypeId(),
                              'data-path':      node.path +'/value',
                              value:            aValue.children[0].item.list[0]
                           )
                        }
                        else
                        {
                           // radio button group
                           for (String item: aValue.children[0].item.list)
                           {
                              // FIXME: this is BS4
                              div(class: 'form-check') {
                                 input(
                                    type:             'radio',
                                    class:            node.rmTypeName +' '+ fieldClass +' form-check-input',
                                    'data-tpath':     node.templatePath +'/value',
                                    'data-archetype': node.getOwnerArchetypeId(),
                                    'data-path':      node.path +'/value',
                                    value:            item,
                                    name:             node.templatePath +'/value' // FIXME: also the occurrence # should be used to prevent duplicated names in different groups
                                 )
                                 label(class: 'form-check-label', item)
                              }
                           }
                        }
                     }
                  }
               }
               else
               {
                  builder.textarea(
                     class:            node.rmTypeName +' '+ fieldClass, // FIXME: this should be the type of the DV_TEXT.value attribute not the DV_TEXT
                     'data-tpath':     node.templatePath + '/value',
                     'data-archetype': node.getOwnerArchetypeId(),
                     'data-path':      node.path +'/value',
                     ''
                  )
               }
            break
            case 'DV_CODED_TEXT':

               def attr = node.attributes.find{ it.rmAttributeName == 'defining_code' }

               if (!attr)
               {
                  builder.div(class: 'row') {
                     div(class: 'col') {
                        label('value')
                        input(
                           type:'text',
                           'data-tpath':     node.templatePath +'/value',
                           'data-archetype': node.getOwnerArchetypeId(),
                           'data-path':      node.path +'/value',
                           class:            ' '+ node.rmTypeName +' '+ fieldClass
                        )
                     }
                     div(class: 'col') {
                        label('code')
                        input(
                           type:'text',
                           'data-tpath':     node.templatePath +'/defining_code/code_string',
                           'data-archetype': node.getOwnerArchetypeId(),
                           'data-path':      node.path +'/defining_code/code_string',
                           class:            ' '+ node.rmTypeName +' '+ fieldClass
                        )
                     }
                     div(class: 'col') {
                        label('terminology')
                        input(
                           type:             'text',
                           'data-tpath':     node.templatePath +'/defining_code/terminology_id',
                           'data-archetype': node.getOwnerArchetypeId(),
                           'data-path':      node.path +'/defining_code/terminology_id',
                           class:            ' '+ node.rmTypeName +' '+ fieldClass
                        )
                     }
                  }
               }
               else
               {
                  def constraint = attr.children[0]

                  if (constraint.rmTypeName == "CODE_PHRASE")
                  {
                     // is a ConstraintRef?
                     if (constraint.terminologyRef)
                     {
                        //def attachmentStyle = (this.bootstrapVersion == 5) ? ' : 'input-group-append'

                        builder.div(class: 'input-group') {
                           input(
                              type:             'text',
                              class:            node.rmTypeName +' '+ fieldClass,
                              'data-tpath':     constraint.templatePath,
                              'data-archetype': node.getOwnerArchetypeId(),
                              'data-path':      constraint.path
                           )
                           // FIXME: use fontawesome5 icons for BS4 or BS5 icons
                           if (this.bootstrapVersion == 4)
                           {
                              div(class: 'input-group-append') {
                                 span(class: 'input-group-text') {
                                    i(class: 'bi bi-search', '')
                                 }
                              }
                           }
                           else
                           {
                              span(class: 'input-group-text') {
                                 i(class: 'bi bi-search', '')
                              }
                           }
                        }
                     }
                     else // constraint is CCodePhrase
                     {
                        builder.select(
                           class:            node.rmTypeName +' '+ fieldClass,
                           'data-tpath':     constraint.templatePath,
                           'data-archetype': node.getOwnerArchetypeId(),
                           'data-path':      constraint.path
                           ) {

                           option(value:'', '')

                           if (constraint.terminologyId == 'local')
                           {
                              constraint.codeList.each { code_node ->
                                 // FIXME: it's not enough to know the archetype ID: if there are two object constraints for the same
                                 //        archetype in the template, and those have different constraints for a coded text node,
                                 //        then this method won't find the code if it's defined in the second object constraint, it only
                                 //        finds the first one. I think we need to ask the current node's parent archetype root and not
                                 //        the OPT for the term.
                                 //option(value:code_node, opt.getTerm(parent_arch_id, code_node))
                                 option(value:code_node, node.ownerArchetypeRoot.getText(code_node))
                              }

                              // FIXME: constraint can be by code list or by terminology reference. For term ref we should have a search control, not a select
                              if (constraint.codeList.size() == 0) log.warning("Empty DV_CODED_TEXT.defining_code constraint "+ parent_arch_id + constraint.templatePath)
                           }
                           else if (constraint.terminologyId == 'openehr')
                           {
                              constraint.codeList.each { code_node ->
                                 option(value:code_node, terminology.getRubric(opt.langCode, code_node))
                              }
                           }
                           else
                           {
                              // TODO: add a dummy search control for external terminologies
                           }
                        }
                     }
                  }
                  else throw Exception("coded text constraint not supported "+ constraint.rmTypeName)
               }


            break
            case 'DV_QUANTITY':

               builder.div(class: 'row') {
                  div(class: 'col-3') {
                     label('magnitude')
                     input(
                        class: node.rmTypeName +' '+ fieldClass,
                        type:'number',
                        'data-tpath':     node.templatePath +'/magnitude',
                        'data-archetype': node.getOwnerArchetypeId(),
                        'data-path':      node.path +'/magnitude'
                     )
                  }
                  div(class: 'col-3') {
                     label('units')
                     if (node.list.size() == 0)
                     {
                        input(
                           class: node.rmTypeName +' '+ fieldClass,
                           type:'text',
                           'data-tpath':     node.templatePath+ '/units',
                           'data-archetype': node.getOwnerArchetypeId(),
                           'data-path':      node.path +'/units'
                        )
                     }
                     else
                     {
                        select(
                           class:            node.rmTypeName +' '+ fieldClass,
                           'data-tpath':     node.templatePath +'/units',
                           'data-archetype': node.getOwnerArchetypeId(),
                           'data-path':      node.path +'/units'
                        ) {

                           option(value:'', '')
                           node.list.units.each { u ->

                              option(value:u, u)
                           }
                        }
                     }
                  }
               }

            break
            case 'DV_COUNT':
               builder.input(
                  class:            node.rmTypeName +' '+ fieldClass,
                  type:             'number',
                  'data-tpath':     node.templatePath,
                  'data-archetype': node.getOwnerArchetypeId(),
                  'data-path':      node.path
               )
            break
            case 'DV_ORDINAL':

               // ordinal.value // int
               // ordinal.symbol // DvCodedText
               // ordinal.symbol.codeString
               // ordinal.symbol.terminologyId

               builder.select(
                  class:            node.rmTypeName +' '+ fieldClass,
                  'data-tpath':     node.templatePath,
                  'data-archetype': node.getOwnerArchetypeId(),
                  'data-path':      node.path
               ) {

                  option(value:'', '')

                  node.list.each { ord ->

                     //option(value:ord.value, opt.getTerm(parent_arch_id, ord.symbol.codeString))
                     option(value:ord.value, node.ownerArchetypeRoot.getText(ord.symbol.codeString))
                  }
               }
            break
            case 'DV_TIME':
               builder.input(
                  class:            node.rmTypeName +' '+ fieldClass,
                  type:             'time',
                  'data-tpath':     node.templatePath,
                  'data-archetype': node.getOwnerArchetypeId(),
                  'data-path':      node.path
               )
            break
            case 'DV_DATE':
               builder.input(
                  class:            node.rmTypeName +' '+ fieldClass,
                  type:             'date',
                  'data-tpath':     node.templatePath,
                  'data-archetype': node.getOwnerArchetypeId(),
                  'data-path':      node.path
               )
            break
            case 'DV_DATE_TIME':
               builder.input(
                  class:            node.rmTypeName +' '+ fieldClass,
                  type:             'datetime-local',
                  'data-tpath':     node.templatePath,
                  'data-archetype': node.getOwnerArchetypeId(),
                  'data-path':      node.path
               )
            break
            case 'DV_BOOLEAN': // TODO: fix form check classes for BS4 and BS5
               builder.input(
                  class:            node.rmTypeName,
                  type:             'checkbox',
                  'data-tpath':     node.templatePath,
                  'data-archetype': node.getOwnerArchetypeId(),
                  'data-path':      node.path
               )
            break
            case 'DV_DURATION':
               builder.div(class:'row') {
                  div(class:'col') {
                     label('D')
                     input(
                        type:'number',
                        'data-tpath':     node.templatePath +'/D',
                        'data-archetype': node.getOwnerArchetypeId(),
                        'data-path':      node.path +'/D',
                        class: node.rmTypeName +' '+ fieldClass)
                  }
                  div(class:'col') {
                     label('H')
                     input(
                        type:'number',
                        'data-tpath':     node.templatePath +'/H',
                        'data-archetype': node.getOwnerArchetypeId(),
                        'data-path':      node.path +'/H',
                        class: node.rmTypeName +' '+ fieldClass)
                  }
                  div(class:'col') {
                     label('M')
                     input(
                        type:'number',
                        'data-tpath':     node.templatePath +'/M',
                        'data-archetype': node.getOwnerArchetypeId(),
                        'data-path':      node.path +'/M',
                        class: node.rmTypeName +' '+ fieldClass)
                  }
                  div(class:'col') {
                     label('S')
                     input(
                        type:'number',
                        'data-tpath':     node.templatePath +'/S',
                        'data-archetype': node.getOwnerArchetypeId(),
                        'data-path':      node.path +'/S',
                        class: node.rmTypeName +' '+ fieldClass)
                  }
               }
            break
            case 'DV_PROPORTION':
               builder.div(class:'row') {
                  div(class:'col-3') {
                     label('numerator')
                     input(
                        type:             'number',
                        'data-tpath':     node.templatePath +'/numerator',
                        'data-archetype': node.getOwnerArchetypeId(),
                        'data-path':      node.path +'/numerator',
                        class:            node.rmTypeName +' '+ fieldClass
                     )
                  }
                  div(class:'col-3') {
                     label('denominator')
                     input(
                        type:             'number',
                        'data-tpath':     node.templatePath +'/denominator',
                        'data-archetype': node.getOwnerArchetypeId(),
                        'data-path':      node.path +'/denominator',
                        class:            node.rmTypeName +' '+ fieldClass
                     )
                  }
               }
            break
            case 'DV_IDENTIFIER':
               builder.div(class:'row') {
                  div(class:'col') {
                     label(class: '', 'issuer')
                     input(
                        type:'text',
                        'data-tpath':     node.templatePath +'/issuer',
                        'data-archetype': node.getOwnerArchetypeId(),
                        'data-path':      node.path +'/issuer',
                        class:            node.rmTypeName +' '+ fieldClass
                     )
                  }
                  div(class:'col') {
                     label(class: '', 'assigner')
                     input(
                        type:'text',
                        'data-tpath':     node.templatePath +'/assigner',
                        'data-archetype': node.getOwnerArchetypeId(),
                        'data-path':      node.path +'/assigned',
                        class:            node.rmTypeName +' '+ fieldClass
                     )
                  }
                  div(class:'col') {
                     label(class: '', 'id')
                     input(
                        type:             'text',
                        'data-tpath':     node.templatePath +'/id',
                        'data-archetype': node.getOwnerArchetypeId(),
                        'data-path':      node.path +'/id',
                        class:            node.rmTypeName +' '+ fieldClass
                     )
                  }
                  div(class:'col') {
                     label(class: '', 'type')
                     input(
                        type:             'text',
                        'data-tpath':     node.templatePath +'/type',
                        'data-archetype': node.getOwnerArchetypeId(),
                        'data-path':      node.path +'/type',
                        class:            node.rmTypeName +' '+ fieldClass
                     )
                  }
               }
            break
            case 'DV_MULTIMEDIA':
               builder.input(
                  class:            node.rmTypeName,
                  type:             'file',
                  'data-tpath':     node.templatePath,
                  'data-archetype': node.getOwnerArchetypeId(),
                  'data-path':      node.path
               )
            break
            case 'DV_PARSABLE':
               builder.textarea(
                  class: node.rmTypeName +' '+ fieldClass,
                  'data-tpath':     node.templatePath,
                  'data-archetype': node.getOwnerArchetypeId(),
                  'data-path':      node.path,
                  ''
               )
            break
            case 'DV_URI':
               builder.input(
                  class:            node.rmTypeName +' '+ fieldClass,
                  type:             'text',
                  'data-tpath':     node.templatePath,
                  'data-archetype': node.getOwnerArchetypeId(),
                  'data-path':      node.path
               )
            break
            default: // TODO: generar campos para los DV_INTERVAL
               log.info("Datatype "+ node.rmTypeName +" not supported yet")
         }
      }

   }
}
