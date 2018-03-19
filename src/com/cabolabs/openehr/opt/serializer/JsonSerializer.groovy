package com.cabolabs.openehr.opt.serializer

import groovy.json.JsonBuilder

import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.opt.model.primitive.*
import com.cabolabs.openehr.opt.model.domain.*

class JsonSerializer {

   def builder = new JsonBuilder()
   Map root

   Map serialize(OperationalTemplate opt)
   {
      //println "opt"
      def n = [
         language:    opt.language,
         concept:     opt.concept,
         purpose:     opt.purpose,
         uid:         opt.uid,
         template_id: opt.templateId,
         definition:  serialize(opt.definition)
      ]

      if (!root) root = n

      return n
   }

   Map serialize(ObjectNode obn)
   {
      //println "obn"
      def n = [
         archetype_id: obn.archetypeId,
         path:         obn.path,
         type:         obn.type,
         rm_type_name: obn.rmTypeName,
         node_id:      obn.nodeId,
         _class:       obn.getClass().getSimpleName(),
         attributes:   []
      ]

      obn.attributes.each {
         n.attributes << serialize(it)
      }

      return n
   }

   Map serialize(AttributeNode atn)
   {
      //println "atn"
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
   Map serialize(PrimitiveObjectNode obn)
   {
      //println "obn"
      def n = [
         archetype_id: obn.archetypeId,
         path:         obn.path,
         type:         obn.type,
         rm_type_name: obn.rmTypeName,
         node_id:      obn.nodeId,
         _class:       obn.getClass().getSimpleName(),
         item:         serialize(obn.item)
      ]

      return n
   }


   // CPrimitives
   Map serialize(CInteger cp)
   {
      def n = [
         range: serialize(cp.range)
      ]

      return n
   }


   // Intervals
   Map serialize(IntervalInt iv)
   {
      def n = [
         lower: iv.lower,
         upper: iv.upper
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
