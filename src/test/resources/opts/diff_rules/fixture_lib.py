"""Shared builders for the OPT rule fixtures (see opt_diff_rules.md). Minimal, valid OPTs:
a COMPOSITION with one or more ADMIN_ENTRY archetype roots, each with an ITEM_TREE whose `items` attribute (multiple)
holds ELEMENT / CLUSTER members. ELEMENT.value (single) has alternatives. Each pair changes exactly one thing."""
import os
import zlib
from xml.dom import minidom

HERE = os.path.dirname(os.path.abspath(__file__))
XSI = 'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"'
E, P, Z = (0, 1), (1, 1), (0, 0)     # optional, required, prohibited


def iv(lo, hi, tag):
    """interval; lo / hi None = unbounded"""
    lunb, unb = lo is None, hi is None
    return (f'<{tag}><lower_included>{"false" if lunb else "true"}</lower_included>'
            f'<upper_included>{"false" if unb else "true"}</upper_included>'
            f'<lower_unbounded>{"true" if lunb else "false"}</lower_unbounded><upper_unbounded>{"true" if unb else "false"}</upper_unbounded>'
            f'{"" if lunb else f"<lower>{lo}</lower>"}{"" if unb else f"<upper>{hi}</upper>"}</{tag}>')


def attr_single(name, existence, children=''):
    return (f'<attributes xsi:type="C_SINGLE_ATTRIBUTE"><rm_attribute_name>{name}</rm_attribute_name>'
            f'{iv(*existence, "existence")}<match_negated>false</match_negated>{children}</attributes>')


def attr_multiple(name, existence, card, children='', ordered=False, unique=False):
    return (f'<attributes xsi:type="C_MULTIPLE_ATTRIBUTE"><rm_attribute_name>{name}</rm_attribute_name>'
            f'{iv(*existence, "existence")}<match_negated>false</match_negated>{children}'
            f'<cardinality><is_ordered>{str(ordered).lower()}</is_ordered><is_unique>{str(unique).lower()}</is_unique>'
            f'{iv(card[0], card[1], "interval")}</cardinality></attributes>')


def obj(rm_type, occ, node_id='', inner='', kind='C_COMPLEX_OBJECT'):
    return (f'<children xsi:type="{kind}"><rm_type_name>{rm_type}</rm_type_name>{iv(*occ, "occurrences")}'
            f'<node_id>{node_id}</node_id>{inner}</children>')


# ---- leaf constraints -------------------------------------------------------------------------------------------
def prim_int(occ=P, values=None, rng=None):
    """C_PRIMITIVE_OBJECT INTEGER; no values/rng = no item (any)"""
    item = ''
    if values is not None:
        item = '<item xsi:type="C_INTEGER"><list>' + '</list><list>'.join(map(str, values)) + '</list></item>'
    elif rng is not None:
        lo, hi = rng
        item = '<item xsi:type="C_INTEGER">' + iv(lo, hi, 'range') + '</item>'
    return obj('INTEGER', occ, '', item, 'C_PRIMITIVE_OBJECT')


def code_phrase(codes=('433',), terminology='openehr', occ=P):
    cl = ''.join(f'<code_list>{c}</code_list>' for c in codes)
    return (f'<children xsi:type="C_CODE_PHRASE"><rm_type_name>CODE_PHRASE</rm_type_name>{iv(*occ, "occurrences")}'
            f'<node_id></node_id><terminology_id><value>{terminology}</value></terminology_id>{cl}</children>')


def prim(rm_type, item_xml, occ=P):
    return obj(rm_type, occ, '', item_xml, 'C_PRIMITIVE_OBJECT')


def c_int(values=None, rng=None):
    if values is not None:
        return '<item xsi:type="C_INTEGER">' + ''.join(f'<list>{v}</list>' for v in values) + '</item>'
    return '<item xsi:type="C_INTEGER">' + iv(*rng, 'range') + '</item>'


def c_real(rng):
    return '<item xsi:type="C_REAL">' + iv(*rng, 'range') + '</item>'


def c_string(values=None, pattern=None):
    if values is not None:
        return '<item xsi:type="C_STRING">' + ''.join(f'<list>{v}</list>' for v in values) + '</item>'
    return f'<item xsi:type="C_STRING"><pattern>{pattern}</pattern></item>'


def c_pattern(kind, pattern):
    """kind: C_DATE, C_TIME, C_DATE_TIME"""
    return f'<item xsi:type="{kind}"><pattern>{pattern}</pattern></item>'


def c_bool(true_valid=True, false_valid=True):
    return (f'<item xsi:type="C_BOOLEAN"><true_valid>{str(true_valid).lower()}</true_valid>'
            f'<false_valid>{str(false_valid).lower()}</false_valid></item>')


def quantity_items(items):
    """items: [(units, magnitude_range or None, precision_range or None)] -> inner xml of a C_DV_QUANTITY"""
    out = '<property><terminology_id><value>openehr</value></terminology_id><code_string>124</code_string></property>'
    for units, mag, prec in items:
        out += '<list>'
        if mag is not None:
            out += iv(*mag, 'magnitude')
        if prec is not None:
            out += iv(*prec, 'precision')
        out += (f'<units>{units}</units><units_system>http://unitsofmeasure.org</units_system>'
                f'<units_display_name>{units}</units_display_name></list>')
    return out


def ordinal_items(values):
    return ''.join(f'<list><value>{v}</value><symbol><defining_code><terminology_id><value>local</value></terminology_id>'
                   f'<code_string>at{4 + v:04d}</code_string></defining_code></symbol></list>' for v in values)


def proportion_type(values=(2,)):
    """DV_PROPORTION alternative with `type` constrained"""
    return attr_single('type', P, prim_int(P, values=list(values)))


def coded_text_code(codes=('433',)):
    return attr_single('defining_code', P, code_phrase(codes))


def count_magnitude(lo=0):
    return attr_single('magnitude', P, prim_int(P, rng=(lo, None)))


# ---- structure --------------------------------------------------------------------------------------------------
def element(n, occ=E, value_existence=E, alternatives=(('DV_TEXT', P),)):
    """alternatives: [(rm_type, occurrences[, inner_xml])]; None = no `value` attribute; [] = attribute without children"""
    node = f'at{n:04d}'
    if alternatives is None:
        return obj('ELEMENT', occ, node)
    alts = ''.join(obj(a[0], a[1], '', a[2] if len(a) > 2 else '', a[3] if len(a) > 3 else 'C_COMPLEX_OBJECT')
                   for a in alternatives)
    return obj('ELEMENT', occ, node, attr_single('value', value_existence, alts))


def cluster(n, occ=E, members=(), items_existence=E, items_card=(0, None)):
    node = f'at{n:04d}'
    inner = attr_multiple('items', items_existence, items_card, ''.join(members)) if members is not None else ''
    return obj('CLUSTER', occ, node, inner)


def root(rid='a', members=(), occ=E, items_existence=E, items_card=(0, None), ordered=False, unique=False,
         data_existence=P, tree_occ=P, data_children=None, ver=1, texts=None):
    """ADMIN_ENTRY archetype root; members=None -> ITEM_TREE without the `items` attribute"""
    items = (attr_multiple('items', items_existence, items_card, ''.join(members), ordered, unique)
             if members is not None else '')
    data = attr_single('data', data_existence, data_children if data_children is not None
                       else obj('ITEM_TREE', tree_occ, 'at0001', items))
    texts = texts or {}
    terms = ''.join(f'<term_definitions code="at{n:04d}"><items id="text">{texts.get(n, f"node {n}")}</items><items id="description"></items>'
                    '</term_definitions>' for n in range(0, 10))
    return (f'<children xsi:type="C_ARCHETYPE_ROOT"><rm_type_name>ADMIN_ENTRY</rm_type_name>{iv(*occ, "occurrences")}'
            f'<node_id>at0000</node_id>{data}<archetype_id><value>openEHR-EHR-ADMIN_ENTRY.rule_fixture_{rid}.v{ver}</value>'
            f'</archetype_id>{terms}</children>')


def pretty(xml):
    """indent with 2 spaces so the fixtures can be read and diffed by hand"""
    text = minidom.parseString(xml.encode('utf-8')).toprettyxml(indent='  ', encoding='UTF-8').decode('utf-8')
    return text


def template(case, version, roots, context=None):
    """roots: list of root(...) xml; context: existence tuple or None (no context attribute)"""
    category = attr_single('category', P, obj('DV_CODED_TEXT', P, '', attr_single('defining_code', P, code_phrase())))
    ctx = attr_single('context', context, obj('EVENT_CONTEXT', P)) if context is not None else ''
    content = attr_multiple('content', E, (1, None), ''.join(roots))
    definition = (f'<definition><rm_type_name>COMPOSITION</rm_type_name>{iv(*P, "occurrences")}<node_id>at0000</node_id>'
                  f'{category}{ctx}{content}'
                  '<archetype_id><value>openEHR-EHR-COMPOSITION.rule_fixture_composition.v1</value></archetype_id>'
                  f'<template_id><value>{case}_{version}</value></template_id>'
                  '<term_definitions code="at0000"><items id="text">rule fixture composition</items>'
                  '<items id="description"></items></term_definitions></definition>')
    return ('<template xmlns="http://schemas.openehr.org/v1" ' + XSI + '><language><terminology_id><value>ISO_639-1</value>'
            '</terminology_id><code_string>en</code_string></language><description><original_author id="date">2026-11-20'
            '</original_author><lifecycle_state>unmanaged</lifecycle_state><details><language><terminology_id>'
            '<value>ISO_639-1</value></terminology_id><code_string>en</code_string></language>'
            '<purpose>OPT diff rule fixture</purpose></details></description>'
            f'<uid><value>00000000-0000-4000-8000-{zlib.crc32((case + version).encode()):012d}</value></uid>'
            f'<template_id><value>{case}_{version}</value></template_id><concept>{case} {version}</concept>'
            f'{definition}</template>')


class Table:
    def __init__(self, folder, title):
        self.folder, self.title, self.cases = folder, title, []

    def pair(self, name, v0, v1, doc):
        """v0 / v1: dict(roots=[...], context=...)"""
        self.cases.append((name, v0, v1, doc))

    def write(self):
        out = os.path.join(HERE, self.folder)
        os.makedirs(out, exist_ok=True)
        for f in os.listdir(out):
            os.remove(os.path.join(out, f))
        lines = [f'# {self.title}', '', 'Generated by the script of this table (do not edit the .opt files by hand). '
                 'Each pair `<case>_v0.opt` -> `<case>_v1.opt` changes one thing. See `opt_diff_rules.md`.', '',
                 '| Case | Change |', '|---|---|']
        for name, v0, v1, doc in self.cases:
            for ver, kw in (('v0', v0), ('v1', v1)):
                with open(os.path.join(out, f'{name}_{ver}.opt'), 'w', encoding='utf-8') as fh:
                    fh.write(pretty(template(name, ver, **kw)))
            lines.append(f'| `{name}` | {doc} |')
        with open(os.path.join(out, 'README.md'), 'w', encoding='utf-8') as fh:
            fh.write('\n'.join(lines) + '\n')
        print(self.folder, len(self.cases), 'pairs')


def one(*members, **root_kw):
    """a template with a single root"""
    return dict(roots=[root('a', list(members), **root_kw)])
