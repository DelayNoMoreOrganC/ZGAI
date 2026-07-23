import json
import os
import tempfile
import unittest
from pathlib import Path

import fitz
from PIL import Image

from app.engine import _normalize_pdf, analyze_archive, assemble_archive


class ArchiveEngineTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="zgai-archive-test-")
        self.root = Path(self.temp.name).resolve()
        os.environ["ARCHIVE_ALLOWED_ROOTS"] = str(self.root)
        os.environ["ARCHIVE_OCR_CACHE"] = str(self.root / "cache")

    def tearDown(self):
        self.temp.cleanup()

    def test_assemble_preserves_source_pages_and_writes_manifest(self):
        source = self.root / "判决书.pdf"
        pdf = fitz.open()
        for text in ("第一页判决内容", "第二页判决内容"):
            page = pdf.new_page(width=fitz.paper_rect("a4").width, height=fitz.paper_rect("a4").height)
            page.insert_text((72, 72), text, fontname="china-s", fontsize=12)
        pdf.save(source)
        pdf.close()
        output = self.root / "电子卷宗.pdf"

        result = assemble_archive({
            "jobId": 1,
            "templateVersion": "CIVIL_V1",
            "caseNumber": "ZGAI-2026-001",
            "fields": {"案件类别": "民事诉讼", "委托人": "测试客户", "案由": "合同纠纷"},
            "documents": [{
                "itemId": 8, "caseDocumentId": 9, "fileName": source.name,
                "path": str(source), "catalogSeq": 14, "catalogName": "裁定书、判决书、调解书",
            }],
            "outputPath": str(output),
        })

        self.assertTrue(result["success"])
        self.assertEqual(2, result["sourcePageCount"])
        self.assertEqual(0, result["gapPages"])
        self.assertEqual(0, result["duplicatePages"])
        self.assertTrue(output.is_file())
        manifest = Path(result["manifestPath"])
        self.assertTrue(manifest.is_file())
        self.assertEqual(result["sha256"], json.loads(manifest.read_text(encoding="utf-8"))["sha256"])
        final = fitz.open(output)
        self.assertEqual(result["pageCount"], final.page_count)
        self.assertGreaterEqual(len(final.get_toc()), 5)
        final.close()

    def test_image_conversion_uses_a4_without_cropping(self):
        source = self.root / "横向证据.png"
        Image.new("RGB", (1600, 500), "white").save(source)

        normalized, cleanup = _normalize_pdf(source)
        try:
            pdf = fitz.open(normalized)
            self.assertEqual(1, pdf.page_count)
            self.assertAlmostEqual(fitz.paper_rect("a4").width, pdf[0].rect.width, places=1)
            self.assertAlmostEqual(fitz.paper_rect("a4").height, pdf[0].rect.height, places=1)
            pdf.close()
        finally:
            if cleanup:
                import shutil
                shutil.rmtree(cleanup, ignore_errors=True)

    def test_analysis_reads_plain_text_case_material_without_office_conversion(self):
        source = self.root / "利冲审查报告.txt"
        source.write_text("案件名称：测试案件\n经审查未发现利益冲突。", encoding="utf-8")

        result = analyze_archive({
            "jobId": 2,
            "templateVersion": "CIVIL_V1",
            "documents": [{
                "caseDocumentId": 10,
                "fileName": source.name,
                "path": str(source),
                "documentType": "利冲审查报告",
            }],
        })

        self.assertEqual(1, result["documents"][0]["sourcePageCount"])
        self.assertEqual(10, result["documents"][0]["caseDocumentId"])


if __name__ == "__main__":
    unittest.main()
