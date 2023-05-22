package com.cabolabs.openehr.rm_1_0_2

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
class Model {

   public static Map rm_attributes_not_in_opt = [
      'EHR_STATUS': [
         'subject': 'PARTY_SELF',
         'is_queryable': 'Boolean',
         'is_modifiable': 'Boolean'
      ],
      'COMPOSITION': [
         'context': 'EVENT_CONTEXT' // if no other_context is specified the event context is not on the OPT, we need to check if it is or not to avoid double indexing.
      ],
      'EVENT_CONTEXT': [
         'setting': 'DV_CODED_TEXT',
         'location': 'String',
         'start_time': 'DV_DATE_TIME',
         'end_time': 'DV_DATE_TIME'
      ],
      'ACTION': [
         'time': 'DV_DATE_TIME',
         'instruction_details': 'INSTRUCTION_DETAILS'
      ],
      'INSTRUCTION_DETAILS': [
         'instruction_id': 'LOCATABLE_REF',
         'activity_id': 'String'
      ],
      'INSTRUCTION': [
         'narrative': 'DV_TEXT',
         'expiry_time': 'DV_DATE_TIME'
      ],
      'ACTIVITY': [
         'timing': 'DV_PARSABLE',
         'action_archetype_id': 'String'
      ],
      'HISTORY': [
         'origin': 'DV_DATE_TIME',
         'period': 'DV_DURATION',
         'duration': 'DV_DURATION'
      ],
      'EVENT': [ // to avoid issues with clients using abstract types, considered point event
         'time': 'DV_DATE_TIME'
      ],
      'POINT_EVENT': [
         'time': 'DV_DATE_TIME'
      ],
      'INTERVAL_EVENT': [
         'time': 'DV_DATE_TIME',
         'width': 'DV_DURATION'
      ],
      'ELEMENT': [
         'null_flavour': 'DV_CODED_TEXT' // this could be in the opt constraining the possible codes
      ],

      // DEMOGRAPHIC
      'PARTY_RELATIONSHIP': [
         'source': 'PARTY_REF', // need to support queries over the relationship.source to find all the relationships of an actor
         'time_validity': 'DV_INTERVAL'
      ],
      'ROLE': [
         'time_validity': 'DV_INTERVAL'
      ],
      'CAPABILITY': [
         'time_validity': 'DV_INTERVAL'
      ],
      'CONTACT': [
         'time_validity': 'DV_INTERVAL'
      ],

      // REF and ID
      'PARTY_REF': [
         'id': 'OBJECT_VERSION_ID' // NOTE: this is OBJECT_ID but our implementation only allows OBJECT_VERSION_ID here, this should be part of the conformance statement!
      ],
      'OBJECT_VERSION_ID': [
         'value': 'String' // need to query by the PARTY_REF.id by a string criteria
      ],
      'LOCATABLE_REF': [ // to allow to query by locatable_ref id and path, used in INSTRUCTION_DETAILS.instruction_id
         'id': 'OBJECT_VERSION_ID',
         'path': 'String'
      ]
   ]

   private static Map collection_attrs = [
      COMPOSITION: [
         'content'
      ],
      SECTION: [
         'items'
      ],
      OBSERVATION: [
         'other_participations' // inherited from ENTRY
      ],
      EVALUATION: [
         'other_participations' // inherited from ENTRY
      ],
      INSTRUCTION: [
         'activities',
         'other_participations' // inherited from ENTRY
      ],
      ACTION: [
         'other_participations' // inherited from ENTRY
      ],
      HISTORY: [
         'events'
      ],
      ITEM_TREE: [
         'items'
      ],
      ITEM_LIST: [
         'items'
      ],
      ITEM_TABLE: [
         'rows'
      ],
      CLUSTER: [
         'items'
      ],
      FOLDER: [
         'folders'
      ],
      DV_TEXT: [
         'mappings'
      ],
      DV_CODED_TEXT: [
         'mappings'
      ],
      DV_ORDINAL: [
         'other_reference_ranges'
      ],
      DV_PROPORTION: [
         'other_reference_ranges'
      ],
      DV_QUANTITY: [
         'other_reference_ranges'
      ],
      DV_COUNT: [
         'other_reference_ranges'
      ]

   ]

   private static List abstract_classes = [
      'PATHABLE',
      'LOCTABLE',
      'VERSION',
      'PARTY_PROXY',
      'CONTENT_ITEM',
      'ENTRY',
      'CARE_ENTRY',
      'DATA_STRUCTURE',
      'EVENT',
      'ITEM_STRUCTURE',
      'ITEM',
      'DATA_VALUE',
      'DV_ENCAPSULATED',
      'DV_ABSOLUTE_QUANTITY',
      'DV_AMOUNT',
      'DV_ORDERED',
      'DV_QUANTIFIED',
      'DV_TEMPORAL',
      'ACTOR',
      'PARTY',
      'ACCESS_CONTROL_SETTINGS',
      'OBJECT_ID',
      'UID',
      'UID_BASED_ID'
   ]

   // fields with abstract type
   private static Map abstract_fields = [
      COMPOSITION: [
         'content',
         'composer'
      ],
      OBSERVATION: [
         // CARE_ENTRY
         'protocol'
      ],
      EVALUATION: [
         'data',
         // CARE_ENTRY
         'protocol',
         // ENTRY
         'subject',
         'provider'
      ],
      INSTRUCTION: [
         // CARE_ENTRY
         'protocol',
         // ENTRY
         'subject',
         'provider'
      ],
      ACTION: [
         'description',
         // CARE_ENTRY
         'protocol',
         // ENTRY
         'subject',
         'provider'
      ],
      ADMIN_ENTRY: [
         'data',
         // ENTRY
         'subject',
         'provider'
      ],
      ACTIVITY: [
         'description'
      ],
      INSTRUCTION_DETAILS: [
         'wf_details'
      ],
      HISTORY: [
         'summary'
      ],
      POINT_EVENT: [
         // EVENT
         'data',
         'state'
      ],
      INTERVAL_EVENT: [
         // EVENT
         'data',
         'state'
      ],
      CLUSTER: [
         'items'
      ],
      ELEMENT: [
         'value'
      ]
      // TODO: dv abstract fields
   ]

   private static Map full = [
      EHR_STATUS: [
         subject:              'PARTY_SELF',
         is_queryable:         'Boolean',
         is_modifiable:        'Boolean',
         other_details:        ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE'],
         // LOCATABLE
         name:                 ['DV_TEXT', 'DV_CODED_TEXT'],
         archetype_node_id:    'String',
         uid:                  'UID_BASED_ID',
         archetype_details:    'ARCHETYPED'
      ],
      COMPOSITION: [
         language:  'CODE_PHRASE',
         territory: 'CODE_PHRASE',
         category:  'DV_CODED_TEXT',
         composer:  ['PARTY_IDENTIFIED', 'PARTY_RELATED', 'PARTY_SELF'],
         content:   ['SECTION', 'ADMIN_ENTRY', 'OBSERVATION', 'EVALUATION', 'INSTRUCTION', 'ACTION'], // list because the type is abstract, multiple attribute, can query the multiple attr map
         context:   'EVENT_CONTEXT', // if no other_context is specified the event context is not on the OPT, we need to check if it is or not to avoid double indexing.
         // LOCATABLE
         archetype_details: 'ARCHETYPED',
         name:              ['DV_TEXT', 'DV_CODED_TEXT'],
         archetype_node_id: 'String',
         uid:               ['HIER_OBJECT_ID', 'OBJECT_VERSION_ID']
      ],
      EVENT_CONTEXT: [
         start_time:           'DV_DATE_TIME',
         end_time:             'DV_DATE_TIME',
         location:             'String',
         setting:              'DV_CODED_TEXT',
         other_context:        ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE'],
         health_care_facility: ['PARTY_IDENTIFIED', 'PARTY_RELATED'],
         participations:       'PARTICIPATION' // multiple attribute, can query the multiple attr map
      ],
      SECTION: [
         items:                ['SECTION', 'ADMIN_ENTRY', 'OBSERVATION', 'EVALUATION', 'INSTRUCTION', 'ACTION'], // list because the type is abstract, multiple attribute, can query the multiple attr map
         // LOCATABLE
         name:                 ['DV_TEXT', 'DV_CODED_TEXT'],
         archetype_node_id:    'String',
         uid:                  'UID_BASED_ID',
         archetype_details:    'ARCHETYPED'
      ],
      OBSERVATION: [
         data:                 'HISTORY',
         state:                'HISTORY',
         // CARE_ENTRY
         protocol:             ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE'],
         guideline_id:         'OBJECT_REF',
         // ENTRY
         language:             'CODE_PHRASE',
         encoding:             'CODE_PHRASE',
         subject:              ['PARTY_IDENTIFIED', 'PARTY_RELATED', 'PARTY_SELF'],
         provider:             ['PARTY_IDENTIFIED', 'PARTY_RELATED', 'PARTY_SELF'],
         other_participations: 'PARTICIPATION', // multiple attribute, can query the multiple attr map
         // LOCATABLE
         name:                 ['DV_TEXT', 'DV_CODED_TEXT'],
         archetype_node_id:    'String',
         uid:                  'UID_BASED_ID',
         archetype_details:    'ARCHETYPED'
      ],
      EVALUATION: [
         data:         ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE'],
         // CARE_ENTRY
         protocol:     ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE'],
         guideline_id: 'OBJECT_REF',
         // ENTRY
         language:     'CODE_PHRASE',
         encoding:     'CODE_PHRASE',
         subject:      ['PARTY_IDENTIFIED', 'PARTY_RELATED', 'PARTY_SELF'],
         provider:     ['PARTY_IDENTIFIED', 'PARTY_RELATED', 'PARTY_SELF'],
         other_participations: 'PARTICIPATION', // multiple attribute, can query the multiple attr map
         // LOCATABLE
         name:                 ['DV_TEXT', 'DV_CODED_TEXT'],
         archetype_node_id:    'String',
         uid:                  'UID_BASED_ID',
         archetype_details:    'ARCHETYPED'
      ],
      INSTRUCTION: [
         narrative:     ['DV_TEXT', 'DV_CODED_TEXT'],
         expiry_time:   'DV_DATE_TIME',
         wf_definition: 'DV_PARSABLE',
         activities:    'ACTIVITY', // multiple attribute, can query the multiple attr map
         // CARE_ENTRY
         protocol:      ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE'],
         guideline_id:  'OBJECT_REF',
         // ENTRY
         language:      'CODE_PHRASE',
         encoding:      'CODE_PHRASE',
         subject:       ['PARTY_IDENTIFIED', 'PARTY_RELATED', 'PARTY_SELF'],
         provider:      ['PARTY_IDENTIFIED', 'PARTY_RELATED', 'PARTY_SELF'],
         other_participations: 'PARTICIPATION', // multiple attribute, can query the multiple attr map
         // LOCATABLE
         name:                 ['DV_TEXT', 'DV_CODED_TEXT'],
         archetype_node_id:    'String',
         uid:                  'UID_BASED_ID',
         archetype_details:    'ARCHETYPED'
      ],
      ACTION: [
         time:                 'DV_DATE_TIME',
         description:          ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE'],
         ism_transition:       'ISM_TRANSITION',
         instruction_details:  'INSTRUCTION_DETAILS',
         // CARE_ENTRY
         protocol:             ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE'],
         guideline_id:         'OBJECT_REF',
         // ENTRY
         language:             'CODE_PHRASE',
         encoding:             'CODE_PHRASE',
         subject:              ['PARTY_IDENTIFIED', 'PARTY_RELATED', 'PARTY_SELF'],
         provider:             ['PARTY_IDENTIFIED', 'PARTY_RELATED', 'PARTY_SELF'],
         other_participations: 'PARTICIPATION', // multiple attribute, can query the multiple attr map
         // LOCATABLE
         name:                 ['DV_TEXT', 'DV_CODED_TEXT'],
         archetype_node_id:    'String',
         uid:                  'UID_BASED_ID',
         archetype_details:    'ARCHETYPED'
      ],
      ADMIN_ENTRY: [
         data:                 ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE'],
         // ENTRY
         language:             'CODE_PHRASE',
         encoding:             'CODE_PHRASE',
         subject:              ['PARTY_IDENTIFIED', 'PARTY_RELATED', 'PARTY_SELF'],
         provider:             ['PARTY_IDENTIFIED', 'PARTY_RELATED', 'PARTY_SELF'],
         other_participations: 'PARTICIPATION', // multiple attribute, can query the multiple attr map
         // LOCATABLE
         name:                 ['DV_TEXT', 'DV_CODED_TEXT'],
         archetype_node_id:    'String',
         uid:                  'UID_BASED_ID',
         archetype_details:    'ARCHETYPED'
      ],
      INSTRUCTION_DETAILS: [
         instruction_id:      'LOCATABLE_REF',
         activity_id:         'String',
         wf_details:          ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE']
      ],
      ACTIVITY: [
         description:         ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE'],
         timing:              'DV_PARSABLE',
         action_archetype_id: 'String',
         // LOCATABLE
         name:                 ['DV_TEXT', 'DV_CODED_TEXT'],
         archetype_node_id:    'String',
         uid:                  'UID_BASED_ID',
         archetype_details:    'ARCHETYPED'
      ],
      HISTORY: [
         origin:               'DV_DATE_TIME',
         period:               'DV_DURATION',
         duration:             'DV_DURATION',
         events:               ['POINT_EVENT', 'INTERVAL_EVENT'],  // multiple attribute, can query the multiple attr map
         summary:              ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE'],
         // LOCATABLE
         name:                 ['DV_TEXT', 'DV_CODED_TEXT'],
         archetype_node_id:    'String',
         uid:                  'UID_BASED_ID',
         archetype_details:    'ARCHETYPED'
      ],
      // FIXME: this type shouldn't be in the schema
      EVENT: [ // to avoid issues with clients using abstract types, considered point event
         time: 'DV_DATE_TIME',
         data:  ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE'],
         satte: ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE']
      ],
      POINT_EVENT: [
         // EVENT
         time: 'DV_DATE_TIME',
         data:  ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE'],
         satte: ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE']
      ],
      INTERVAL_EVENT: [
         width:         'DV_DURATION',
         math_function: 'DV_CODED_TEXT',
         sample_count:  'Integer',
         // EVENT
         time: 'DV_DATE_TIME',
         data:  ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE'],
         satte: ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE']
      ],

      ITEM_TREE: [
         items: ['ELEMENT', 'CLUSTER'], // multiple
         // LOCATABLE
         name:                 ['DV_TEXT', 'DV_CODED_TEXT'],
         archetype_node_id:    'String',
         uid:                  'UID_BASED_ID',
         archetype_details:    'ARCHETYPED'
      ],

      CLUSTER: [
         items: ['ELEMENT', 'CLUSTER'], // multiple attribute, can query the multiple attr map
         // LOCATABLE
         name:                 ['DV_TEXT', 'DV_CODED_TEXT'],
         archetype_node_id:    'String',
         uid:                  'UID_BASED_ID',
         archetype_details:    'ARCHETYPED'
      ],
      ELEMENT: [
         // FIXME: set all possible concrete types
         value: [
            'DV_TEXT',
            'DV_CODED_TEXT',
            'DV_QUANTITY',
            'DV_PROPORTION',
            'DV_COUNT',
            'DV_ORDINAL',
            'DV_BOOLEAN',
            'DV_IDENTIFIER',
            'DV_DATE',
            'DV_DATE_TIME',
            'DV_TIME',
            'DV_DURATION'
         ],
         null_flavour: 'DV_CODED_TEXT', // this could be in the opt constraining the possible codes
         // LOCATABLE
         name:                 ['DV_TEXT', 'DV_CODED_TEXT'],
         archetype_node_id:    'String',
         uid:                  'UID_BASED_ID',
         archetype_details:    'ARCHETYPED'
      ],
      PARTY_IDENTIFIED: [
         name: 'String',
         identifiers: 'DV_IDENTIFIER', // multiple
         // PARTY_PROXY
         external_ref: 'PARTY_REF'
      ],
      PARTY_RELATED: [
         relationship: 'DV_CODED_TEXT',
         // PARTY_IDENTIFIED
         name: 'String',
         identifiers: 'DV_IDENTIFIER', // multiple
         // PARTY_PROXY
         external_ref: 'PARTY_REF'
      ],
      PARTY_SELF: [
         // PARTY_PROXY
         external_ref: 'PARTY_REF'
      ],
      OBJECT_REF: [
         namespace: 'String',
         type:      'String',
         id:        ['GENERIC_ID', 'HIER_OBJECT_ID', 'OBJECT_VERSION_ID', 'TERMINOLOGY_ID', 'TEMPLATE_ID', 'ARCHETYPE_ID']
      ],
      PARTY_REF: [
         // OBJECT_REF
         namespace: 'String',
         type:      'String',
         id:        ['GENERIC_ID', 'HIER_OBJECT_ID', 'OBJECT_VERSION_ID', 'TERMINOLOGY_ID', 'TEMPLATE_ID', 'ARCHETYPE_ID']
      ],
      LOCATABLE_REF: [
         path: 'String',
         // OBJECT_REF
         namespace: 'String',
         type: 'String',
         id: ['HIER_OBJECT_ID', 'OBJECT_VERSION_ID'] // LOCATABLE_REF redefines id to be only UID_BASED_ID
      ],
      GENERIC_ID: [
         scheme: 'String',
         // OBJECT_ID
         value: 'String'
      ],
      TERMINOLOGY_ID: [
         // OBJECT_ID
         value: 'String'
      ],
      ARCHETYPE_ID: [
         // OBJECT_ID
         value: 'String'
      ],
      TEMPLATE_ID: [
         // OBJECT_ID
         value: 'String'
      ],
      OBJECT_VERSION_ID: [
         // OBJECT_ID
         value: 'String'
      ],
      HIER_OBJECT_ID: [
         // OBJECT_ID
         value: 'String'
      ],

      DV_BOOLEAN: [
         value: 'Boolean'
      ],
      DV_IDENTIFIER: [
         issuer: 'String',
         assigner: 'String',
         id: 'String',
         type: 'String'
      ],

      DV_TEXT: [
         value:      'String',
         hyperlink:  'DV_URI',
         formatting: 'String',
         mappings:   'TERM_MAPPING', // multiple
         language:   'CODE_PHRASE',
         encoding:   'CODE_PHRASE'
      ],
      DV_CODED_TEXT: [
         defining_code: 'CODE_PHRASE',
         // DV_TEXT
         value:      'String',
         hyperlink:  'DV_URI',
         formatting: 'String',
         mappings:   'TERM_MAPPING', // multiple
         language:   'CODE_PHRASE',
         encoding:   'CODE_PHRASE'
      ],
      CODE_PHRASE: [
         terminology_id: 'TERMINOLOGY_ID',
         code_string: 'String'
      ],
      TERM_MAPPING: [
         match:   'Character',
         purpose: 'DV_CODED_TEXT',
         target:  'CODE_PHRASE'
      ],

      DV_ORDINAL: [
         value: 'Integer',
         symbol: 'DV_CODED_TEXT',
         // DV_ORDERED
         normal_status: 'CODE_PHRASE',
         normal_range: 'DV_INTERVAL',
         other_reference_ranges: 'DV_INTERVAL' // multiple
      ],
      DV_PROPORTION: [
         numerator: 'Real',
         denominator: 'Real',
         type: 'Integer',
         precision: 'Integer',
         // DV_AMOUNT
         accuracy: 'Real',
         accuracy_is_percent: 'Boolean',
         // DV_QUANTIFIED
         magnitude_status: 'String',
         // DV_ORDERED
         normal_status: 'CODE_PHRASE',
         normal_range: 'DV_INTERVAL',
         other_reference_ranges: 'DV_INTERVAL' // multiple
      ],
      DV_QUANTITY: [
         magnitude: 'Double',
         precision: 'Integer',
         units: 'String',
         // DV_AMOUNT
         accuracy: 'Real',
         accuracy_is_percent: 'Boolean',
         // DV_QUANTIFIED
         magnitude_status: 'String',
         // DV_ORDERED
         normal_status: 'CODE_PHRASE',
         normal_range: 'DV_INTERVAL',
         other_reference_ranges: 'DV_INTERVAL' // multiple
      ],
      DV_COUNT: [
         magnitude: 'Integer',
         // DV_AMOUNT
         accuracy: 'Real',
         accuracy_is_percent: 'Boolean',
         // DV_QUANTIFIED
         magnitude_status: 'String',
         // DV_ORDERED
         normal_status: 'CODE_PHRASE',
         normal_range: 'DV_INTERVAL',
         other_reference_ranges: 'DV_INTERVAL' // multiple
      ],

      DV_DATE: [
         value: 'String',
         // DV_QUANTIFIED
         magnitude_status: 'String',
         // DV_ORDERED
         normal_status: 'CODE_PHRASE',
         normal_range: 'DV_INTERVAL',
         other_reference_ranges: 'DV_INTERVAL' // multiple
      ],
      DV_TIME: [
         value: 'String',
         // DV_QUANTIFIED
         magnitude_status: 'String',
         // DV_ORDERED
         normal_status: 'CODE_PHRASE',
         normal_range: 'DV_INTERVAL',
         other_reference_ranges: 'DV_INTERVAL' // multiple
      ],
      DV_DATE_TIME: [
         value: 'String',
         // DV_QUANTIFIED
         magnitude_status: 'String',
         // DV_ORDERED
         normal_status: 'CODE_PHRASE',
         normal_range: 'DV_INTERVAL',
         other_reference_ranges: 'DV_INTERVAL' // multiple
      ],
      DV_DURATION: [
         value: 'String',
         // DV_AMOUNT
         accuracy: 'Real',
         accuracy_is_percent: 'Boolean',
         // DV_QUANTIFIED
         magnitude_status: 'String',
         // DV_ORDERED
         normal_status: 'CODE_PHRASE',
         normal_range: 'DV_INTERVAL',
         other_reference_ranges: 'DV_INTERVAL' // multiple
      ]
   ]

   static List primitive_types = [
      'Integer',
      'Real',
      'Double',
      'Boolean',
      'String',
      'Character'
   ]


   static def get_type(String clazz, String attr)
   {
      full[clazz][attr] // can be null, string or list
   }

   static def get_attribute_map(String clazz)
   {
      full[clazz]
   }

   static boolean is_multiple(String clazz, String attr)
   {
      collection_attrs[clazz] ? collection_attrs[clazz].contains(attr) : false
   }

   static boolean is_primitive(String clazz)
   {
      primitive_types.contains(clazz)
   }

   static boolean is_abstract_class(String clazz)
   {
      abstract_classes.contains(clazz)
   }

   static boolean is_abstract_field(String clazz, String attr)
   {
      abstract_fields[clazz] ? abstract_fields[clazz].contains(attr) : false
   }
}