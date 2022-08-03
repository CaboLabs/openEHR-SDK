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
         ]
      ]

      // adds the entries to the out map
      out.data = traverseObject(opt.definition)

      return out
   }

   String getJsMindTreeString(OperationalTemplate opt)
   {
      def out = getJsMindTree(opt)
      return JsonOutput.toJson(out)
   }

   def traverseObject(ObjectNode cobject)
   {
      // def node = [
      //    // the '_object' avoids to have the same id as the attr when the obj doesn't have node_id
      //    id:       'object_'+ obj.templateDataPath,
      //    topic:    obj.text,
      //    children: []
      // ]

      def res = [
         id: cobject.templateDataPath,
         topic: '<div align="center">'+ cobject.text +'<br/><span style="font-size: 0.8em">&lt;'+ cobject.rmTypeName +'&gt;</span></div>',
         children: [],
         'background-color': '#4e73df',
         'foreground-color': '#fff'
      ]

      cobject.attributes.each { cattr ->

         res.children.addAll(traverseAttribute(cattr))
      }

      return res
   }

   def traverseAttribute(AttributeNode cattr)
   {
      def res = [
         id:    'attr_'+ cattr.templateDataPath,
         topic: '<div align="center">'+ cattr.rmAttributeName +'</div>',
         children: [],
         'background-color': 'rgb(133, 135, 150)',
         'foreground-color': '#fff'
      ]

      cattr.children.each { cobj ->

         res.children << traverseObject(cobj)
      }

      return res
   }
}