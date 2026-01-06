# 本地内容审核（Python，可选）

目标：用一个“轻量、快”的本地 Python 服务，对**发布动态**、**发布动态评论**、**发布 Wiki 评论**时的文本/图片做基础违规检测（暴恐/色情/广告）。准确率不是目标，优先保证运行速度与开发可用性。

后端默认 **不开启**（避免其他同学拉代码后启动就报错）。开启后若 Python 服务不可用，后端按 `moderation.python.fail-open` 配置决定是否放行。

## 1) 安装与启动（本机 Python）

前置：Python 3.10+（建议 3.11），Windows/macOS/Linux 均可。

```bash
cd tools/moderation
python -m venv .venv
.\.venv\Scripts\pip install -r requirements.txt
.\.venv\Scripts\python server.py
```

默认监听：`http://127.0.0.1:8099`

## 1.1) 可选：下载并启用一个“超小模型”（YOLOv5n）做暴恐/刀具信号

YOLOv5n 很小、推理快，但它不是“暴恐专用模型”，这里只取 `knife/scissors` 作为粗略暴力信号（速度优先，误报/漏报都可能发生）。

```bash
.\.venv\Scripts\python download_models.py
set MODERATION_ENABLE_YOLO=true
.\.venv\Scripts\python server.py
```

## 2) 后端配置

在 `backend/src/main/resources/application.properties` 添加/修改：

```properties
moderation.python.enabled=true
moderation.python.base-url=http://127.0.0.1:8099
moderation.python.timeout-ms=800
moderation.python.fail-open=true
```

## 3) 环境变量（可选）

- `MODERATION_PORT`：服务端口（默认 `8099`）
- `MODERATION_MAX_IMAGE_BYTES`：最大图片字节（默认 `6000000`）
- `MODERATION_NUDITY_STRICT`：色情检测阈值（默认 `0.75`）
- `MODERATION_VIOLENCE_STRICT`：暴恐检测阈值（默认 `0.65`）
- `MODERATION_AD_STRICT`：广告检测阈值（默认 `0.60`）

## 4) 返回值说明

服务会返回 `action`：
- `ALLOW`：通过
- `REVIEW`：疑似违规（后端对图片会直接拦截；对文本会进入“需复核/过滤发布”的逻辑）
- `BLOCK`：明确违规（后端直接拒绝发布）
