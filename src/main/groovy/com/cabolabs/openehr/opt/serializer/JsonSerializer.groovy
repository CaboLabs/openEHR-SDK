package com.cabolabs.openehr.opt.serializer

import groovy.json.JsonBuilder

import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.opt.model.primitive.*
import com.cabolabs.openehr.opt.model.domain.*
import com.cabolabs.openehr.opt.model.datatypes.*


/**
 * @author pablo.pazos@cabolabs.com
 * Transforms an OPT into a JSON string
 */
class JsonSerializer {

   def builder = new JsonBuilder()
   Map root

   Map serialize(OperationalTemplate opt)
   {
      def n = [
         language:    opt.language,
         concept:     opt.concept,
         purpose:     opt.purpose,
         uid:         opt.uid,
         template_id: opt.templateId
      ]

      if (!root) root = n

      n.definition = serialize(opt.definition)

      return n
   }

   Map serialize(ObjectNode obn)
   {
      def n = [
         archetype_id: obn.archetypeId,
         path:         obn.path,
         type:         obn.type,
         rm_type_name: obn.rmTypeName,
         node_id:      obn.nodeId,
         text:         obn.text,
         description:  obn.description,
         _class:       obn.getClass().getSimpleName(),
         attributes:   []
      ]

      // object could also be the root if the client wants to get a json from a subtree of the OPT
      if (!root) root = n

      obn.attributes.each {
         n.attributes << serialize(it)
      }

      return n
   }

   Map serialize(AttributeNode atn)
   {
      def n = [
         type:              atn.type,
         rm_attribute_name: atn.rmAttributeName,
         children:          []
      ]

      atn.children.each {
         n.children << serialize(it)
      }

      return n
   }

   // Other types of nodes
   Map serialize(CCodePhrase obn)
   {
      def n = [
         archetype_id: obn.archetypeId,
         path:         obn.path,
         type:         obn.type,
         rm_type_name: obn.rmTypeName,
         node_id:      obn.nodeId,
         text:         obn.text,
         description:  obn.description,
         _class:       obn.getClass().getSimpleName(),
         attributes:   []
      ]

      if (obn.reference) n.reference = obn.reference

      if (!obn.terminologyRef)
      {
         n.terminology_id = obn.terminologyId
         n.code_list = []

         obn.codeList.each {
            n.code_list << it
         }
      }
      else
      {
         n.referenceSetUri = obn.terminologyRef
      }

      obn.attributes.each {
         n.attributes << serialize(it)
      }

      return n
   }

   Map serialize(CDvOrdinal obn)
   {
      def n = [
         archetype_id: obn.archetypeId,
         path:         obn.path,
         type:         obn.type,
         rm_type_name: obn.rmTypeName,
         node_id:      obn.nodeId,
         text:         obn.text,
         description:  obn.description,
         _class:       obn.getClass().getSimpleName(),
         attributes:   []
      ]

      n.list = []


      obn.list.each {
         n.list << serialize(it)
      }


      obn.attributes.each {
         n.attributes << serialize(it)
      }

      return n
   }
   Map serialize(CDvOrdinalItem obn)
   {
      def n = [
         value: obn.value,
         symbol: obn.symbol ? serialize(obn.symbol) : null
      ]

      return n
   }

   Map serialize(CDvQuantity obn)
   {
      def n = [
         archetype_id: obn.archetypeId,
         path:         obn.path,
         type:         obn.type,
         rm_type_name: obn.rmTypeName,
         node_id:      obn.nodeId,
         text:         obn.text,
         description:  obn.description,
         _class:       obn.getClass().getSimpleName(),
         attributes:   []
      ]

      n.list = []

      obn.list.each {
         n.list << serialize(it)
      }


      obn.attributes.each {
         n.attributes << serialize(it)
      }

      return n
   }
   Map serialize(CQuantityItem obn)
   {
      def n = [
         magnitude: obn.magnitude ? serialize(obn.magnitude) : null,
         precision: obn.precision ? serialize(obn.precision) : null,
         units: obn.units
      ]

      return n
   }


   Map serialize(PrimitiveObjectNode obn)
   {
      //println "pobn "+ obn.type +" "+ obn.rmTypeName
      def n = [
         archetype_id: obn.archetypeId,
         path:         obn.path,
         type:         obn.type,
         rm_type_name: obn.rmTypeName,
         node_id:      obn.nodeId,
         text:         obn.text,
         description:  obn.description,
         _class:       obn.getClass().getSimpleName()
      ]

      // checking for any allowed == no constraint
      if (obn.item)
      {
         n.item = serialize(obn.item)
      }

      return n
   }


   // CPrimitives
   Map serialize(CInteger cp)
   {
      def n = [:]

      if (cp.range)
         n.range = serialize(cp.range)
      else
         n.list = cp.list

      return n
   }
   Map serialize(CReal cp)
   {
      def n = [
         range: serialize(cp.range)
      ]

      return n
   }
   Map serialize(CDuration cp)
   {
      def n = [:]

      if (cp.range) n.range = serialize(cp.range)
      else n.pattern = cp.pattern

      return n
   }
   Map serialize(CDateTime cp)
   {
      def n = [
         pattern: cp.pattern
      ]

      return n
   }
   Map serialize(CDate cp)
   {
      def n = [
         pattern: cp.pattern
      ]

      return n
   }
   Map serialize(CString cp)
   {
      def n = [:]

      if (cp.pattern) n.pattern = cp.pattern
      else
      {
         n.list = []
         cp.list.each {
            n.list << it
         }
      }

      return n
   }
   Map serialize(CBoolean cp)
   {
      return [:]
   }


   // Intervals
   Map serialize(IntervalInt iv)
   {
      def n = [
         lower_unbounded: iv.lowerUnbounded,
         upper_unbounded: iv.upperUnbounded
      ]

      if (!iv.lowerUnbounded) n.lower = iv.lower
      if (!iv.upperUnbounded) n.upper = iv.upper

      return n
   }

   Map serialize(IntervalBigDecimal iv)
   {
      def n = [
         lower_unbounded: iv.lowerUnbounded,
         upper_unbounded: iv.upperUnbounded
      ]

      if (!iv.lowerUnbounded) n.lower = iv.lower
      if (!iv.upperUnbounded) n.upper = iv.upper

      return n
   }

   Map serialize(IntervalDuration iv)
   {
      def n = [
         lower_unbounded: iv.lowerUnbounded,
         upper_unbounded: iv.upperUnbounded
      ]

      if (!iv.lowerUnbounded) n.lower = iv.lower.value
      if (!iv.upperUnbounded) n.upper = iv.upper.value

      return n
   }

   Map serialize(CodePhrase c)
   {
      def n = [
         codeString: c.codeString,
         terminologyId: c.terminologyId
      ]

      return n
   }


   String get(boolean pretty_print = false)
   {
      builder(root)
      if (pretty_print) return builder.toPrettyString()

      return builder.toString()
      //root = null
   }
}
