package com.cabolabs.openehr.rm_1_0_2

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
class Model {

   private Map rm_attributes_not_in_opt = [
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

   private Map collection_attrs = [
      'COMPOSITION': [
         'content'
      ],
      'SECTION': [
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

      'HISTORY': [
         'events'
      ],
      'ITEM_TREE': [
         'items'
      ],
      'ITEM_LIST': [
         'items'
      ],
      'ITEM_TABLE': [
         'rows'
      ],
      'CLUSTER': [
         'items'
      ],
      'FOLDER': [
         'folders'
      ]
   ]

   private Map full = [
      'EHR_STATUS': [
         'subject': 'PARTY_SELF',
         'is_queryable': 'Boolean',
         'is_modifiable': 'Boolean'
      ],
      'COMPOSITION': [
         language:  'CODE_PHRASE',
         territory: 'CODE_PHRASE',
         category:  'DV_CODED_TEXT',
         composer:  'PARTY_PROXY',
         content:   ['SECTION', 'ADMIN_ENTRY', 'OBSERVATION', 'EVALUATION', 'INSTRUCTION', 'ACTION'], // list because the type is abstract, multiple attribute, can query the multiple attr map
         context:   'EVENT_CONTEXT' // if no other_context is specified the event context is not on the OPT, we need to check if it is or not to avoid double indexing.
      ],
      EVENT_CONTEXT: [
         setting:       'DV_CODED_TEXT',
         location:      'String',
         start_time:    'DV_DATE_TIME',
         end_time:      'DV_DATE_TIME',
         other_context: ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE']
      ],
      OBSERVATION: [
         data:         'HISTORY',
         state:        'HISTORY',
         protocol:     ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE'],
         guideline_id: 'OBJECT_REF',
         language:     'CODE_PHRASE',
         encoding:     'CODE_PHRASE',
         subject:      'PARTY_PROXY',
         provider:     'PARTY_PROXY',
         other_participations: 'PARTICIPATION' // multiple attribute, can query the multiple attr map
      ],
      EVALUATION: [
         data:         ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE'],
         protocol:     ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE'],
         guideline_id: 'OBJECT_REF',
         language:     'CODE_PHRASE',
         encoding:     'CODE_PHRASE',
         subject:      'PARTY_PROXY',
         provider:     'PARTY_PROXY',
         other_participations: 'PARTICIPATION' // multiple attribute, can query the multiple attr map
      ],
      INSTRUCTION: [
         narrative:    ['DV_TEXT', 'DV_CODED_TEXT'],
         expiry_time:  'DV_DATE_TIME',
         wf_definition: 'DV_PARSABLE',
         activities:   'ACTIVITY', // multiple attribute, can query the multiple attr map
         protocol:     ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE'],
         guideline_id: 'OBJECT_REF',
         language:     'CODE_PHRASE',
         encoding:     'CODE_PHRASE',
         subject:      'PARTY_PROXY',
         provider:     'PARTY_PROXY',
         other_participations: 'PARTICIPATION' // multiple attribute, can query the multiple attr map
      ],
      ACTION: [
         time:                 'DV_DATE_TIME',
         description:          ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE'],
         ism_transition:       'ISM_TRANSITION',
         instruction_details:  'INSTRUCTION_DETAILS',
         protocol:             ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE'],
         guideline_id:         'OBJECT_REF',
         language:             'CODE_PHRASE',
         encoding:             'CODE_PHRASE',
         subject:              'PARTY_PROXY',
         provider:             'PARTY_PROXY',
         other_participations: 'PARTICIPATION' // multiple attribute, can query the multiple attr map
      ],
      ADMIN_ENTRY: [
         data:                 ['ITEM_TREE', 'ITEM_LIST', 'ITEM_TABLE', 'ITEM_SINGLE'],
         language:             'CODE_PHRASE',
         encoding:             'CODE_PHRASE',
         subject:              'PARTY_PROXY',
         provider:             'PARTY_PROXY',
         other_participations: 'PARTICIPATION' // multiple attribute, can query the multiple attr map
      ]

      INSTRUCTION_DETAILS: [
         'instruction_id': 'LOCATABLE_REF',
         'activity_id': 'String'
      ],

      ACTIVITY: [
         'timing': 'DV_PARSABLE',
         'action_archetype_id': 'String'
      ],
      HISTORY: [
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
}