#!/usr/bin/env python3
"""Fixtures for table 6.2 of opt_diff_rules.md (removal and widening to the RM). Run: python3 table_6_2.py"""
from fixture_lib import *

T = Table('table_6_2', 'Rule fixtures, table 6.2: removal and widening to the RM')
el = element

# ---- removed constraint node: paths below it are OPT-specific (node_id) -> not re-created by the RM -------------------------
T.pair('removed_member_optional', one(el(2), el(3, E)), one(el(2)), 'ELEMENT member at0003 (optional) removed')
T.pair('removed_member_required', one(el(2), el(3, P)), one(el(2)), 'ELEMENT member at0003 (required) removed')
T.pair('removed_cluster_subtree_optional', one(el(2), cluster(3, E, [el(4), el(5, P)])), one(el(2)),
       'optional CLUSTER with children removed: only the topmost node is reported')
T.pair('removed_cluster_subtree_required', one(el(2), cluster(3, P, [el(4), el(5, P)])), one(el(2)),
       'required CLUSTER with children removed')
T.pair('removed_member_inside_cluster', one(cluster(2, E, [el(3), el(4)])), one(cluster(2, E, [el(3)])),
       'ELEMENT removed from inside a CLUSTER')
T.pair('removed_items_attribute', one(el(2), el(3)), dict(roots=[root('a', None)]),
       'the whole `items` attribute (with its members) removed')
T.pair('removed_alternative_of_two', one(el(2, alternatives=[('DV_TEXT', E), ('DV_CODED_TEXT', E)])),
       one(el(2, alternatives=[('DV_TEXT', E)])), 'id-less alternative DV_CODED_TEXT removed, DV_TEXT stays')
T.pair('removed_alternative_required_of_two', one(el(2, alternatives=[('DV_TEXT', E), ('DV_CODED_TEXT', P)])),
       one(el(2, alternatives=[('DV_TEXT', E)])), 'the required alternative of two removed')
T.pair('removed_root_optional', dict(roots=[root('a', [el(2)], occ=E), root('b', [el(2)])]), dict(roots=[root('b', [el(2)])]),
       'optional archetype root removed (with its whole subtree)')
T.pair('removed_root_required', dict(roots=[root('a', [el(2)], occ=P), root('b', [el(2)])]), dict(roots=[root('b', [el(2)])]),
       'required archetype root removed')

# ---- removed constraint node that complete() re-creates from the RM: the path still exists ----------------------------------
T.pair('removed_value_attribute_optional', one(el(2, value_existence=E)), one(el(2, alternatives=None)),
       'ELEMENT.value attribute removed (existence was 0..1): RM attribute, falls back to the RM')
T.pair('removed_value_attribute_required', one(el(2, value_existence=P)), one(el(2, alternatives=None)),
       'ELEMENT.value attribute removed (existence was 1..1)')
T.pair('removed_sole_alternative_object', one(el(2, alternatives=[('DV_TEXT', E)])), one(el(2, alternatives=[])),
       'the only alternative removed, the attribute `value` stays without children (any value type)')
T.pair('removed_all_alternatives', one(el(2, alternatives=[('DV_TEXT', E), ('DV_CODED_TEXT', E)])), one(el(2, alternatives=[])),
       'both alternatives removed, the attribute `value` stays without children')
T.pair('removed_rm_attribute_primitive', one(el(2, alternatives=[('DV_PROPORTION', P, proportion_type((2,)))])),
       one(el(2, alternatives=[('DV_PROPORTION', P)])), 'DV_PROPORTION.type (INTEGER [2]) attribute removed: falls back to [0..4]')
T.pair('removed_rm_attribute_code', one(el(2, alternatives=[('DV_CODED_TEXT', P, coded_text_code())])),
       one(el(2, alternatives=[('DV_CODED_TEXT', P)])), 'DV_CODED_TEXT.defining_code attribute removed: any code')
T.pair('removed_rm_attribute_magnitude', one(el(2, alternatives=[('DV_COUNT', P, count_magnitude(0))])),
       one(el(2, alternatives=[('DV_COUNT', P)])), 'DV_COUNT.magnitude attribute ([0..*)) removed: falls back to (*..*)')
T.pair('removed_context_attribute', dict(roots=[root('a', [el(2)])], context=E), dict(roots=[root('a', [el(2)])]),
       'COMPOSITION.context attribute removed (RM-defined)')

# ---- constraint replaced by `any`: the node stays, its value constraint goes ---------------------------------------------
T.pair('any_primitive_integer_range', one(el(2, alternatives=[('DV_COUNT', P, attr_single('magnitude', P, prim_int(P, rng=(0, None))))])),
       one(el(2, alternatives=[('DV_COUNT', P, attr_single('magnitude', P, prim_int(P, rng=(None, None))))])),
       'DV_COUNT.magnitude INTEGER [0..*) -> explicit open range (*..*) (any). A C_PRIMITIVE_OBJECT without item is not a valid OPT')
T.pair('any_code_list', one(el(2, alternatives=[('DV_CODED_TEXT', P, attr_single('defining_code', P, code_phrase(('433', '434'))))])),
       one(el(2, alternatives=[('DV_CODED_TEXT', P, attr_single('defining_code', P, code_phrase(())))])),
       'CODE_PHRASE code list [433, 434] -> only the terminology (any code of openehr)')
T.pair('any_code_phrase_terminology', one(el(2, alternatives=[('DV_CODED_TEXT', P, attr_single('defining_code', P, code_phrase(('433',))))])),
       one(el(2, alternatives=[('DV_CODED_TEXT', P, attr_single('defining_code', P, code_phrase((), 'local')))])),
       'CODE_PHRASE terminology openehr + code 433 -> terminology local with no codes')

if __name__ == '__main__':
    T.write()
