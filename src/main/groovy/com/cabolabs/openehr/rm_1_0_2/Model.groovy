package com.cabolabs.openehr.rm_1_0_2

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
class Model {

   private static Map rm_attributes_not_in_opt = [
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
      ]
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
         context:   'EVENT_CONTEXT' // if no other_context is specified the event context is not on the OPT, we need to check if it is or not to avoid double indexing.
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
         subject:              'PARTY_PROXY',
         provider:             'PARTY_PROXY',
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
         subject:      'PARTY_PROXY',
         provider:     'PARTY_PROXY',
         other_participations: 'PARTICIPATION', // multiple attribute, can query the multiple attr map
         // LOCATABLE
         name:                 ['DV_TEXT', 'DV_CODED_TEXT'],
         archetype_node_id:    'String',
         uid:                  'UID_BASED_ID',
         archetype_details:    'ARCHETYPED'
      ],
      INSTRUCTION: [
         narrative:    ['DV_TEXT', 'DV_CODED_TEXT'],
         expiry_time:  'DV_DATE_TIME',
         wf_definition: 'DV_PARSABLE',
         activities:   'ACTIVITY', // multiple attribute, can query the multiple attr map
         // CARE_ENTRY
         protocol:     ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE'],
         guideline_id: 'OBJECT_REF',
         // ENTRY
         language:     'CODE_PHRASE',
         encoding:     'CODE_PHRASE',
         subject:      'PARTY_PROXY',
         provider:     'PARTY_PROXY',
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
         subject:              'PARTY_PROXY',
         provider:             'PARTY_PROXY',
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
         subject:              'PARTY_PROXY',
         provider:             'PARTY_PROXY',
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
      CLUSTER: [
         items: ['ELEMENT', 'CLUSTER'], // multiple attribute, can query the multiple attr map
         // LOCATABLE
         name:                 ['DV_TEXT', 'DV_CODED_TEXT'],
         archetype_node_id:    'String',
         uid:                  'UID_BASED_ID',
         archetype_details:    'ARCHETYPED'
      ],
      ELEMENT: [
         value: 'DATA_VALUE', // FIXME: set all possible concrete types
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
         type: 'String',
         id: ['GENERIC_ID', 'HIER_OBJECT_ID', 'OBJECT_VERSION_ID', 'TERMINOLOGY_ID', 'TEMPLATE_ID', 'ARCHETYPE_ID']
      ],
      PARTY_REF: [
         // OBJECGT_REF
         namespace: 'String',
         type: 'String',
         id: ['GENERIC_ID', 'HIER_OBJECT_ID', 'OBJECT_VERSION_ID', 'TERMINOLOGY_ID', 'TEMPLATE_ID', 'ARCHETYPE_ID']
      ],
      LOCATABLE_REF: [
         path: 'String',
         // OBJECGT_REF
         namespace: 'String',
         type: 'String',
         id: ['GENERIC_ID', 'HIER_OBJECT_ID', 'OBJECT_VERSION_ID', 'TERMINOLOGY_ID', 'TEMPLATE_ID', 'ARCHETYPE_ID']
      ],
      DV_IDENTIFIER: [
         issuer: 'String',
         assigner: 'String',
         id: 'String',
         type: 'String'
      ],
      DV_CODED_TEXT: [
         defining_code: 'CODE_PHRASE',
         // DV_TEXT
         value: 'String'
         // TODO: other fields
      ],
      CODE_PHRASE: [
         terminology_id: 'TERMINOLOGY_ID',
         code_string: 'String'
      ],
      TERMINOLOGY_ID: [
         // OBJECT_ID
         value: 'String'
      ]
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
}