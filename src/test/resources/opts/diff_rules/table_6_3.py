#!/usr/bin/env python3
"""Fixtures for table 6.3 of opt_diff_rules.md (value constraints of leaf objects). Run: python3 table_6_3.py

Each family changes one leaf constraint of ELEMENT.value (or of a child attribute of the value). `None` as old / new
means the constraint is not in the OPT (no constraint, the RM default)."""
from fixture_lib import *

T = Table('table_6_3', 'Rule fixtures, table 6.3: value constraints (leaf objects)')
el = element


def value_case(name, rm, old, new, doc, kind='C_COMPLEX_OBJECT'):
    """old / new: inner xml of the alternative `rm` (None = no constraint)"""
    mk = lambda inner: one(el(2, alternatives=[(rm, P, inner or '', kind)]))
    T.pair(name, mk(old), mk(new), doc)


def attr_case(name, rm, attr, old, new, doc, existence=P):
    """old / new: item xml of a C_PRIMITIVE_OBJECT under attribute `attr` of `rm` (None = attribute not in the OPT)"""
    prim_type = {'C_INTEGER': 'INTEGER', 'C_REAL': 'REAL', 'C_STRING': 'STRING', 'C_BOOLEAN': 'BOOLEAN',
                 'C_DATE': 'DATE', 'C_TIME': 'TIME', 'C_DATE_TIME': 'DATE_TIME'}
    def mk(item):
        if item is None:
            return None
        t = item.split('"')[1]
        return attr_single(attr, existence, prim(prim_type[t], item))
    value_case(name, rm, mk(old), mk(new), doc)


# ---- integer range: DV_COUNT.magnitude ---------------------------------------------------------------------------------
M = ('DV_COUNT', 'magnitude')
for suffix, old, new, doc in [
    ('narrowed_from_any', None, (0, None), '(*..*) -> [0..*) (constraint added over the RM default)'),
    ('narrowed_from_explicit_open', (None, None), (0, None), 'explicit (*..*) -> [0..*)'),
    ('narrowed_lower_raised', (0, None), (10, None), '[0..*) -> [10..*)'),
    ('narrowed_upper_lowered', (0, 100), (0, 50), '[0..100] -> [0..50]'),
    ('narrowed_both', (0, 100), (10, 50), '[0..100] -> [10..50]'),
    ('narrowed_upper_added', (0, None), (0, 100), '[0..*) -> [0..100]'),
    ('narrowed_to_single_value', (0, 100), (5, 5), '[0..100] -> [5..5]'),
    ('widened_upper_raised', (0, 100), (0, 200), '[0..100] -> [0..200]'),
    ('widened_lower_lowered', (10, 100), (0, 100), '[10..100] -> [0..100]'),
    ('widened_upper_removed', (0, 100), (0, None), '[0..100] -> [0..*)'),
]:
    attr_case(f'integer_range_{suffix}', *M, None if old is None else c_int(rng=old), c_int(rng=new), 'DV_COUNT.magnitude ' + doc)

# ---- real range: DV_PROPORTION.numerator -------------------------------------------------------------------------------
for suffix, old, new, doc in [
    ('narrowed_from_any', None, (0, 100), '(*..*) -> [0..100]'),
    ('narrowed', (0, 100), (0, 50), '[0..100] -> [0..50]'),
    ('widened', (0, 50), (0, 100), '[0..50] -> [0..100]'),
]:
    attr_case(f'real_range_{suffix}', 'DV_PROPORTION', 'numerator', None if old is None else c_real(old), c_real(new),
              'DV_PROPORTION.numerator ' + doc)

# ---- integer list / enumeration: DV_PROPORTION.type ------------------------------------------------------------------
for suffix, old, new, doc in [
    ('narrowed_from_any', None, [2], '[0, 1, 2, 3, 4] (RM) -> [2]'),
    ('value_dropped', [0, 1, 2], [0, 1], '[0, 1, 2] -> [0, 1]'),
    ('values_dropped_to_one', [0, 1, 2], [2], '[0, 1, 2] -> [2]'),
    ('value_added', [0, 1], [0, 1, 2], '[0, 1] -> [0, 1, 2]'),
    ('replaced_value', [0, 1], [0, 2], '[0, 1] -> [0, 2] (one dropped, one added)'),
]:
    attr_case(f'integer_list_{suffix}', 'DV_PROPORTION', 'type', None if old is None else c_int(values=old), c_int(values=new),
              'DV_PROPORTION.type ' + doc)

# ---- precision: DV_PROPORTION.precision ---------------------------------------------------------------------------------
for suffix, old, new, doc in [
    ('narrowed_from_any', None, (0, 2), '(*..*) -> [0..2]'),
    ('narrowed', (0, 2), (1, 1), '[0..2] -> [1..1]'),
    ('widened', (1, 1), (0, 2), '[1..1] -> [0..2]'),
]:
    attr_case(f'precision_{suffix}', 'DV_PROPORTION', 'precision', None if old is None else c_int(rng=old), c_int(rng=new),
              'DV_PROPORTION.precision ' + doc, existence=E)

# ---- date / datetime patterns (C_TIME is not supported by the SDK parser) -------------------------------------------------------------------------------------------------
for name, rm, kind, old, new, doc in [
    ('datetime_narrowed_from_any', 'DV_DATE_TIME', 'C_DATE_TIME', None, 'yyyy-mm-ddThh:mm:ss', 'any -> yyyy-mm-ddThh:mm:ss'),
    ('datetime_narrowed', 'DV_DATE_TIME', 'C_DATE_TIME', 'yyyy-mm-ddThh:??:??', 'yyyy-mm-ddThh:mm:ss', 'yyyy-mm-ddThh:??:?? -> yyyy-mm-ddThh:mm:ss'),
    ('datetime_widened', 'DV_DATE_TIME', 'C_DATE_TIME', 'yyyy-mm-ddThh:mm:ss', 'yyyy-mm-ddThh:??:??', 'yyyy-mm-ddThh:mm:ss -> yyyy-mm-ddThh:??:??'),
    ('date_narrowed_from_any', 'DV_DATE', 'C_DATE', None, 'yyyy-mm-dd', 'any -> yyyy-mm-dd'),
    ('date_narrowed', 'DV_DATE', 'C_DATE', 'yyyy-??-XX', 'yyyy-mm-dd', 'yyyy-??-XX -> yyyy-mm-dd'),
    ('date_widened', 'DV_DATE', 'C_DATE', 'yyyy-mm-dd', 'yyyy-??-XX', 'yyyy-mm-dd -> yyyy-??-XX'),
]:
    attr_case(f'pattern_{name}', rm, 'value', None if old is None else c_pattern(kind, old), c_pattern(kind, new),
              f'{rm}.value pattern {doc}')

# ---- strings: DV_TEXT.value ------------------------------------------------------------------------------------------------
for suffix, old, new, doc in [
    ('list_narrowed_from_any', None, dict(values=['a', 'b']), 'any -> list [a, b]'),
    ('list_value_dropped', dict(values=['a', 'b', 'c']), dict(values=['a', 'b']), 'list [a, b, c] -> [a, b]'),
    ('list_value_added', dict(values=['a', 'b']), dict(values=['a', 'b', 'c']), 'list [a, b] -> [a, b, c]'),
    ('pattern_narrowed_from_any', None, dict(pattern='[a-z]+'), 'any -> pattern [a-z]+'),
    ('pattern_changed', dict(pattern='[a-z]+'), dict(pattern='[a-c]+'), 'pattern [a-z]+ -> [a-c]+'),
    ('list_replaced_by_pattern', dict(values=['a', 'b']), dict(pattern='[a-z]+'), 'list [a, b] -> pattern [a-z]+'),
]:
    attr_case(f'string_{suffix}', 'DV_TEXT', 'value', None if old is None else c_string(**old), c_string(**new), 'DV_TEXT.value ' + doc)

# ---- boolean: DV_BOOLEAN.value -----------------------------------------------------------------------------------------------
for suffix, old, new, doc in [
    ('narrowed_from_any', None, (True, False), '[true, false] (RM) -> [true]'),
    ('narrowed', (True, True), (True, False), '[true, false] -> [true]'),
    ('widened', (True, False), (True, True), '[true] -> [true, false]'),
]:
    attr_case(f'boolean_{suffix}', 'DV_BOOLEAN', 'value', c_bool(*old) if old else None, c_bool(*new), 'DV_BOOLEAN.value ' + doc)

# ---- code phrase: DV_CODED_TEXT.defining_code -------------------------------------------------------------------------------------
cp = lambda codes, term='openehr': attr_single('defining_code', P, code_phrase(codes, term))
for suffix, old, new, doc in [
    ('narrowed_from_any_code', cp(()), cp(('433',)), 'terminology openehr, any code -> code list [433]'),
    ('value_dropped', cp(('433', '434')), cp(('433',)), 'code list [433, 434] -> [433]'),
    ('values_dropped_all_but_one', cp(('433', '434', '435')), cp(('435',)), 'code list [433, 434, 435] -> [435]'),
    ('value_added', cp(('433',)), cp(('433', '434')), 'code list [433] -> [433, 434]'),
    ('replaced_value', cp(('433', '434')), cp(('433', '435')), 'code list [433, 434] -> [433, 435]'),
]:
    value_case(f'code_list_{suffix}', 'DV_CODED_TEXT', old, new, 'DV_CODED_TEXT.defining_code ' + doc)

# ---- quantity items: C_DV_QUANTITY ---------------------------------------------------------------------------------------------------
Q = 'DV_QUANTITY'
def quantity_case(name, old, new, doc):
    value_case(name, Q, None if old is None else quantity_items(old), quantity_items(new), 'DV_QUANTITY ' + doc, 'C_DV_QUANTITY')
quantity_case('quantity_magnitude_narrowed_from_any', [('mg', None, None)], [('mg', (0.0, None), None)], 'mg magnitude (*..*) -> [0.0..*)')
quantity_case('quantity_magnitude_narrowed', [('mg', (0.0, None), None)], [('mg', (1.0, None), None)], 'mg magnitude [0.0..*) -> [1.0..*)')
quantity_case('quantity_magnitude_upper_lowered', [('mg', (0.0, 100.0), None)], [('mg', (0.0, 50.0), None)], 'mg magnitude [0.0..100.0] -> [0.0..50.0]')
quantity_case('quantity_magnitude_widened', [('mg', (1.0, None), None)], [('mg', (0.0, None), None)], 'mg magnitude [1.0..*) -> [0.0..*)')
quantity_case('quantity_precision_narrowed_from_any', [('mg', None, None)], [('mg', None, (0, 2))], 'mg precision (*..*) -> [0..2]')
quantity_case('quantity_precision_narrowed', [('mg', None, (0, 2))], [('mg', None, (1, 1))], 'mg precision [0..2] -> [1..1]')
quantity_case('quantity_precision_widened', [('mg', None, (1, 1))], [('mg', None, (0, 2))], 'mg precision [1..1] -> [0..2]')
quantity_case('quantity_unit_dropped', [('mg', None, None), ('g', None, None)], [('mg', None, None)], 'units [mg, g] -> [mg]')
quantity_case('quantity_unit_added', [('mg', None, None)], [('mg', None, None), ('g', None, None)], 'units [mg] -> [mg, g]')
quantity_case('quantity_unit_magnitude_one_of_two', [('mg', (0.0, None), None), ('g', (0.0, None), None)],
              [('mg', (0.0, None), None), ('g', (1.0, None), None)], 'only the g item: magnitude [0.0..*) -> [1.0..*)')
T.pair('quantity_units_added_from_any', one(el(2, alternatives=[('DV_QUANTITY', P)])),
       one(el(2, alternatives=[('DV_QUANTITY', P, quantity_items([('mg', None, None)]), 'C_DV_QUANTITY')])),
       'DV_QUANTITY without constraint (any quantity) -> C_DV_QUANTITY with units [mg]')

# ---- ordinal: C_DV_ORDINAL ------------------------------------------------------------------------------------------------------------
def ordinal_case(name, old, new, doc):
    value_case(name, 'DV_ORDINAL', ordinal_items(old), ordinal_items(new), 'DV_ORDINAL ' + doc, 'C_DV_ORDINAL')
ordinal_case('ordinal_value_dropped', [1, 2, 3], [1, 2], 'values [1, 2, 3] -> [1, 2]')
ordinal_case('ordinal_value_added', [1, 2], [1, 2, 3], 'values [1, 2] -> [1, 2, 3]')
ordinal_case('ordinal_values_replaced', [1, 2], [1, 3], 'values [1, 2] -> [1, 3]')
T.pair('ordinal_narrowed_from_any', one(el(2, alternatives=[('DV_ORDINAL', P)])),
       one(el(2, alternatives=[('DV_ORDINAL', P, ordinal_items([1, 2]), 'C_DV_ORDINAL')])),
       'DV_ORDINAL without constraint -> C_DV_ORDINAL with values [1, 2]')

if __name__ == '__main__':
    T.write()
