#!/usr/bin/env python3
"""Test fixtures for section 7 of opt_diff_rules.md (pairing of constraint nodes, type changes, nodes that cannot be paired).
Expected behaviour: one unmatched node on each side under the same attribute = type change; more than one unmatched
on a side (or any node_id change) = no pairing, reported as removed + added with an explanation, and
the migration plan asks the question. Unambiguous changes elsewhere are reported individually.
Each case says what is expected. `observe_section_7.sh` records what the SDK reports today.
Run: python3 section_7_pairing.py"""
from fixture_lib import *

T = Table('section_7_pairing', 'Rule fixtures, section 7: pairing of nodes, type changes, nodes that cannot be paired')
el = element


def C(lo):                 # id-less DV_COUNT alternative, magnitude >= lo
    return ('DV_COUNT', E, count_magnitude(lo))


def alts(*a, **kw):
    return one(el(2, alternatives=list(a)), **kw)


# ---- A. id-less node whose RM type changes: pairs as a type change, or cannot be paired --------------------------------------
# Expected (opt_diff_rules.md section 7): exactly one unmatched node on each side under the same attribute = type change of
# that path (data potential, queries potential). More than one unmatched on a side = the diff does not pair them: they are
# reported as removed + added (data potential, queries potential) with an explanation, and the migration plan asks the question.
T.pair('A1_alternative_type_changed', alts(C(0)), alts(('DV_QUANTITY', E)),
       'Type change. Sole alternative DV_COUNT -> DV_QUANTITY: one unmatched node on each side, paired as a type change. '
       'Data potential, queries potential.')
T.pair('A2_alternative_replaced_similar_subtree', alts(C(0)), alts(('DV_PROPORTION', E, count_magnitude(0))),
       'Type change. DV_COUNT -> DV_PROPORTION, both with a magnitude >= 0 child: the similar subtree does not matter, '
       'one unmatched on each side is a type change.')
T.pair('A3_leaf_primitive_type_changed',
       alts(('DV_TEXT', P, attr_single('value', P, prim('STRING', c_string(pattern='[a-z]+'))))),
       alts(('DV_TEXT', P, attr_single('value', P, prim('INTEGER', c_int(rng=(0, None)))))),
       'Type change of an id-less leaf: DV_TEXT.value STRING pattern -> INTEGER range. Data and queries potential.')
T.pair('A4_alternative_removed_other_added', alts(C(0), ('DV_TEXT', E)), alts(C(0), ('DV_QUANTITY', E)),
       'Type change beside an unchanged alternative: DV_TEXT -> DV_QUANTITY while DV_COUNT is unchanged. '
       'The unchanged DV_COUNT pairs cleanly and is not reported.')
T.pair('A5_two_unmatched_each_side', alts(('DV_TEXT', E), C(0)), alts(('DV_QUANTITY', E), ('DV_PROPORTION', E)),
       'CANNOT PAIR. {DV_TEXT, DV_COUNT} -> {DV_QUANTITY, DV_PROPORTION}: two unmatched on each side, no pairing is made. '
       'Reported as 2 removed + 2 added (data and queries potential) with an explanation; the plan asks the question.')
T.pair('A6_type_changes_at_different_levels',
       one(el(2, alternatives=[C(0)]), cluster(3, E, [el(4, alternatives=[('DV_TEXT', E)])])),
       one(el(2, alternatives=[('DV_QUANTITY', E)]), cluster(3, E, [el(4, alternatives=[('DV_CODED_TEXT', E)])])),
       'Two independent type changes at different depths (ELEMENT at0002 directly under the root, ELEMENT at0004 nested in '
       'CLUSTER at0003). Each is its own type change; neither affects the other, and the CLUSTER is not reported as changed.')

# ---- B. nodes with node_id: a node_id change is always removed + added (two independent items) -----------------------------------------------------------------------
T.pair('B1_member_id_renamed_identical_siblings', one(el(2, alternatives=[C(0)]), el(3, alternatives=[C(0)])),
       one(el(2, alternatives=[C(0)]), el(4, alternatives=[C(0)])),
       'node_id at0003 -> at0004 on a member with identical content to its sibling. Two independent items: at0003 removed '
       '(data affected, queries potential), at0004 added; each with its own explanation. The plan asks whether it is a rename.')
T.pair('B2_member_removed_and_added_different', one(el(2), el(3, alternatives=[C(0)])),
       one(el(2), el(4, alternatives=[('DV_TEXT', E)])),
       'Member at0003 (DV_COUNT) removed and at0004 (DV_TEXT) added: not pairable by content, reported as removed + added.')

# ---- C. controls: id-less siblings that ARE distinguishable ---------------------------------------------------------------------
T.pair('C1_quantity_items_distinguishable_by_units',
       alts(('DV_QUANTITY', E, quantity_items([('mg', (0, 10), None), ('g', (0, 5), None)]), 'C_DV_QUANTITY')),
       alts(('DV_QUANTITY', E, quantity_items([('mg', (0, 20), None), ('g', (0, 5), None)]), 'C_DV_QUANTITY')),
       'Control: C_DV_QUANTITY_ITEMs are paired by units, so the mg range change is NOT ambiguous.')
T.pair('C2_different_types_one_changes', alts(C(0), ('DV_QUANTITY', E, quantity_items([('mg', None, None)]), 'C_DV_QUANTITY')),
       alts(C(5), ('DV_QUANTITY', E, quantity_items([('mg', None, None)]), 'C_DV_QUANTITY')),
       'Control: alternatives of different RM types are paired by type, the DV_COUNT change is NOT ambiguous.')

if __name__ == '__main__':
    T.write()
