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
   }

   private Locatable parse_locatable(Map flat_map)
   {
      // 1. cargar OPT asociado a la raiz
      // 2. crear objeto del RM que va a ser el raiz que devuelvo
      // 3. crear metodos attribute_builder a los que le paso todas las paths que tiene como raiz ese atributo, y les paso el objeto padre para setear los atributos
      // 4. los attribute_builders se van a llamar recursivamente para los atributos de los atributos de la clase raiz
      // 5. para saber el tipo concreto de un atributo es necesario consultar el OPT por el node_id en caso de que hayan distintos hermanos con distintos tipos como alternativa
      // 6. para los nodos multiples, la ruta va a tener el indice incluido, para saber el nombre del atributo hay que sacar el indice pero para saber qué rutas van juntas para seguir la recursion es necesario pasar todas las rutas que tengan la misma raiz, incluyendo el indice

   }

   private Version parse_version(Map flat_map)
   {


   }

   private EhrDto parse_ehr(Map flat_map)
   {


   }
}