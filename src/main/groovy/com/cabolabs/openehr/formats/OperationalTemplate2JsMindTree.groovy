package com.cabolabs.openehr.formats

import com.cabolabs.openehr.opt.model.*
import groovy.json.*

// Serializes an OPT instance into a JsMind Tree object
// https://github.com/hizzgdev/jsmind/blob/master/docs/en/1.usage.md#12-data-format
class OperationalTemplate2JsMindTree {

   def opt

   Map getJsMindTree(OperationalTemplate opt)
   {
      def out = [
         meta: [
            name: 'CaboLabs openEHR Toolkit',
            author: 'info@cabolabs.com',
            version: '0.1'
         ],
         format: 'node_tree',
         data: [:]
         /* [
            id: opt.definition.templateDataPath,
            topic: opt.concept,
            children: []
         ]
         */
      ]

      traverseObject(opt.definition, out.data)

      return out
   }

   String getJsMindTreeString(OperationalTemplate opt)
   {
      def out = getJsMindTree(opt)
      return JsonOutput.toJson(out)
   }

   def traverseObject(ObjectNode obj, Map parent)
   {
      def node = [
         // the '_object' avoids to have the same id as the attr when the obj doesn't have node_id
         id:       'object_'+ obj.templateDataPath,
         topic:    obj.text,
         children: []
      ]

      // I'm the root obj?
      if (parent.size() == 0)
      {
         parent << node // adds the map entries to the out map
      }
      // Parent is an attribute
      else
      {
         parent.children << node // adds the node in the children list
      }

      obj.attributes.each { attr ->
         traverseAttribute(attr, node)
      }
   }

   def traverseAttribute(AttributeNode attr, Map parent)
   {
      def node = [
         id:       attr.templateDataPath,
         topic:    attr.rmAttributeName,
         children: []
      ]

      parent.children << node
      
      attr.children.each { obj ->
         traverseObject(obj, node)
      }
   }
}