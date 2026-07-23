from __future__ import annotations

import hmac
import os
from fastapi import Depends, FastAPI, Header, HTTPException

from .engine import analyze_archive, assemble_archive, engine_health

app = FastAPI(title="ZGAI Internal Archive Engine", docs_url=None, redoc_url=None)


def require_internal_token(authorization: str = Header(default="")) -> None:
    expected = os.getenv("ARCHIVE_WORKER_TOKEN", "").strip()
    supplied = authorization.removeprefix("Bearer ").strip()
    if not expected or not hmac.compare_digest(expected, supplied):
        raise HTTPException(status_code=401, detail="invalid internal token")


@app.get("/health")
def health():
    return engine_health()


@app.post("/internal/archive/analyze", dependencies=[Depends(require_internal_token)])
def analyze(payload: dict):
    return analyze_archive(payload)


@app.post("/internal/archive/assemble", dependencies=[Depends(require_internal_token)])
def assemble(payload: dict):
    return assemble_archive(payload)
