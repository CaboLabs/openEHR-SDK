package com.cabolabs.openehr.query.saqm

import com.cabolabs.openehr.query.OpenEhrQuery
import com.cabolabs.openehr.query.saqm.condition.*

class SAQMQuery extends OpenEhrQuery {

   String uid

   String name
   String description

   List projections
   SAQMCondition where

   boolean isCount = false

   /**
    * Retrieves a list of simple conditions from the where, helps analyzing the query.
    */
   List<SAQMConditionSimple> getSimpleConditions()
   {
      def queue = new LinkedList()

      def list = []

      queue.offer(this.where)

      def cond

      while (!queue.isEmpty())
      {
         cond = queue.poll()

         if (cond instanceof SAQMConditionSimple)
         {
            list << cond
         }
         else if (cond instanceof SAQMConditionAnd)
         {
            queue.offer(cond.c1)
            queue.offer(cond.c2)
         }
         // TODO: OR
         // TODO: NOT
      }

      return list
   }
}