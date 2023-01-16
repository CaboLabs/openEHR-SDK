package com.cabolabs.openehr.formats

import com.cabolabs.openehr.rm_1_0_2.common.change_control.Version
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.*
import com.cabolabs.openehr.dto_1_0_2.ehr.EhrDto

class FlatMapParser {

   public Object parse(Map flat_map)
   {
      if (!flat_map._type)
      {
         throw new Exception("Key '_type' is required")
      }

      switch (flat_map._type)
      {
         case ['COMPOSITION', 'EHR_STATUS', 'FOLDER']: // TODO: demographic locatables

         break
         case ['ORIGINAL_VERSION', 'IMPORTED_VERSION']:
           // TODO:
         break
         case 'EHR':
            // TODO:
         break
         default:
            throw new Exception("Type ${flat_map._type} not supported")
      }

      // TODO:
      return null
   }

   private Locatable parse_locatable(Map flat_map)
   {
      // 1. cargar OPT asociado a la raiz
      // 2. crear objeto del RM que va a ser el raiz que devuelvo
      // 3. crear metodos attribute_builder a los que le paso todas las paths que tiene como raiz ese atributo, y les paso el objeto padre para setear los atributos
      // 4. los attribute_builders se van a llamar recursivamente para los atributos de los atributos de la clase raiz
      // 5. para saber el tipo concreto de un atributo es necesario consultar el OPT por el node_id en caso de que hayan distintos hermanos con distintos tipos como alternativa
      // 6. para los nodos multiples, la ruta va a tener el indice incluido, para saber el nombre del atributo hay que sacar el indice pero para saber qué rutas van juntas para seguir la recursion es necesario pasar todas las rutas que tengan la misma raiz, incluyendo el indice

      // TODO:
      return null
   }

   private Version parse_version(Map flat_map)
   {
      // keys are dataPaths

      // 1. [/a[atNNNN](0)/b[atMMMM]/c/d, ...] => [[a[atNNNN](0), b[atMMMM], c, d], ...] // transforms a list of paths in a list of path tokens
      def tokens = flat_map.keySet().collect{ it.split('/').tail() }

      // 2. => [[[a, atNNNN, (0)], [b, atMMMM], [c], [d]], ...] // list of path tokens, each path tokens are broken into path token parts
      tokens = tokens.collect{ it*.split("[\\[\\]]") }

      // 3. => [[[a, atNNNN, 0], [b, atMMMM], [c], [d]], ...] // goes through each path tokens parts, checking for an index part and removes the parentheses leaving just the index
      tokens = tokens.each { tokens_parts -> // path tokens parts is a list of all the tokens of the same path, already transformed to path parts
         tokens_parts.collect{
            if (it.last() ==~ /\(\d+\)/)
            {
               it[it.size()-1] = it.last().split("[\\(\\)]").last()
            }
            it
         }
      }

      // 4. => [[[att: a, nid: atNNNN, idx: 0], [att: b, nid: atMMMM], [att: c], [att: d]], ...] // adds attribute names for each part of the path token parts
      def attr_name, map

      tokens = tokens.collect { tokens_parts ->

         tokens_parts = tokens_parts.collect{ parts ->

            // list to map with attribute keys
            map = (parts as List).withIndex().collectEntries{ part, index ->

               if (index == 0) attr_name = 'att'
               else if (index == 1)
               {
                  if (part.isNumber()) attr_name = 'idx'
                  else attr_name = 'nid'
               }
               else attr_name = 'idx'

               [(attr_name): part]
            }

            return map
         }

         return tokens_parts
      }

      // TODO: with the root type and the path tokens with the keys included we can reconstruct to object

      // TODO:
      return null
   }

   private EhrDto parse_ehr(Map flat_map)
   {

      // TODO:
      return null

   }
}