#!/usr/bin/env python3
"""Fixtures for table 6.4 of opt_diff_rules.md (alternatives, type and identity changes, ambiguous pairing).
Subtype exclusion (DV_TEXT + DV_CODED_TEXT 0..0, ITEM_STRUCTURE subclasses) is in subtype_alternatives.py.
Run: python3 table_6_4.py"""
from fixture_lib import *

T = Table('table_6_4', 'Rule fixtures, table 6.4: alternatives, type and identity changes, ambiguous pairing')
el = element
CNT = count_magnitude(0)       # DV_COUNT alternative with a real constraint: magnitude >= 0


def alts(*a):
    return one(el(2, alternatives=list(a)))


# ---- alternatives added (extended) / occurrences of an alternative ------------------------------------------------------------
T.pair('alt_added_over_any', alts(), alts(('DV_TEXT', E)),
       'ELEMENT.value without alternatives (any) -> one DV_TEXT alternative 0..1 matching anything: extended, no impact')
T.pair('alt_added_beside_existing', alts(('DV_TEXT', E)), alts(('DV_TEXT', E), ('DV_QUANTITY', E)),
       'DV_TEXT 0..1 -> plus DV_QUANTITY 0..1 matching anything: extended, no impact')
T.pair('alt_added_with_constraints_over_any', alts(), alts(('DV_COUNT', E, CNT)),
       'any -> DV_COUNT 0..1 with magnitude >= 0: extended, data potential (old DV_COUNT data may fail)')
T.pair('alt_added_with_constraints_beside_existing', alts(('DV_TEXT', E)), alts(('DV_TEXT', E), ('DV_COUNT', E, CNT)),
       'DV_TEXT 0..1 -> plus DV_COUNT 0..1 with magnitude >= 0')
T.pair('alt_added_required_over_any', alts(), alts(('DV_TEXT', P)),
       'any -> DV_TEXT 1..1: narrowed, every other type is now invalid')
T.pair('alt_added_required_beside_existing', alts(('DV_TEXT', E)), alts(('DV_TEXT', E), ('DV_QUANTITY', P)),
       'DV_TEXT 0..1 -> plus DV_QUANTITY 1..1: narrowed (the value must be a quantity)')
T.pair('alt_occurrences_raised', alts(('DV_TEXT', E)), alts(('DV_TEXT', P)),
       'sole alternative DV_TEXT 0..1 -> 1..1: narrowed, other types no longer allowed')
T.pair('alt_occurrences_lowered', alts(('DV_TEXT', P)), alts(('DV_TEXT', E)),
       'sole alternative DV_TEXT 1..1 -> 0..1: widened, other types allowed again')
T.pair('alt_removed_optional', alts(('DV_TEXT', E), ('DV_QUANTITY', E)), alts(('DV_TEXT', E)),
       'DV_QUANTITY 0..1 alternative removed: widened (see also table 6.2)')

# ---- members added (see also table 6.1 added nodes) ----------------------------------------------------------------------------
T.pair('member_added_optional', one(el(2)), one(el(2), el(3, E)), 'optional ELEMENT member added: extended, no impact')
T.pair('member_added_required', one(el(2)), one(el(2), el(3, P)), 'required ELEMENT member added: narrowed, data potential')

# ---- RM type changed ---------------------------------------------------------------------------------------------------------
T.pair('rm_type_member_element_to_cluster', one(el(2)), one(cluster(2, E, [el(3)])),
       'member at0002 ELEMENT -> CLUSTER, same node_id (potential for data and queries)')
T.pair('rm_type_structure', dict(roots=[root('a', [el(2)])]),
       dict(roots=[root('a', None, data_children=obj('ITEM_LIST', P, 'at0001'))]),
       'ADMIN_ENTRY.data ITEM_TREE -> ITEM_LIST, same node_id')
T.pair('rm_type_alternative_idless', alts(('DV_TEXT', P)), alts(('DV_COUNT', P)),
       'sole id-less alternative DV_TEXT -> DV_COUNT: type changed, or removed plus added? (ambiguous pairing, see section 7)')
T.pair('rm_type_leaf_primitive', alts(('DV_TEXT', P, attr_single('value', P, prim('STRING', c_string(pattern='[a-z]+'))))),
       alts(('DV_TEXT', P, attr_single('value', P, prim('INTEGER', c_int(rng=(0, None)))))),
       'DV_TEXT.value primitive STRING pattern -> INTEGER range (id-less leaf whose RM type changes)')

# ---- archetype_id changed ---------------------------------------------------------------------------------------------------
def a(ver, texts=None, count_lo=0, extra=()):
    return dict(roots=[root('a', [el(2, alternatives=[('DV_COUNT', E, count_magnitude(count_lo))]), *extra],
                            ver=ver, texts=texts)])


T.pair('archetype_id_changed_nothing_below', a(1), a(2),
       'archetype root openEHR-EHR-ADMIN_ENTRY.rule_fixture_a.v1 -> .v2, content identical')
T.pair('archetype_id_version_downgraded', a(2), a(1),
       'archetype root openEHR-EHR-ADMIN_ENTRY.rule_fixture_a.v2 -> .v1 (version only, same as an update), content identical')
T.pair('archetype_id_specialisation_added', dict(roots=[root('a', [el(2)])]), dict(roots=[root('a-sp', [el(2)])]),
       'rule_fixture_a.v1 -> rule_fixture_a-sp.v1 (specialisation added, same concept): roots pair, an update like a version change; '
       'node pairing inside across specialisation codes is still to be defined (section 6.4)')
T.pair('archetype_id_concept_changed', dict(roots=[root('a', [el(2)])]), dict(roots=[root('b', [el(2)])]),
       'concept part rule_fixture_a -> rule_fixture_b (same version, same content): a different archetype, '
       'so root a removed + root b added, two independent items')
T.pair('archetype_id_concept_and_version_changed', dict(roots=[root('a', [el(2)], ver=1)]), dict(roots=[root('b', [el(2)], ver=2)]),
       'rule_fixture_a.v1 -> rule_fixture_b.v2: the concept differs, so removed + added (not an update)')
T.pair('archetype_id_changed_lexical', a(1), a(2, texts={2: 'renamed node 2'}),
       'archetype_id .v1 -> .v2 and only a term text changed below (lexical)')
T.pair('archetype_id_changed_data_constraint', a(1), a(2, count_lo=5),
       'archetype_id .v1 -> .v2 and a constraint narrowed below (DV_COUNT magnitude >= 0 -> >= 5)')
T.pair('archetype_id_changed_node_removed', a(1, extra=[el(3)]), a(2),
       'archetype_id .v1 -> .v2 and a member removed below')
T.pair('archetype_id_changed_node_added', a(1), a(2, extra=[el(3, P)]),
       'archetype_id .v1 -> .v2 and a required member added below')

# ---- node_id changed = removed + added (two independent items) ----------------------------------------------------------------------------------------------------------
T.pair('node_id_changed_single', one(el(2)), one(el(5)), 'only member: node_id at0002 -> at0005, same content: removed at0002 + added at0005, two items')
T.pair('node_id_changed_with_siblings', one(el(2), el(3, alternatives=[('DV_COUNT', E, CNT)])),
       one(el(2), el(5, alternatives=[('DV_COUNT', E, CNT)])),
       'two members, the second changes node_id at0003 -> at0005 (same content, different from its sibling)')
T.pair('node_id_changed_cluster', one(cluster(2, E, [el(3), el(4)])), one(cluster(6, E, [el(3), el(4)])),
       'CLUSTER at0002 -> at0006 with identical children')
T.pair('node_id_changed_and_constraint', one(el(2, alternatives=[('DV_COUNT', E, CNT)])),
       one(el(5, alternatives=[('DV_COUNT', E, count_magnitude(5))])),
       'node_id at0002 -> at0005 and the DV_COUNT constraint narrowed')

# ---- text only ---------------------------------------------------------------------------------------------------------------
T.pair('text_only_changed', dict(roots=[root('a', [el(2)])]), dict(roots=[root('a', [el(2)], texts={2: 'new text', 0: 'new root text'})]),
       'term texts of at0000 and at0002 changed, same archetype_id and structure: no impact')

# ---- nodes that cannot be paired, or pair as a type change (section 7; more cases in section_7_pairing/) --------------------------
T.pair('amb_member_removed_and_added', one(el(2), el(3, alternatives=[('DV_COUNT', E, CNT)])),
       one(el(2), el(4, alternatives=[('DV_COUNT', E, CNT)])),
       'member at0003 removed and member at0004 with the same content added in the same place (renamed, or removed plus added? not paired: removed + added)')
T.pair('amb_member_replaced_different', one(el(2), el(3, alternatives=[('DV_COUNT', E, CNT)])),
       one(el(2), el(4, alternatives=[('DV_TEXT', E)])),
       'member at0003 removed and member at0004 with different content added in the same place')
T.pair('amb_alternative_replaced', alts(('DV_COUNT', E, CNT)), alts(('DV_QUANTITY', E, attr_single('property', E))),
       'id-less alternative DV_COUNT replaced by DV_QUANTITY (one unmatched on each side: paired as a type change)')

if __name__ == '__main__':
    T.write()
