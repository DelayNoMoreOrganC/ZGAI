FROM python:3.11-slim-bookworm

RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    libreoffice-core libreoffice-writer libreoffice-calc poppler-utils \
    tesseract-ocr tesseract-ocr-chi-sim fonts-noto-cjk curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY archive-worker/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt
COPY archive-worker/app ./app

RUN useradd --uid 10002 --create-home --shell /usr/sbin/nologin archive \
    && mkdir -p /data/archive-cache /data/case-files \
    && chown -R archive:archive /app /data
USER archive

EXPOSE 8091
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -fsS http://127.0.0.1:8091/health || exit 1
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8091"]
