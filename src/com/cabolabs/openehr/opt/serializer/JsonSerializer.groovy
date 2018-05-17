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

      if (obn.reference) n.reference = obn.reference

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
   Map serialize(CCodePhrase obn)
   {
      def n = [
         archetype_id: obn.archetypeId,
         path:         obn.path,
         type:         obn.type,
         rm_type_name: obn.rmTypeName,
         node_id:      obn.nodeId,
         _class:       obn.getClass().getSimpleName(),
         attributes:   []
      ]

      if (!obn.terminologyRef)
      {
         n.terminology_id = obn.terminologyIdName
         n.code_list = []

         if (obn.terminologyIdVersion) n.terminology_id += '('+ obn.terminologyIdVersion +')'

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


   Map serialize(PrimitiveObjectNode obn)
   {
      println "pobn "+ obn.type +" "+ obn.rmTypeName
      def n = [
         archetype_id: obn.archetypeId,
         path:         obn.path,
         type:         obn.type,
         rm_type_name: obn.rmTypeName,
         node_id:      obn.nodeId,
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
         lower: iv.lower,
         upper: iv.upper
      ]

      return n
   }
   Map serialize(IntervalFloat iv)
   {
      def n = [
         lower: iv.lower,
         upper: iv.upper
      ]

      return n
   }
   Map serialize(IntervalDuration iv)
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
