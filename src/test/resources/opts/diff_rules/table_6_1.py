#!/usr/bin/env python3
"""Fixtures for table 6.1 of opt_diff_rules.md (existence, occurrences, cardinality). Run: python3 table_6_1.py"""
from fixture_lib import *

T = Table('table_6_1', 'Rule fixtures, table 6.1: existence, occurrences, cardinality')
el, one_ = element, one

# ---- becomes required --------------------------------------------------------------------------------------------
T.pair('required_value_existence', one(el(2, E, value_existence=E)), one(el(2, E, value_existence=P)),
       'ELEMENT.value existence 0..1 -> 1..1')
T.pair('required_alternative_sole', one(el(2, alternatives=[('DV_TEXT', E)])), one(el(2, alternatives=[('DV_TEXT', P)])),
       'sole alternative DV_TEXT occurrences 0..1 -> 1..1')
T.pair('required_alternative_of_several',
       one(el(2, alternatives=[('DV_TEXT', E), ('DV_CODED_TEXT', E)])),
       one(el(2, alternatives=[('DV_TEXT', P), ('DV_CODED_TEXT', E)])), 'one of two alternatives occurrences 0..1 -> 1..1')
T.pair('required_member_occurrences', one(el(2, E), el(3, E)), one(el(2, P), el(3, E)),
       'member ELEMENT at0002 occurrences 0..1 -> 1..1')
T.pair('required_container_existence', one(el(2), items_existence=E), one(el(2), items_existence=P),
       'multiple attribute `items` existence 0..1 -> 1..1')
T.pair('required_cardinality_min_not_covered', one(el(2), el(3), items_card=(0, None)), one(el(2), el(3), items_card=(2, None)),
       'cardinality 0..* -> 2..*, members optional (not covered by children)')
T.pair('required_cluster_member', one(cluster(2, E, [el(3)]), el(4)), one(cluster(2, P, [el(3)]), el(4)),
       'CLUSTER member (subtree) occurrences 0..1 -> 1..1')
T.pair('required_member_inside_cluster', one(cluster(2, E, [el(3, E)])), one(cluster(2, E, [el(3, P)])),
       'ELEMENT inside an optional CLUSTER occurrences 0..1 -> 1..1')
T.pair('required_root', dict(roots=[root('a', [el(2)], occ=E), root('b', [el(2)])]),
       dict(roots=[root('a', [el(2)], occ=P), root('b', [el(2)])]), 'archetype root (content item) occurrences 0..1 -> 1..1')
T.pair('required_context', dict(roots=[root('a', [el(2)])], context=E), dict(roots=[root('a', [el(2)])], context=P),
       'COMPOSITION.context existence 0..1 -> 1..1')

# ---- cardinality min raised but covered ------------------------------------------------------------------------------
T.pair('cardinality_min_covered', one(el(2, P), el(3, E), items_card=(0, None)), one(el(2, P), el(3, E), items_card=(1, None)),
       'cardinality 0..* -> 1..* while at0002 is already required: covered, nothing to report')
T.pair('cardinality_min_covered_by_sum', one(el(2, P), el(3, P), items_card=(1, None)), one(el(2, P), el(3, P), items_card=(2, None)),
       'cardinality 1..* -> 2..* with two required members: covered by the sum of children minimums')
T.pair('cardinality_min_partly_covered', one(el(2, P), el(3, E), items_card=(0, None)), one(el(2, P), el(3, E), items_card=(2, None)),
       'cardinality 0..* -> 2..* with one required member: effective min goes 1 -> 2, not covered')

# ---- max lowered -------------------------------------------------------------------------------------------------
T.pair('occurrences_max_lowered', one(el(2, (0, 3))), one(el(2, (0, 1))), 'member occurrences 0..3 -> 0..1')
T.pair('occurrences_max_lowered_unbounded', one(el(2, (0, None))), one(el(2, (0, 3))), 'member occurrences 0..* -> 0..3')
T.pair('cardinality_max_lowered', one(el(2, (0, 3)), items_card=(0, 5)), one(el(2, (0, 3)), items_card=(0, 2)),
       'cardinality 0..5 -> 0..2')
T.pair('cardinality_max_lowered_unbounded', one(el(2), items_card=(0, None)), one(el(2), items_card=(0, 4)),
       'cardinality 0..* -> 0..4')

# ---- unique / ordered ------------------------------------------------------------------------------------------------
T.pair('unique_added', one(el(2), unique=False), one(el(2), unique=True), 'cardinality unique false -> true')
T.pair('unique_removed', one(el(2), unique=True), one(el(2), unique=False), 'cardinality unique true -> false')
T.pair('ordered_added', one(el(2), ordered=False), one(el(2), ordered=True), 'cardinality ordered false -> true')
T.pair('ordered_removed', one(el(2), ordered=True), one(el(2), ordered=False), 'cardinality ordered true -> false')

# ---- prohibited (0..0): kind of node x was optional/required -----------------------------------------------------------
# member (ELEMENT, leaf-like)
T.pair('prohibited_member_was_optional', one(el(2, E), el(3)), one(el(2, Z), el(3)), 'ELEMENT member occurrences 0..1 -> 0..0')
T.pair('prohibited_member_was_required', one(el(2, P), el(3)), one(el(2, Z), el(3)), 'ELEMENT member occurrences 1..1 -> 0..0')
# member with subtree (CLUSTER)
T.pair('prohibited_cluster_was_optional', one(cluster(2, E, [el(3)]), el(4)), one(cluster(2, Z, [el(3)]), el(4)),
       'CLUSTER member (with subtree) occurrences 0..1 -> 0..0')
T.pair('prohibited_cluster_was_required', one(cluster(2, P, [el(3)]), el(4)), one(cluster(2, Z, [el(3)]), el(4)),
       'CLUSTER member (with subtree) occurrences 1..1 -> 0..0')
T.pair('prohibited_member_inside_cluster', one(cluster(2, E, [el(3, E), el(4)])), one(cluster(2, E, [el(3, Z), el(4)])),
       'ELEMENT inside a CLUSTER (nested member) occurrences 0..1 -> 0..0')
T.pair('prohibited_cluster_items_container', one(cluster(2, E, [el(3)], items_existence=E)),
       one(cluster(2, E, [el(3)], items_existence=Z)), 'CLUSTER.items container existence 0..1 -> 0..0')
# leaf attribute (single)
T.pair('prohibited_value_existence_was_optional', one(el(2, value_existence=E)), one(el(2, value_existence=Z)),
       'ELEMENT.value existence 0..1 -> 0..0')
T.pair('prohibited_value_existence_was_required', one(el(2, value_existence=P)), one(el(2, value_existence=Z)),
       'ELEMENT.value existence 1..1 -> 0..0')
# alternatives of a single attribute
T.pair('prohibited_alternative_of_two_optional',
       one(el(2, alternatives=[('DV_TEXT', E), ('DV_CODED_TEXT', E)])), one(el(2, alternatives=[('DV_TEXT', E), ('DV_CODED_TEXT', Z)])),
       'one of two alternatives occurrences 0..1 -> 0..0')
T.pair('prohibited_alternative_of_two_required',
       one(el(2, alternatives=[('DV_TEXT', E), ('DV_CODED_TEXT', P)])), one(el(2, alternatives=[('DV_TEXT', E), ('DV_CODED_TEXT', Z)])),
       'the required alternative of two occurrences 1..1 -> 0..0')
T.pair('prohibited_alternative_sole', one(el(2, alternatives=[('DV_TEXT', E)])), one(el(2, alternatives=[('DV_TEXT', Z)])),
       'sole alternative occurrences 0..1 -> 0..0')
T.pair('prohibited_alternative_sole_required', one(el(2, alternatives=[('DV_TEXT', P)])), one(el(2, alternatives=[('DV_TEXT', Z)])),
       'sole alternative occurrences 1..1 -> 0..0')
# container (multiple attribute) and its cardinality
T.pair('prohibited_container_was_optional', one(el(2), items_existence=E), one(el(2), items_existence=Z),
       'ITEM_TREE.items existence 0..1 -> 0..0')
T.pair('prohibited_container_was_required', one(el(2), items_existence=P), one(el(2), items_existence=Z),
       'ITEM_TREE.items existence 1..1 -> 0..0')
T.pair('prohibited_cardinality_zero', one(el(2), items_card=(0, None)), one(el(2), items_card=(0, 0)),
       'ITEM_TREE.items cardinality 0..* -> 0..0 (always empty)')
# intermediate levels
T.pair('prohibited_data_attribute', one(el(2), data_existence=P), one(el(2), data_existence=Z),
       'ADMIN_ENTRY.data existence 1..1 -> 0..0 (RM-mandatory attribute, intermediate level)')
T.pair('prohibited_item_tree_object', one(el(2), tree_occ=P), one(el(2), tree_occ=Z),
       'ITEM_TREE object occurrences 1..1 -> 0..0 (intermediate object)')
# top level
T.pair('prohibited_root_was_optional', dict(roots=[root('a', [el(2)], occ=E), root('b', [el(2)])]),
       dict(roots=[root('a', [el(2)], occ=Z), root('b', [el(2)])]), 'archetype root occurrences 0..1 -> 0..0')
T.pair('prohibited_root_was_required', dict(roots=[root('a', [el(2)], occ=P), root('b', [el(2)])]),
       dict(roots=[root('a', [el(2)], occ=Z), root('b', [el(2)])]), 'archetype root occurrences 1..1 -> 0..0')
T.pair('prohibited_context', dict(roots=[root('a', [el(2)])], context=E), dict(roots=[root('a', [el(2)])], context=Z),
       'COMPOSITION.context existence 0..1 -> 0..0 (top-level attribute)')

# ---- widened ----------------------------------------------------------------------------------------------------
T.pair('widened_value_existence', one(el(2, value_existence=P)), one(el(2, value_existence=E)), 'ELEMENT.value existence 1..1 -> 0..1')
T.pair('widened_member_occurrences', one(el(2, P)), one(el(2, E)), 'member occurrences 1..1 -> 0..1')
T.pair('widened_member_max', one(el(2, (0, 1))), one(el(2, (0, None))), 'member occurrences 0..1 -> 0..*')
T.pair('widened_cardinality', one(el(2), items_card=(1, 3)), one(el(2), items_card=(0, None)), 'cardinality 1..3 -> 0..*')
T.pair('widened_container_existence', one(el(2), items_existence=P), one(el(2), items_existence=E), 'items existence 1..1 -> 0..1')
T.pair('widened_from_prohibited', one(el(2, Z), el(3)), one(el(2, E), el(3)), 'member occurrences 0..0 -> 0..1')
T.pair('widened_alternative_from_prohibited',
       one(el(2, alternatives=[('DV_TEXT', E), ('DV_CODED_TEXT', Z)])), one(el(2, alternatives=[('DV_TEXT', E), ('DV_CODED_TEXT', E)])),
       'alternative occurrences 0..0 -> 0..1')

# ---- added nodes ------------------------------------------------------------------------------------------------
T.pair('added_mandatory_node', one(el(2)), one(el(2), el(3, P)), 'new member at0003 occurrences 1..1')
T.pair('added_optional_node', one(el(2)), one(el(2), el(3, E)), 'new member at0003 occurrences 0..1')
T.pair('added_mandatory_node_in_optional_container', one(el(2), items_existence=E), one(el(2), el(3, P), items_existence=E),
       'new mandatory member under an optional container')
T.pair('added_mandatory_cluster_subtree', one(el(2)), one(el(2), cluster(3, P, [el(4, P)])),
       'new mandatory CLUSTER with a mandatory child (only the topmost node is mandatory-added)')
T.pair('added_optional_cluster_with_mandatory_child', one(el(2)), one(el(2), cluster(3, E, [el(4, P)])),
       'new optional CLUSTER with a mandatory child (the child is only required if the cluster is present)')
T.pair('added_alternative', one(el(2, alternatives=[('DV_TEXT', E)])), one(el(2, alternatives=[('DV_TEXT', E), ('DV_CODED_TEXT', E)])),
       'new alternative DV_CODED_TEXT under ELEMENT.value')
T.pair('added_root', dict(roots=[root('a', [el(2)])]), dict(roots=[root('a', [el(2)]), root('b', [el(2)], occ=P)]),
       'new mandatory archetype root')
T.pair('added_context', dict(roots=[root('a', [el(2)])]), dict(roots=[root('a', [el(2)])], context=E),
       'COMPOSITION.context attribute added (optional)')

if __name__ == '__main__':
    T.write()
