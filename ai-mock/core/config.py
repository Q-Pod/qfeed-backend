import os
from functools import lru_cache


def _get_delay_seconds(env_name: str, default: float) -> float:
    raw = os.getenv(env_name)
    if raw is None:
        return default

    try:
        return max(float(raw), 0.0)
    except ValueError:
        return default


class Settings:
    def __init__(self) -> None:
        self.mock_feedback_delay_sec = _get_delay_seconds("MOCK_FEEDBACK_DELAY_SEC", 40.0)
        self.mock_follow_up_delay_sec = _get_delay_seconds("MOCK_FOLLOW_UP_DELAY_SEC", 15.0)
        self.mock_stt_delay_sec = _get_delay_seconds("MOCK_STT_DELAY_SEC", 1.0)
        self.mock_tts_delay_sec = _get_delay_seconds("MOCK_TTS_DELAY_SEC", 1.5)


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
