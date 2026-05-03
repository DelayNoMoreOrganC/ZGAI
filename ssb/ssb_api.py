#!/usr/bin/python3
# -*- coding: utf-8 -*-
"""
省时宝 (SSB) API 包装器 - 集成到 ZGAI 律所智能案件管理系统
为 Java 后端提供 REST API 代理，端口 5002
"""

import sys
import os
import json
import traceback
import tempfile
import shutil
from pathlib import Path
from datetime import datetime

# 设置路径：引用 SSB 仓库的 server 目录
SSB_REPO_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'ssb-repo', 'server'))
sys.path.insert(0, SSB_REPO_DIR)

# 切换到 SSB server 目录作为工作目录（模板库路径基于此）
os.chdir(SSB_REPO_DIR)

from flask import Flask, request, jsonify, send_file
from flask_cors import CORS

# 导入 SSB 核心模块
from core.project_manager import get_projects_list, get_project_files
from core.document_processor import process_documents_with_tracking
from core.ai_extractor import extract_pdf_data
from core.excel_processor import get_project_excel_data
from config.settings import TEMPLATE_DIR, GENERATED_FILES_DIR

app = Flask(__name__)
CORS(app)

# ============================================================
# 健康检查
# ============================================================
@app.route('/api/health', methods=['GET'])
def health():
    """健康检查"""
    template_count = 0
    if os.path.exists(TEMPLATE_DIR):
        template_count = len([d for d in os.listdir(TEMPLATE_DIR)
                             if os.path.isdir(os.path.join(TEMPLATE_DIR, d))])
    return jsonify({
        'success': True,
        'service': 'shengshibao',
        'version': '2.0.0',
        'timestamp': datetime.now().isoformat(),
        'template_count': template_count,
        'template_dir_exists': os.path.exists(TEMPLATE_DIR)
    })


# ============================================================
# 模板管理 API
# ============================================================
@app.route('/api/templates', methods=['GET'])
def list_templates():
    """获取所有项目模板列表"""
    try:
        result = get_projects_list()
        return jsonify(result)
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500


@app.route('/api/templates/<path:project_path>/files', methods=['GET'])
def get_template_files(project_path):
    """获取指定模板项目的文件列表"""
    try:
        result = get_project_files(project_path)
        return jsonify(result)
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500


@app.route('/api/templates/<path:project_path>/fields', methods=['GET'])
def get_template_fields(project_path):
    """获取模板项目中的字段定义（从 Excel 提取要素内容）"""
    try:
        result = get_project_excel_data(project_path)
        if result['success']:
            return jsonify({
                'success': True,
                'project_path': project_path,
                'fields': result.get('fields', [])
            })
        return jsonify(result), 404
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500


# ============================================================
# 文档生成 API
# ============================================================
@app.route('/api/generate', methods=['POST'])
def generate_documents():
    """
    根据模板和提取数据生成文档

    请求体 (JSON):
    {
        "project_path": "建设银行-个人快贷",
        "extracted_data": {
            "被告姓名": "张三",
            "身份证号": "440...",
            ...
        },
        "output_dir": "/optional/custom/output/path"  // 可选
    }
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({'success': False, 'error': '请求体不能为空'}), 400

        project_path = data.get('project_path', '')
        extracted_data = data.get('extracted_data', {})

        if not project_path:
            return jsonify({'success': False, 'error': '缺少 project_path'}), 400
        if not extracted_data:
            return jsonify({'success': False, 'error': '缺少 extracted_data'}), 400

        # 构建模板文件路径
        template_path = os.path.join(TEMPLATE_DIR, project_path)
        if not os.path.exists(template_path):
            return jsonify({'success': False, 'error': f'模板项目不存在: {project_path}'}), 404

        # 设置输出目录
        if data.get('output_dir'):
            output_path = data['output_dir']
        else:
            # 生成带时间戳的输出目录
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            person_name = extracted_data.get('被告姓名', extracted_data.get('姓名', 'unknown'))
            foldername = f"{project_path}_{person_name}_{timestamp}"
            output_path = os.path.join(GENERATED_FILES_DIR, foldername)

        os.makedirs(output_path, exist_ok=True)

        # 调用 SSB 核心文档生成
        result = process_documents_with_tracking(template_path, extracted_data, output_path)

        if result.get('error'):
            return jsonify({
                'success': False,
                'error': result['error'],
                'details': result
            }), 500

        return jsonify({
            'success': True,
            'output_path': output_path,
            'total_files': result.get('success_count', 0),
            'success_count': result.get('success_count', 0),
            'failed_count': result.get('failed_count', 0),
            'files': result.get('details', []),
            'missing_variables': result.get('global_missing_variables', [])
        })

    except Exception as e:
        return jsonify({'success': False, 'error': str(e), 'traceback': traceback.format_exc()}), 500


# ============================================================
# AI 文档提取 API (PDF → 结构化数据)
# ============================================================
@app.route('/api/extract-pdf', methods=['POST'])
def extract_pdf():
    """
    上传 PDF 并通过 AI 提取结构化数据

    multipart/form-data:
        pdf_file: PDF 文件
        project_name: 项目名称
        data_fields: JSON 字符串，字段定义列表
    """
    try:
        if 'pdf_file' not in request.files:
            return jsonify({'success': False, 'error': '未找到 PDF 文件'}), 400

        pdf_file = request.files['pdf_file']
        project_name = request.form.get('project_name', '')
        data_fields_str = request.form.get('data_fields', '[]')

        try:
            data_fields = json.loads(data_fields_str)
        except json.JSONDecodeError:
            return jsonify({'success': False, 'error': 'data_fields 格式错误'}), 400

        result = extract_pdf_data(pdf_file, project_name, data_fields)
        return jsonify(result)

    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500


# ============================================================
# 启动
# ============================================================
if __name__ == '__main__':
    port = int(os.environ.get('SSB_API_PORT', 5002))
    print(f"=" * 60)
    print(f"  省时宝 (SSB) API 服务 v2.0.0")
    print(f"  Python: {sys.version.split()[0]}")
    print(f"  端口: {port}")
    print(f"  模板库: {TEMPLATE_DIR}")
    print(f"=" * 60)

    # 检查模板库
    if os.path.exists(TEMPLATE_DIR):
        templates = [d for d in os.listdir(TEMPLATE_DIR) if os.path.isdir(os.path.join(TEMPLATE_DIR, d))]
        print(f"  📁 可用模板: {len(templates)} 个")
        for t in templates:
            print(f"    - {t}")
    else:
        print(f"  ⚠️  模板库不存在: {TEMPLATE_DIR}")

    app.run(host='127.0.0.1', port=port, debug=False, threaded=True)
