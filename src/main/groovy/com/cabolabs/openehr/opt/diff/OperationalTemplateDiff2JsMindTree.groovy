package com.cabolabs.openehr.opt.diff

import com.cabolabs.openehr.opt.model.*
import groovy.json.*

// Serializes an OPT diff instance into a JsMind Tree object
// https://github.com/hizzgdev/jsmind/blob/master/docs/en/1.usage.md#12-data-format
class OperationalTemplateDiff2JsMindTree {

   Map getJsMindTree(OperationalTemplateDiff diff)
   {
      def out = [
         meta: [
            name: 'CaboLabs openEHR Toolkit',
            author: 'info@cabolabs.com',
            version: '0.1'
         ]
      ]

      // adds the entries to the out map
      out.data = traverseNodeDiff(diff.root)

      return out
   }

   String getJsMindTreeString(OperationalTemplateDiff diff)
   {
      def out = getJsMindTree(diff)
      return JsonOutput.toJson(out)
   }

   def traverseNodeDiff(NodeDiff node)
   {
      def res = [
         id: node.templateDataPath,
         //topic: '<div align="center">'+ cobject.text +'<br/><span style="font-size: 0.8em">&lt;'+ cobject.rmTypeName +'&gt;</span></div>',
         topic: node.templateDataPath, // TODO: get info from corresponding node
         children: [],
         'background-color': '#4e73df',
         'foreground-color': '#fff'
      ]

      switch (node.compareResult)
      {
         case "added":
            res['background-color'] = '#43DF73'
         break
         case "removed":
            res['background-color'] = '#DF734E'
         break
         //default: // same
      }

      def attr_node
      node.attributeDiffs.each { attr, nodes ->

         attr_node = [
            id:    node.templateDataPath +"/"+ attr, // TODO: check this is unique
            topic: '<div align="center">'+ attr +'</div>',
            children: [],
            'background-color': 'rgb(133, 135, 150)',
            'foreground-color': '#fff'
         ]

         nodes.each { child_node ->

            attr_node.children << traverseNodeDiff(child_node)
         }
         //res.children.addAll(traverseAttribute(cattr))

         res.children << attr_node
      }

      return res
   }
}