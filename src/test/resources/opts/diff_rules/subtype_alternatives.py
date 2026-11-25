#!/usr/bin/env python3
"""Fixtures for alternatives that involve RM inheritance (DV_CODED_TEXT is a DV_TEXT): a supertype constraint with a
sibling subtype constraint with occurrences 0..0 means "this node can hold the supertype but not the subtype".
Run: python3 subtype_alternatives.py
Also used to check RmValidator2 (alternatives are an OR, but overlapping sibling constraints must be considered)."""
from fixture_lib import *

T = Table('subtype_alternatives', 'Rule fixtures: alternatives with RM inheritance (DV_TEXT / DV_CODED_TEXT)')
el = element
TEXT_PATTERN = attr_single('value', P, prim('STRING', c_string(pattern='[a-z0-9 ]+')))


def alts(*a):
    return one(el(2, alternatives=list(a)))


T.pair('subtype_prohibited_added', alts(('DV_TEXT', P)), alts(('DV_TEXT', P), ('DV_CODED_TEXT', Z)),
       'DV_TEXT only -> DV_TEXT plus sibling DV_CODED_TEXT 0..0 (the subtype is now explicitly not allowed)')
T.pair('subtype_prohibited_removed', alts(('DV_TEXT', P), ('DV_CODED_TEXT', Z)), alts(('DV_TEXT', P)),
       'DV_TEXT plus sibling DV_CODED_TEXT 0..0 -> DV_TEXT only (the subtype is allowed again)')
T.pair('subtype_prohibited_optional_text_added', alts(('DV_TEXT', E)), alts(('DV_TEXT', E), ('DV_CODED_TEXT', Z)),
       'same as subtype_prohibited_added with DV_TEXT occurrences 0..1')
T.pair('subtype_prohibited_with_constraint_added', alts(('DV_TEXT', P, TEXT_PATTERN)),
       alts(('DV_TEXT', P, TEXT_PATTERN), ('DV_CODED_TEXT', Z)),
       'DV_TEXT with a value pattern -> plus sibling DV_CODED_TEXT 0..0')
T.pair('subtype_prohibited_made_allowed', alts(('DV_TEXT', P), ('DV_CODED_TEXT', Z)), alts(('DV_TEXT', P), ('DV_CODED_TEXT', E)),
       'sibling DV_CODED_TEXT occurrences 0..0 -> 0..1')
T.pair('subtype_supertype_prohibited_added', alts(('DV_CODED_TEXT', P)), alts(('DV_CODED_TEXT', P), ('DV_TEXT', Z)),
       'DV_CODED_TEXT only -> plus sibling DV_TEXT 0..0 (the supertype is not allowed, only coded text; DV_CODED_TEXT is also a DV_TEXT)')
T.pair('subtype_coded_required_vs_text', alts(('DV_TEXT', E), ('DV_CODED_TEXT', E)), alts(('DV_TEXT', E), ('DV_CODED_TEXT', P)),
       'both alternatives optional -> DV_CODED_TEXT required (1..1)')

# ---- ITEM_STRUCTURE (abstract) and its subclasses ITEM_TREE, ITEM_LIST, ITEM_SINGLE, ITEM_TABLE ----------------------------
def structs(*a):
    """ADMIN_ENTRY.data alternatives: [(rm_type, occurrences, node_id)]"""
    return dict(roots=[root('a', None, data_children=''.join(obj(t, o, n) for t, o, n in a))])


T.pair('item_structure_subtype_prohibited_added', structs(('ITEM_STRUCTURE', P, 'at0001')),
       structs(('ITEM_STRUCTURE', P, 'at0001'), ('ITEM_TABLE', Z, 'at0002')),
       'ADMIN_ENTRY.data: ITEM_STRUCTURE (any) -> plus sibling ITEM_TABLE 0..0 (any structure except a table)')
T.pair('item_structure_subtypes_prohibited_added', structs(('ITEM_STRUCTURE', P, 'at0001')),
       structs(('ITEM_STRUCTURE', P, 'at0001'), ('ITEM_TABLE', Z, 'at0002'), ('ITEM_LIST', Z, 'at0003'), ('ITEM_SINGLE', Z, 'at0004')),
       'ADMIN_ENTRY.data: ITEM_STRUCTURE (any) -> plus ITEM_TABLE, ITEM_LIST and ITEM_SINGLE 0..0 (only ITEM_TREE remains)')
T.pair('item_structure_subtype_prohibited_removed',
       structs(('ITEM_STRUCTURE', P, 'at0001'), ('ITEM_TABLE', Z, 'at0002')), structs(('ITEM_STRUCTURE', P, 'at0001')),
       'ADMIN_ENTRY.data: ITEM_STRUCTURE plus ITEM_TABLE 0..0 -> ITEM_STRUCTURE only')
T.pair('item_structure_to_concrete', structs(('ITEM_STRUCTURE', P, 'at0001')), structs(('ITEM_TREE', P, 'at0001')),
       'ADMIN_ENTRY.data: ITEM_STRUCTURE (any structure) -> ITEM_TREE only (type narrowed to one subclass)')

# ACTOR subclasses (PERSON, ORGANISATION, GROUP, AGENT) would follow the same pattern, but they only appear as the
# root of a demographic template, so no fixture is built until a use case shows where such alternatives occur.

if __name__ == '__main__':
    T.write()
