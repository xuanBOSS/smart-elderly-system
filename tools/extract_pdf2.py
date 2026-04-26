# -*- coding: utf-8 -*-
"""Extract text from 测试结果2.pdf via PyMuPDF."""
from pathlib import Path

try:
    import fitz  # PyMuPDF
except ImportError:
    fitz = None

ROOT = Path(__file__).resolve().parent.parent
PDF = ROOT.parent / "测试结果2.pdf"
OUT = ROOT / "tools" / "pdf2_extracted.txt"


def main():
    if fitz is None:
        OUT.write_text("PyMuPDF not installed", encoding="utf-8")
        return
    if not PDF.exists():
        OUT.write_text(f"missing: {PDF}", encoding="utf-8")
        return
    doc = fitz.open(PDF)
    parts = []
    for i, page in enumerate(doc):
        parts.append(f"--- PAGE {i + 1} ---\n")
        parts.append(page.get_text("text"))
    OUT.write_text("\n".join(parts), encoding="utf-8")
    print("wrote", OUT)


if __name__ == "__main__":
    main()
