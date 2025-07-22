package com.cabolabs.openehr.opt.serializer

import groovy.xml.MarkupBuilder

import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.opt.model.primitive.*
import com.cabolabs.openehr.opt.model.domain.*
import com.cabolabs.openehr.opt.model.datatypes.*

@groovy.util.logging.Slf4j
class OptXmlSerializer {

   def writer
   def builder
   boolean pretty

   public OptXmlSerializer()
   {
      this(false)
   }

   public OptXmlSerializer(boolean pretty)
   {
      this.pretty = pretty
   }

   String serialize(OperationalTemplate opt)
   {
      // Important to have the initialization here because if the same serializer
      // is used twice, it accumulares the results from the previous serialization!
      this.writer = new StringWriter()

      if (this.pretty)
      {
         this.builder = new MarkupBuilder(this.writer)
      }
      else
      {
         this.builder = new MarkupBuilder(new IndentPrinter(new PrintWriter(this.writer), "", false))
      }

      this.builder.setDoubleQuotes(true)


      // Begin serializing the opt

      builder.template(
         xmlns:'http://schemas.openehr.org/v1',
         'xmlns:xsd':'http://www.w3.org/2001/XMLSchema',
         'xmlns:xsi':'http://www.w3.org/2001/XMLSchema-instance'
      )
      {
         language {
            terminology_id {
               value(opt.language.split('::')[0])
            }
            code_string(opt.language.split('::')[1])
         }
         description {
            // TODO: we don't parse the description elements!
            original_author(id: 'Original Author', 'Not Specified') // TODO: get value from OPT
            lifecycle_state()
            details {
               language {
                  terminology_id {
                     value(opt.language.split('::')[0])
                  }
                  code_string(opt.language.split('::')[1])
               }
               purpose(opt.purpose)
            }
         }
         if (opt.uid)
         {
            uid {
               value(opt.uid)
            }
         }
         template_id {
            value(opt.templateId)
         }
         concept(opt.concept)
         definition {
            serialize(opt.definition)
         }
      }

      return this.writer.toString()
   }

   // common for all object nodes
   void fillObjectNode(ObjectNode obn)
   {
      builder.rm_type_name(obn.rmTypeName)

      builder.occurrences {
         serialize(obn.occurrences)
      }

      // node_id is mandatory in the XSD even if the value is empty
      // NOTE: if the nodeId is null, <node_id /> is generated
      //       but if it's empty string <node_id></node_id> is generated!
      builder.node_id(obn.nodeId ?: null)
   }

   void serialize(ObjectNode obn)
   {
      fillObjectNode(obn)

      obn.attributes.each {
         serialize(it)
      }

      if (obn.archetypeId)
      {
         builder.archetype_id {
            value(obn.archetypeId)
         }
      }

      if (!obn.parent)
      {
         builder.template_id {
            value(obn.owner.templateId)
         }
      }

      obn.termDefinitions.each { termdef ->
         builder.term_definitions(code: termdef.code) {
            items(id: 'text', termdef.term.text)
            items(id: 'description', termdef.term.description)
         }
      }
   }
   void serialize(ArchetypeSlot obn)
   {
      fillObjectNode(obn)

      if (obn.includes)
      {
         builder.includes {
            expression('xsi:type': 'EXPR_BINARY_OPERATOR') {
               type('Boolean')
               operator('2007') // matches
               precedence_overridden('false')
               left_operand('xsi:type': 'EXPR_LEAF') {
                  type('String')
                  item('xsi:type': 'xsd:string', 'archetype_id/value')
                  reference_type('attribute')
               }
               right_operand('xsi:type': 'EXPR_LEAF') {
                  type('C_STRING')
                  item('xsi:type': 'C_STRING') {
                     pattern(obn.includes)
                  }
                  reference_type('constraint')
               }
            }
         }
      }

      if (obn.excludes)
      {
         builder.excludes {
            expression('xsi:type': 'EXPR_BINARY_OPERATOR') {
               type('Boolean')
               operator('2007') // matches
               precedence_overridden('false')
               left_operand('xsi:type': 'EXPR_LEAF') {
                  type('String')
                  item('xsi:type': 'xsd:string', 'archetype_id/value')
                  reference_type('attribute')
               }
               right_operand('xsi:type': 'EXPR_LEAF') {
                  type('C_STRING')
                  item('xsi:type': 'C_STRING') {
                     pattern(obn.excludes)
                  }
                  reference_type('constraint')
               }
            }
         }
      }
   }

   void serialize(AttributeNode atn)
   {
      builder.attributes('xsi:type': atn.type)
      {
         rm_attribute_name(atn.rmAttributeName)

         existence {
            serialize(atn.existence)
         }

         atn.children.each { obn ->
            children('xsi:type': obn.type) {
               serialize(obn)
            }
         }

         if (atn.type == 'C_MULTIPLE_ATTRIBUTE')
         {
            cardinality {
               serialize(atn.cardinality)
            }
         }
      }
   }

   // Other types of nodes
   void serialize(CCodePhrase obn)
   {
      fillObjectNode(obn)

      if (obn.terminologyRef) // type=C_CODE_REFERENCE
      {
         builder.referenceSetUri(obn.terminologyRef)
      }
      else if (obn.reference) // type=CONSTARINT_REF
      {
         builder.reference(obn.reference)
      }
      else // C_CODE_PHRASE
      {
         builder.terminology_id {
            value(obn.terminologyId)
         }
         obn.codeList.each { item ->
            builder.code_list(item)
         }
      }

      assert !obn.attributes
   }

   void serialize(CDvOrdinal obn)
   {
      fillObjectNode(obn)

      obn.list.each { item ->
         builder.list {
            serialize(item) // CDvOrdinalItem
         }
      }

      assert !obn.attributes
   }

   void serialize(CDvOrdinalItem item)
   {
      builder.value(item.value)
      builder.symbol {
         value() // this is required by the XSD
         defining_code {
            terminology_id {
               value(item.symbol.terminologyId)
            }
            code_string(item.symbol.codeString)
         }
      }
   }

   void serialize(CDvQuantity obn)
   {
      fillObjectNode(obn)

      // the OPT might not have property!
      if (obn.property)
      {
         builder.property {
            terminology_id {
               value(obn.property.terminologyId)
            }
            code_string(obn.property.codeString)
         }
      }

      obn.list.each { item -> // CQuantityItem
         builder.list {
            if (item.magnitude)
            {
               magnitude {
                  serialize(item.magnitude)
               }
            }
            if (item.precision)
            {
               precision {
                  serialize(item.precision)
               }
            }
            if (item.units)
            {
               units(item.units)
            }
         }
      }

   }

   String classToOpenEhrType(Object obj)
   {
      String clazz = obj.getClass().getSimpleName()
      clazz.replaceAll("[A-Z]", '_$0').toUpperCase().replaceAll( /^_/, '')
   }

   void serialize(PrimitiveObjectNode obn)
   {
      fillObjectNode(obn)

      if (obn.item)
      {
         builder.item('xsi:type': classToOpenEhrType(obn.item)) {
            serialize(obn.item)
         }
      }
      else
      {
         log.warning("NO ITEM FOR "+ obn.path)
      }
   }

   // CPrimitives
   void serialize(CInteger cp)
   {
      if (cp.range)
      {
         builder.range {
            serialize(cp.range)
         }
      }
      else
      {
         cp.list.each { item ->
            builder.list(item)
         }
      }
   }

   void serialize(CReal cp)
   {
      if (cp.range)
      {
         builder.range {
            serialize(cp.range)
         }
      }
   }

   void serialize(CDuration cp)
   {
      if (cp.pattern)
      {
         builder.pattern(cp.pattern)
      }
      else if (cp.range)
      {
         builder.range {
            serialize(cp.range)
         }
      }
   }

   void serialize(CDateTime cp)
   {
      if (cp.pattern)
      {
         builder.pattern(cp.pattern)
      }
   }

   void serialize(CDate cp)
   {
      if (cp.pattern)
      {
         builder.pattern(cp.pattern)
      }
   }

   void serialize(CString cp)
   {
      if (cp.pattern)
      {
         builder.pattern(cp.pattern)
      }
      else
      {
         cp.list.each { item ->
            builder.list(item)
         }
      }
   }

   void serialize(CBoolean cp)
   {
      builder.true_valid(cp.trueValid)
      builder.false_valid(cp.falseValid)
   }

   void serialize(Cardinality c)
   {
      builder.is_ordered(c.isOrdered)
      builder.is_unique(c.isUnique)
      builder.interval {
         serialize(c.interval)
      }
   }

   // Intervals
   void serialize(IntervalInt iv)
   {
      builder.lower_included(iv.lowerIncluded)
      builder.upper_included(iv.upperIncluded)
      builder.lower_unbounded(iv.lowerUnbounded)
      builder.upper_unbounded(iv.upperUnbounded)

      if (!iv.lowerUnbounded)
      {
         builder.lower(iv.lower)
      }
      if (!iv.upperUnbounded)
      {
         builder.upper(iv.upper)
      }
   }

   void serialize(IntervalBigDecimal iv)
   {
      builder.lower_included(iv.lowerIncluded)
      builder.upper_included(iv.upperIncluded)
      builder.lower_unbounded(iv.lowerUnbounded)
      builder.upper_unbounded(iv.upperUnbounded)

      if (!iv.lowerUnbounded)
      {
         builder.lower(iv.lower)
      }
      if (!iv.upperUnbounded)
      {
         builder.upper(iv.upper)
      }
   }

   void serialize(IntervalDuration iv)
   {
      builder.lower_included(iv.lowerIncluded)
      builder.upper_included(iv.upperIncluded)
      builder.lower_unbouded(iv.lowerUnbounded)
      builder.upper_unbounded(iv.upperUnbounded)

      if (!iv.lowerUnbounded)
      {
         builder.lower(iv.lower)
      }
      if (!iv.upperUnbounded)
      {
         builder.upper(iv.upper)
      }
   }

   void serialize(CodePhrase c)
   {
      builder.terminology_id {
         value(c.terminologyId)
      }
      builder.code_string(c.codeString)
   }
}