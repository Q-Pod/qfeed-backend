import logging
import time

from fastapi import FastAPI, Request

from routers import feedback, follow_up, stt, tts

app = FastAPI(title="Q-Feed AI Mock Server")

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)s | %(name)s | %(message)s",
)
logger = logging.getLogger("ai-mock.access")

app.include_router(feedback.router, prefix="/ai", tags=["feedback"])
app.include_router(follow_up.router, prefix="/ai", tags=["follow-up"])
app.include_router(stt.router, prefix="/ai", tags=["stt"])
app.include_router(tts.router, prefix="/ai", tags=["tts"])


@app.middleware("http")
async def log_request_response(request: Request, call_next):
    start = time.perf_counter()
    client_host = request.client.host if request.client else "-"
    logger.info(
        "[요청] method=%s path=%s client=%s",
        request.method,
        request.url.path,
        client_host,
    )

    try:
        response = await call_next(request)
    except Exception:
        elapsed_ms = (time.perf_counter() - start) * 1000
        logger.exception(
            "[응답] method=%s path=%s status=500 elapsed_ms=%.2f",
            request.method,
            request.url.path,
            elapsed_ms,
        )
        raise

    elapsed_ms = (time.perf_counter() - start) * 1000
    logger.info(
        "[응답] method=%s path=%s status=%s elapsed_ms=%.2f",
        request.method,
        request.url.path,
        response.status_code,
        elapsed_ms,
    )
    return response


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}
