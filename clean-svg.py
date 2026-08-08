#!/usr/bin/env python3
"""Promote SVG CSS style properties to presentation attributes.

This is useful before feeding an SVG into svg2vectordrawable, which does not
parse style="fill:..." strings and would otherwise produce an AVD with no
colors."""
import argparse
import xml.etree.ElementTree as ET

SVG_NS = "http://www.w3.org/2000/svg"


def clean(input_path, output_path):
    tree = ET.parse(input_path)
    root = tree.getroot()

    for el in root.iter("{%s}path" % SVG_NS):
        style = el.get("style")
        if style:
            props = {}
            for part in style.split(";"):
                part = part.strip()
                if not part:
                    continue
                if ":" in part:
                    k, v = part.split(":", 1)
                    props[k.strip()] = v.strip()

            for key in (
                "fill",
                "stroke",
                "fill-rule",
                "stroke-width",
                "stroke-linecap",
                "stroke-linejoin",
            ):
                if key in props:
                    el.set(key, props[key])

            el.attrib.pop("style", None)

        # SVG default fill is black; make it explicit for the converter.
        if not el.get("fill") and not el.get("stroke"):
            el.set("fill", "#000000")

    # Strip any non-SVG namespaced attributes (e.g. sodipodi/inkscape)
    for el in root.iter():
        to_remove = [
            k
            for k in el.attrib.keys()
            if k.startswith("{") and not k.startswith("{%s}" % SVG_NS)
        ]
        for k in to_remove:
            el.attrib.pop(k)

    ET.register_namespace("", SVG_NS)
    tree.write(output_path, encoding="UTF-8", xml_declaration=True)
    print(f"Cleaned SVG written to {output_path}")


def main():
    parser = argparse.ArgumentParser(
        description="Clean SVG style attributes for svg2vectordrawable."
    )
    parser.add_argument(
        "-i", "--input", default="etc/receipt-icon.svg", help="Input SVG"
    )
    parser.add_argument(
        "-o", "--output", default="etc/receipt-icon-clean.svg", help="Output SVG"
    )
    args = parser.parse_args()
    clean(args.input, args.output)


if __name__ == "__main__":
    main()
