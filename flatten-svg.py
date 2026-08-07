#!/usr/bin/env python3
"""Flatten SVG <use> elements and remove non-drawing Inkscape metadata."""
import copy
import re
import sys
import xml.etree.ElementTree as ET

SVG_NS = "http://www.w3.org/2000/svg"
XLINK_NS = "http://www.w3.org/1999/xlink"

NSMAP = {
    "svg": SVG_NS,
    "xlink": XLINK_NS,
}

ET.register_namespace("", SVG_NS)
ET.register_namespace("xlink", XLINK_NS)
ET.register_namespace("sodipodi", "http://sodipodi.sourceforge.net/DTD/sodipodi-0.dtd")
ET.register_namespace("inkscape", "http://www.inkscape.org/namespaces/inkscape")


def parse_transform(t):
    if not t:
        return (1, 0, 0, 1, 0, 0)
    # Support matrix(a,b,c,d,e,f), translate(x,y), translate(x), scale(s)
    m = re.match(r"matrix\s*\(\s*([\d.\-]+)\s*,?\s*([\d.\-]+)\s*,?\s*([\d.\-]+)\s*,?\s*([\d.\-]+)\s*,?\s*([\d.\-]+)\s*,?\s*([\d.\-]+)\s*\)", t)
    if m:
        return tuple(float(x) for x in m.groups())
    m = re.match(r"translate\s*\(\s*([\d.\-]+)\s*(?:,\s*([\d.\-]+))?\s*\)", t)
    if m:
        tx = float(m.group(1))
        ty = float(m.group(2)) if m.group(2) else 0
        return (1, 0, 0, 1, tx, ty)
    m = re.match(r"scale\s*\(\s*([\d.\-]+)\s*(?:,\s*([\d.\-]+))?\s*\)", t)
    if m:
        sx = float(m.group(1))
        sy = float(m.group(2)) if m.group(2) else sx
        return (sx, 0, 0, sy, 0, 0)
    raise ValueError(f"Unsupported transform: {t}")


def combine(m1, m2):
    """Return m1 * m2 (apply m2 first, then m1)."""
    a1, b1, c1, d1, e1, f1 = m1
    a2, b2, c2, d2, e2, f2 = m2
    return (
        a1 * a2 + b1 * c2,
        a1 * b2 + b1 * d2,
        c1 * a2 + d1 * c2,
        c1 * b2 + d1 * d2,
        a1 * e2 + b1 * f2 + e1,
        c1 * e2 + d1 * f2 + f1,
    )


def transform_to_str(m):
    a, b, c, d, e, f = m
    if b == 0 and c == 0:
        if a == d:
            if a != 1:
                return f"scale({a}) translate({e} {f})"
            return f"translate({e} {f})"
    return f"matrix({a},{b},{c},{d},{e},{f})"


def strip_ns(tag):
    if "}" in tag:
        return tag.split("}", 1)[1]
    return tag


def find_by_id(root, id_):
    for el in root.iter():
        if el.get("id") == id_:
            return el
    return None


def is_drawing(el):
    tag = strip_ns(el.tag)
    return tag in ("g", "path", "rect", "circle", "ellipse", "line", "polyline", "polygon")


def clean_inkscape(el):
    for child in list(el):
        tag = strip_ns(child.tag)
        if tag in ("namedview", "metadata") or child.tag.startswith("{") and "sodipodi" in child.tag:
            el.remove(child)
            continue
        if tag == "defs" and len(child) == 0:
            el.remove(child)
            continue
        clean_inkscape(child)


def remove_guides(el):
    for child in list(el):
        tag = strip_ns(child.tag)
        if tag == "circle" and child.get("fill") == "none" and child.get("stroke") == "#000000":
            el.remove(child)
            continue
        remove_guides(child)


def flatten_use(el, root, depth=0):
    if depth > 20:
        raise RecursionError("Too much use nesting")
    for child in list(el):
        tag = strip_ns(child.tag)
        if tag == "use":
            href = child.get(f"{{{XLINK_NS}}}href") or child.get("href")
            if href and href.startswith("#"):
                target = find_by_id(root, href[1:])
                if target is None:
                    el.remove(child)
                    continue
                clone = copy.deepcopy(target)

                # Keep the use's own id on the replacement group so later <use>s can reference it.
                use_id = child.get("id")
                if use_id:
                    clone.set("id", use_id)
                # Strip ids from descendants to avoid duplicates.
                for desc in clone.iter():
                    if desc is not clone:
                        desc.attrib.pop("id", None)

                # SVG semantics: <use x=".." y=".." transform="T"> references <g transform="G">.
                # The referenced content is first transformed by G, then by the use's x/y+T.
                x = float(child.get("x", "0") or 0)
                y = float(child.get("y", "0") or 0)
                xy_t = (1, 0, 0, 1, x, y)
                use_attr_t = parse_transform(child.get("transform")) if child.get("transform") else (1, 0, 0, 1, 0, 0)
                # final use matrix = use_attr_t * xy_t  (xy inner, use_attr outer)
                use_t = combine(use_attr_t, xy_t)

                target_t = parse_transform(clone.get("transform"))
                final_t = combine(use_t, target_t)

                if final_t != (1, 0, 0, 1, 0, 0):
                    clone.set("transform", transform_to_str(final_t))
                else:
                    clone.attrib.pop("transform", None)

                idx = list(el).index(child)
                el.remove(child)
                el.insert(idx, clone)
                # recursively flatten the clone
                flatten_use(clone, root, depth + 1)
            else:
                el.remove(child)
        else:
            flatten_use(child, root, depth)


def main():
    infile = "/run/media/flavolite/30524ad5-1cdc-4501-a036-1312a6bfad76/Development/NanoLedger/etc/receipt-icon.svg"
    outfile = "/run/media/flavolite/30524ad5-1cdc-4501-a036-1312a6bfad76/Development/NanoLedger/etc/receipt-icon-flat.svg"
    tree = ET.parse(infile)
    root = tree.getroot()
    clean_inkscape(root)
    remove_guides(root)
    flatten_use(root, root)
    tree.write(outfile, encoding="UTF-8", xml_declaration=True)
    print(f"Flattened to {outfile}")


if __name__ == "__main__":
    main()
