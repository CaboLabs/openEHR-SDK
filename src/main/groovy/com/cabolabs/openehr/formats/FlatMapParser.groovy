package com.cabolabs.openehr.formats

import com.cabolabs.openehr.rm_1_0_2.Model
import com.cabolabs.openehr.rm_1_0_2.ehr.*
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.*
import com.cabolabs.openehr.rm_1_0_2.common.generic.*
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.*
import com.cabolabs.openehr.rm_1_0_2.common.change_control.*
import com.cabolabs.openehr.rm_1_0_2.common.directory.Folder
import com.cabolabs.openehr.rm_1_0_2.composition.*
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.*
import com.cabolabs.openehr.rm_1_0_2.composition.content.navigation.Section
import com.cabolabs.openehr.rm_1_0_2.data_structures.history.*
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.*
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.*
import com.cabolabs.openehr.rm_1_0_2.data_types.basic.*
import com.cabolabs.openehr.rm_1_0_2.data_types.encapsulated.*
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.*
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.*
import com.cabolabs.openehr.rm_1_0_2.data_types.text.*
import com.cabolabs.openehr.rm_1_0_2.data_types.uri.*
import com.cabolabs.openehr.rm_1_0_2.support.identification.*
import com.cabolabs.openehr.rm_1_0_2.demographic.*
import com.cabolabs.openehr.dto_1_0_2.ehr.EhrDto
import com.cabolabs.openehr.dto_1_0_2.common.change_control.ContributionDto

class FlatMapParser {

   public Object parse(Map flat_map)
   {
      if (!flat_map['/_type'])
      {
         throw new Exception("Key '_type' is required")
      }

      Object out

      switch (flat_map['/_type'])
      {
         case ['COMPOSITION', 'EHR_STATUS', 'FOLDER']: // TODO: demographic locatables
            out = parse_locatable(flat_map)
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

      return out
   }

   private String openEHRClassToClassname(String clazz)
   {
      clazz.toLowerCase().split('_')*.capitalize().join('')
   }

   private Locatable parse_locatable(Map flat_map)
   {
      // flat_map.each {
      //    println it.key +' '+ it.value
      // }

      String type = flat_map['/_type']

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

      // 5. add value for each path tokens
      flat_map.eachWithIndex { path, value, i ->

         //tokens[i] << [value: value] // FIXME: value should be added to the last token part which is the actual attribute name
         tokens[i].last() << [value: value]
      }

      /*
      tokens = [
         ...
         [[att:content, nid:archetype_id=openEHR-EHR-OBSERVATION.lab_test-blood_glucose.v1, idx:0], [att:_type], [value:OBSERVATION]]
         ...
      ]
      */

      // debug: show original path with the corresponding tokens
      flat_map.eachWithIndex { e, i ->
         println e.key +": "+ tokens[i]
      }

      /*
      tokens.each { token_part_list ->

         println token_part_list
         println token_part_list.find{
            it.attr == '_type'
         }
      }

      println tokens.groupBy{ token_part_list ->
         println token_part_list[0].attr
      }
      */


      // tokens.each {
      //    println it
      // }

      // dynamic new Composition() EhrStatus() Folder() etc
      // def rm_class = this.openEHRClassToClassname(type)
      // GroovyClassLoader gcl = new GroovyClassLoader(this.class.classLoader)
      // def rm_obj = gcl.loadClass(rm_class, true, true, true)?.newInstance()

      Map attrs = Model.get_attribute_map(type)

      //println attrs

      Locatable rm_obj = this."new$type"(attrs, tokens)

      //println rm_obj

      // com.cabolabs.openehr.rm_1_0_2.Model

      // TODO: with the root type and the path tokens with the keys included we can reconstruct to object

      // 1. cargar OPT asociado a la raiz
      // 2. crear objeto del RM que va a ser el raiz que devuelvo
      // 3. crear metodos attribute_builder a los que le paso todas las paths que tiene como raiz ese atributo, y les paso el objeto padre para setear los atributos
      // 4. los attribute_builders se van a llamar recursivamente para los atributos de los atributos de la clase raiz
      // 5. para saber el tipo concreto de un atributo es necesario consultar el OPT por el node_id en caso de que hayan distintos hermanos con distintos tipos como alternativa
      // 6. para los nodos multiples, la ruta va a tener el indice incluido, para saber el nombre del atributo hay que sacar el indice pero para saber qué rutas van juntas para seguir la recursion es necesario pasar todas las rutas que tengan la misma raiz, incluyendo el indice

      return rm_obj
   }

   private Map get_constructor_params(String parent_class, Map model_attrs, List tokens)
   {
      Map constructor_params = [:]

      //println ">>> NID for ${parent_class} "+ tokens

      List field_tokens
      Map type_map, field_attrs

      model_attrs.each { attr, type -> // type in the model can be a list of possible concrete classes

         // List<List<Map>> each List<Map> has all the tokens and value for one path/value
         field_tokens = tokens.findAll{ it[0].att == attr } // can have multiple paths for the same attribute

         // if there are no values for the attribute, don't continue processing, some values are optional,
         // this creates rm objects from the available values, doesn't validate values are required or optional
         if (!field_tokens)
         {
            return
         }

         println "--- field tokens for ${attr}: "+ field_tokens

         /* NOTE: archetype_node_id comes in the data but it could be extracted from the path so we can avoid those paths in the serialization
         def archetype_node_id
         def check_field_node_id = field_tokens.find{ it[0].nid } // if not null return a list o maps (one path)
         if (check_field_node_id)
         {
            archetype_node_id = check_field_node_id[0].nid
            if (archetype_node_id)
            {
               // locatable.archetype_node_id
               constructor_params['archetype_node_id'] = archetype_node_id // FIXME: if this is an archetype_id will have the 'archetype_id=' prefix
            }
         }
         */

         /*
         [
            [
               [att:content, nid:archetype_id=openEHR-EHR-OBSERVATION.lab_test-blood_glucose.v1, idx:0],
               [att:_type, value:OBSERVATION]
            ],
            [
               [att:content, nid:archetype_id=openEHR-EHR-OBSERVATION.lab_test-blood_glucose.v1, idx:0],
               [att:protocol, nid:at0004],
               [att:_type, value:ITEM_TREE]
            ],
            [
               [att:content, nid:archetype_id=openEHR-EHR-OBSERVATION.lab_test-blood_glucose.v1, idx:0],
               [att:protocol, nid:at0004],
               [att:items, nid:at0013, idx:0],
               [att:_type, value:CLUSTER]
            ],
            [
               [att:content, nid:archetype_id=openEHR-EHR-OBSERVATION.lab_test-blood_glucose.v1, idx:0],
               [att:protocol, nid:at0004],
               [att:items, nid:at0013, idx:0],
               [att:items, nid:at0062, idx:0],
               [att:_type, value:ELEMENT]
            ],
            ...
         ]
         */

         // type can be String or List, there is no other option in Model
         if (type instanceof List)
         {
            //println "multiple alternative types"
            // type alternatives due inheritance

            // the result of findAll is a list
            // the first element of that list is a list of maps
            // each map is an attribute in a path
            // the map that has att=_type is the instance type that should be instantiated here, because this can't be retrieved from the model
            // note for single type attributes, the type is not in the flat_map paths! is only when the type is not know from the model
            type_map = field_tokens[0].find{ it.att == '_type' }

            if (!type_map) // type doesn't come in the data
            {
               // if the model field type is abstract, then the type should come in the data and we throw the exception
               if (Model.is_abstract_field(parent_class, attr))
               {
                  throw new Exception("_type not found for attribute ${attr}: "+ field_tokens)
               }

               // if the field type is not abstract in the model, the default concrete type should be
               // the first on the list, like [DV_TEXT, DV_CODED_TEXT] => DV_TEXT is default
               type = type[0]
            }
            else // type comes in the data
            {
               type = type_map.value
            }
         }

         //println field_tokens

         // primitive types won't have field attrs
         if (Model.is_primitive(type))
         {
            assert field_tokens.size() == 1         // a single path ends in the primitive processing
            assert field_tokens[0].size() == 1      // the only path in the primitive has just one part
            assert field_tokens[0][0].value != null // there should be a value and the value is not empty, because the flat map doesn't include empty values

            println "${attr} : ${type} tokens: "+ field_tokens[0][0].value
            println "primitive value: "+ field_tokens.value // can be null!!!

            constructor_params[attr] = field_tokens[0][0].value
         }
         else
         {
            // TODO: si el atributo es multiple en el modelo, los field tokens pueden ser para varias
            // instancias distintas de objetos que van en la coleccion, por ejemplo dos observaciones
            // distintas dentro de COMPOSITION.content, porque se piden los tokens para 'content' pero
            // no se discrimina por el archetype_node_id ni por el index que hay en los datos

            field_attrs = Model.get_attribute_map(type)

            // removes the current field map from the tokens to keep iterating on children fields
            def next_field_tokens

            if (Model.is_multiple(parent_class, attr))
            {
               // data could contain mutiple objects for the attr collection, group by idx and process tokens for each idx separatelly
               // it could contain just one, so the group will have just one index, but the processing is the same

               //println "--- attr ${parent_class} ${attr} can have multiple objects, need to group by node_id and index"
               /*
               [ 0:
                  [
                     [
                     [att:content, nid:archetype_id=openEHR-EHR-OBSERVATION.lab_test-blood_glucose.v1, idx:0],
                     [att:_type, value:OBSERVATION]
                     ],
                     [
                     [att:content, nid:archetype_id=openEHR-EHR-OBSERVATION.lab_test-blood_glucose.v1, idx:0],
                     [att:protocol, nid:at0004],
                     [att:_type, value:ITEM_TREE]
                     ],
                     ...
                  ],
                  ...
               ]
               */
               // field_tokens.groupBy{ it[0].idx }.each{
               //    println "index ${it.key}:"
               //    it.value.each { _tokens ->
               //       println _tokens
               //    }
               // }

               // grouping by index is enough
               field_tokens.groupBy{ it[0].idx }.each{ idx, object_i_field_tokens ->

                  next_field_tokens = object_i_field_tokens.collect { it.tail() }

                  if (!constructor_params[attr]) constructor_params[attr] = []
                  constructor_params[attr] << this."new$type"(field_attrs, next_field_tokens)
               }


               //if (!constructor_params[attr]) constructor_params[attr] = []
               //constructor_params[attr] << this."new$type"(field_attrs, next_field_tokens)
            }
            else // attribute is not a collection
            {
               // removes the current field map from the tokens to keep iterating on children fields
               next_field_tokens = field_tokens.collect { it.tail() }

               println "X "+ next_field_tokens

               constructor_params[attr] = this."new$type"(field_attrs, next_field_tokens)
            }
         }
      }

      return constructor_params
   }

   Composition newCOMPOSITION(Map model_attrs, List tokens)
   {
      Map constructor_params = this.get_constructor_params('COMPOSITION', model_attrs, tokens)

      new Composition(constructor_params)
   }

   EventContext newEVENT_CONTEXT(Map model_attrs, List tokens)
   {
      Map constructor_params = this.get_constructor_params('EVENT_CONTEXT', model_attrs, tokens)

      new EventContext(constructor_params)
   }

   PartyIdentified newPARTY_IDENTIFIED(Map model_attrs, List tokens)
   {
      Map constructor_params = this.get_constructor_params('PARTY_IDENTIFIED', model_attrs, tokens)

      new PartyIdentified(constructor_params)
   }

   Observation newOBSERVATION(Map model_attrs, List tokens)
   {
      Map constructor_params = this.get_constructor_params('OBSERVATION', model_attrs, tokens)

      println ">>> Obs tokens: "+ tokens

      println tokens.find { it[0].nid != null }

      new Observation(constructor_params)
   }

   History newHISTORY(Map model_attrs, List tokens)
   {
      Map constructor_params = this.get_constructor_params('HISTORY', model_attrs, tokens)

      new History(constructor_params)
   }

   PointEvent newPOINT_EVENT(Map model_attrs, List tokens)
   {
      Map constructor_params = this.get_constructor_params('POINT_EVENT', model_attrs, tokens)

      new PointEvent(constructor_params)
   }

   IntervalEvent newINTERVAL_EVENT(Map model_attrs, List tokens)
   {
      Map constructor_params = this.get_constructor_params('INTERVAL_EVENT', model_attrs, tokens)

      new IntervalEvent(constructor_params)
   }

   ItemTree newITEM_TREE(Map model_attrs, List tokens)
   {
      Map constructor_params = this.get_constructor_params('ITEM_TREE', model_attrs, tokens)

      new ItemTree(constructor_params)
   }

   Cluster newCLUSTER(Map model_attrs, List tokens)
   {
      Map constructor_params = this.get_constructor_params('CLUSTER', model_attrs, tokens)

      new Cluster(constructor_params)
   }

   Element newELEMENT(Map model_attrs, List tokens)
   {
      Map constructor_params = this.get_constructor_params('ELEMENT', model_attrs, tokens)

      new Element(constructor_params)
   }


   TerminologyId newTERMINOLOGY_ID(Map model_attrs, List tokens)
   {
      Map constructor_params = this.get_constructor_params('TERMINOLOGY_ID', model_attrs, tokens)

      new TerminologyId(constructor_params)
   }

   PartyRef newPARTY_REF(Map model_attrs, List tokens)
   {
      Map constructor_params = this.get_constructor_params('PARTY_REF', model_attrs, tokens)

      new PartyRef(constructor_params)
   }

   PartySelf newPARTY_SELF(Map model_attrs, List tokens)
   {
      Map constructor_params = this.get_constructor_params('PARTY_SELF', model_attrs, tokens)

      new PartySelf(constructor_params)
   }

   HierObjectId newHIER_OBJECT_ID(Map model_attrs, List tokens)
   {
      Map constructor_params = this.get_constructor_params('HIER_OBJECT_ID', model_attrs, tokens)

      new HierObjectId(constructor_params)
   }


   DvDateTime newDV_DATE_TIME(Map model_attrs, List tokens)
   {
      Map constructor_params = this.get_constructor_params('DV_DATE_TIME', model_attrs, tokens)

      new DvDateTime(constructor_params)
   }

   DvQuantity newDV_QUANTITY(Map model_attrs, List tokens)
   {
      Map constructor_params = this.get_constructor_params('DV_QUANTITY', model_attrs, tokens)

      new DvQuantity(constructor_params)
   }

   DvProportion newDV_PROPORTION(Map model_attrs, List tokens)
   {
      Map constructor_params = this.get_constructor_params('DV_PROPORTION', model_attrs, tokens)

      new DvProportion(constructor_params)
   }

   DvIdentifier newDV_IDENTIFIER(Map model_attrs, List tokens)
   {
      Map constructor_params = this.get_constructor_params('DV_IDENTIFIER', model_attrs, tokens)

      new DvIdentifier(constructor_params)
   }

   DvText newDV_TEXT(Map model_attrs, List tokens)
   {
      Map constructor_params = this.get_constructor_params('DV_TEXT', model_attrs, tokens)

      new DvText(constructor_params)
   }

   DvCodedText newDV_CODED_TEXT(Map model_attrs, List tokens)
   {
      Map constructor_params = this.get_constructor_params('DV_CODED_TEXT', model_attrs, tokens)

      new DvCodedText(constructor_params)
   }

   CodePhrase newCODE_PHRASE(Map model_attrs, List tokens)
   {
      Map constructor_params = this.get_constructor_params('CODE_PHRASE', model_attrs, tokens)

      //println "constructor params code phrase "+ constructor_params
      // constructor params code phrase
      // [terminology_id:com.cabolabs.openehr.rm_1_0_2.support.identification.TerminologyId@1de9b505, code_string:en]

      // NOTE: the RM implementation has codeString and the model attrs has code_string, this does the conversion
      constructor_params = constructor_params.collectEntries{ entry -> [(snake2Camel(entry.key)): entry.value] }

      //println "constructor params code phrase 2 "+ constructor_params
      //[terminologyId:com.cabolabs.openehr.rm_1_0_2.support.identification.TerminologyId@7b122839, codeString:en]

      new CodePhrase(constructor_params)
   }


   String snake2Camel(String snake)
   {
      snake.replaceAll( "(_)([A-Za-z0-9])", { Object[] it -> it[2].toUpperCase() } )
   }

   private Version parse_version(Map flat_map)
   {


      // TODO:
      return null
   }

   private EhrDto parse_ehr(Map flat_map)
   {

      // TODO:
      return null

   }
}