#命令行中运行python import_documents_to_milvus.py ./ 来导入RAG知识库
# import_documents_to_milvus.py
from pymilvus import connections, Collection, FieldSchema, CollectionSchema, DataType
import os
from pathlib import Path
import requests
import json
import csv

# Milvus 配置
MILVUS_HOST = "8.210.210.255"
MILVUS_PORT = 19530
COLLECTION_NAME = "beverage_vectors"
VECTOR_DIMENSION = 1536

# 阿里云 Embedding API 配置
ALIYUN_ACCESS_KEY_ID = "2106693"
ALIYUN_ACCESS_KEY_SECRET = "sk-3c69e12e152e434ebe15afc11d313e31"
ALIYUN_REGION = "cn-hangzhou"
EMBEDDING_MODEL = "text-embedding-v2"


def get_embedding(text):
    """调用阿里云 Embedding API 获取向量"""
    import dashscope
    from dashscope import TextEmbedding

    dashscope.api_key = ALIYUN_ACCESS_KEY_SECRET

    response = TextEmbedding.call(
        model=EMBEDDING_MODEL,
        input=text
    )

    if response.status_code == 200:
        return response.output['embeddings'][0]['embedding']
    else:
        raise Exception(f"Embedding API 调用失败: {response.message}")


def chunk_text(text, chunk_size=500, overlap=50):
    """文档分块"""
    chunks = []
    start = 0
    while start < len(text):
        end = start + chunk_size
        chunk = text[start:end]
        chunks.append(chunk)
        start = end - overlap
    return chunks


def parse_csv(file_path):
    """解析CSV文件，将每行转换为文本描述
    
    支持的编码: UTF-8, GBK, GB2312
    支持的分隔符: 逗号(,), 分号(;), 制表符(\t)
    """
    text_lines = []
    encodings = ['utf-8', 'gbk', 'gb2312', 'utf-8-sig']
    
    for encoding in encodings:
        try:
            with open(file_path, 'r', encoding=encoding) as f:
                # 尝试检测分隔符
                sample = f.read(1024)
                f.seek(0)
                
                # 检测分隔符（逗号、分号、制表符）
                delimiter = ','
                if ';' in sample and sample.count(';') > sample.count(','):
                    delimiter = ';'
                elif '\t' in sample:
                    delimiter = '\t'
                
                # 尝试使用DictReader（有表头的情况）
                try:
                    reader = csv.DictReader(f, delimiter=delimiter)
                    fieldnames = reader.fieldnames
                    
                    if fieldnames:
                        # 使用字典读取，将每行转换为描述性文本
                        for row in reader:
                            if any(v and str(v).strip() for v in row.values()):  # 检查是否有非空值
                                # 将每行转换为键值对格式的文本
                                row_text = '。'.join(
                                    f"{key}: {value}" 
                                    for key, value in row.items() 
                                    if value and str(value).strip()
                                )
                                if row_text:
                                    text_lines.append(row_text)
                        break  # 成功读取，跳出编码循环
                except Exception:
                    # DictReader失败，使用普通reader
                    f.seek(0)
                    reader = csv.reader(f, delimiter=delimiter)
                    for row in reader:
                        if row and any(cell and str(cell).strip() for cell in row):
                            text_lines.append(' '.join(str(cell).strip() for cell in row if cell and str(cell).strip()))
                    break  # 成功读取，跳出编码循环
                    
        except UnicodeDecodeError:
            continue  # 尝试下一个编码
        except Exception as e:
            print(f"CSV解析警告 (编码 {encoding}): {e}")
            if encoding == encodings[-1]:  # 最后一个编码也失败
                return ""
            continue
    
    if not text_lines:
        print(f"警告: CSV文件 {file_path.name} 解析后没有有效内容")
        return ""
    
    return '\n'.join(text_lines)


def parse_document(file_path):
    """解析文档（支持 TXT、MD、CSV）"""
    suffix = file_path.suffix.lower()
    
    if suffix == '.csv':
        return parse_csv(file_path)
    else:
        # TXT、MD等文本文件
        with open(file_path, 'r', encoding='utf-8') as f:
            return f.read()


def import_documents_from_directory(directory_path):
    """从目录导入文档到 Milvus"""
    # 连接 Milvus
    connections.connect(
        alias="default",
        host=MILVUS_HOST,
        port=MILVUS_PORT
    )

    # 获取集合
    collection = Collection(COLLECTION_NAME)
    collection.load()

    # 读取文档
    doc_dir = Path(directory_path)
    files = (list(doc_dir.glob("*.txt")) + 
             list(doc_dir.glob("*.md")) + 
             list(doc_dir.glob("*.pdf")) + 
             list(doc_dir.glob("*.csv")))

    total_chunks = 0

    for file_path in files:
        file_type = file_path.suffix.lower()
        print(f"处理文件: {file_path.name} (类型: {file_type})")

        # 解析文档
        if file_type == '.pdf':
            # PDF 解析需要额外库，这里简化处理
            print(f"跳过 PDF 文件（需要额外处理）: {file_path.name}")
            continue

        try:
            if file_type == '.csv':
                print(f"  解析CSV文件...")
            content = parse_document(file_path)
            if not content or not content.strip():
                print(f"  警告: 文件内容为空，跳过: {file_path.name}")
                continue
            if file_type == '.csv':
                lines_count = len(content.split('\n'))
                print(f"  CSV解析完成，共 {lines_count} 行数据")
        except Exception as e:
            print(f"  错误: 解析文件失败: {file_path.name}, 错误: {e}")
            continue

        # 文档分块
        chunks = chunk_text(content)
        print(f"文档分块: {len(chunks)} 块")

        # 批量向量化
        vectors = []
        beverage_ids = []

        for chunk in chunks:
            try:
                vector = get_embedding(chunk)
                vectors.append(vector)
                beverage_ids.append(0)  # 外部文档使用 0
            except Exception as e:
                print(f"向量化失败: {e}")
                continue

        # 插入 Milvus
        if vectors:
            data = [
                beverage_ids,
                vectors
            ]

            collection.insert(data)
            collection.flush()
            total_chunks += len(vectors)
            print(f"成功插入 {len(vectors)} 个向量")

    print(f"导入完成，共处理 {total_chunks} 个文档块")
    connections.disconnect("default")


if __name__ == "__main__":
    import sys

    if len(sys.argv) < 2:
        print("使用方法: python import_documents_to_milvus.py <文档目录路径>")
        sys.exit(1)

    directory_path = sys.argv[1]
    import_documents_from_directory(directory_path)