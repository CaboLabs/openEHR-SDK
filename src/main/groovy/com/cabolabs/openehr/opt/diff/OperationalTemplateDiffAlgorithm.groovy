package com.cabolabs.openehr.opt.diff

import com.cabolabs.openehr.opt.model.OperationalTemplate
import com.cabolabs.openehr.opt.model.ObjectNode

// helps on the result of the OPT diff algorithm
class OperationalTemplateDiffAlgorithm {

   def diff(OperationalTemplate opt1, OperationalTemplate opt2)
   {
      def paths1 = opt1.nodes.values().flatten().collect{ it.templateDataPath }
      def paths2 = opt2.nodes.values().flatten().collect{ it.templateDataPath }

      def shared_paths = paths1.intersect(paths2) //, { p1, p2 -> p1 <=> p2 }) // closure was added in groovy 2.5.0
      def added_paths = paths2.minus(shared_paths)
      def removed_paths = paths1.minus(paths2)
      def all_paths = paths1.plus(paths2).unique().sort()

      return build_diff_tree(opt1, opt2, all_paths, shared_paths, added_paths, removed_paths)
   }

   OperationalTemplateDiff build_diff_tree(OperationalTemplate opt1, OperationalTemplate opt2, List all_paths, List shared_paths, List added_paths, List removed_paths)
   {
      def opt_diff = new OperationalTemplateDiff(
         compared: opt1,
         to: opt2
      )

      def root_diff = build_diff_tree_recursive(opt1, opt2, null, all_paths.head(), all_paths.tail()*.split("(?=/)"), all_paths, shared_paths, added_paths, removed_paths)

      // link the diff tree into the opt_diff
      opt_diff.root = root_diff

      // testing
      //println groovy.json.JsonOutput.toJson(root_diff)

      return opt_diff
   }

   NodeDiff build_diff_tree_recursive(OperationalTemplate opt1, OperationalTemplate opt2, String parent_path, String current_root, List children_paths, List all_paths, List shared_paths, List added_paths, List removed_paths)
   {
      // calculates the full path from the tree root to the current root
      // avoids adding double // at the start
      def path = parent_path ? (parent_path == '/' ? current_root : parent_path + current_root) : current_root

      // println ""
      // println "PATH"
      // println path

      def opt_node

      def compareResult
      if (shared_paths.contains(path))
      {
         compareResult = 'same'
         opt_node = opt1.getNodesByTemplateDataPath(path)[0]
      }
      else if (added_paths.contains(path))
      {
         compareResult = 'added'
         opt_node = opt2.getNodesByTemplateDataPath(path)[0]
      }
      else if (removed_paths.contains(path))
      {
         compareResult = 'removed'
         opt_node = opt1.getNodesByTemplateDataPath(path)[0]
      }

      // println opt_node ? opt_node.class : 'null' // CCodePhrase, ObjectNode, ArchetypeSlot, CDvQuantity, PrimitiveObjectNode
      // assert opt_node instanceof ObjectNode
      // println opt_node.attributes*.rmAttributeName

      def node = new NodeDiff(
         templateDataPath: path,
         compareResult: compareResult
      )

      def grouped = children_paths.groupBy{ it[0] }

      // println "GROUPED"
      // println grouped

      def tmp, next_paths, attribute_name
      grouped.each { next_root, rest ->
      
         /*
         println "NEXT ROOT"
         println next_root
         
         println "REST"
         println rest
         */

         next_paths = []
    
         rest.each { child_children ->
         
            tmp = child_children as List // rest is a String[]
            
            tmp.remove(next_root) // removes the root element
            
            if (tmp) // empty list is the node that was the parent
            {         
               next_paths << tmp
            }
         }
         
         attribute_name = opt_node.attributes*.rmAttributeName.find { next_root.startsWith('/'+it) } // next_root = /category(1)
         
         // continue recursion building tree for children
         if (!node.attributeDiffs[attribute_name]) node.attributeDiffs[attribute_name] = []
         node.attributeDiffs[attribute_name] << build_diff_tree_recursive(opt1, opt2, path, next_root, next_paths, all_paths, shared_paths, added_paths, removed_paths)
      }

      return node
   }
}
