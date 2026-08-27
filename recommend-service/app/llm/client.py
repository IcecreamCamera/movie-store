# -*- coding: utf-8 -*-
import json
import re
import logging

import httpx

from app.config.settings import settings

logger = logging.getLogger(__name__)


def _repair_json(s: str) -> str:
    """LLM 출력의 흔한 JSON 오류를 보정: 배열/객체 끝의 trailing comma 제거."""
    return re.sub(r",\s*([}\]])", r"\1", s)


class LLMClient:
    """
    OpenAI-compatible chat completions 클라이언트.
    - base_url / api_key / model 을 settings에서 로드 (DeepSeek, OpenAI, vLLM 등 호환)
    - json_mode면 response_format=json_object 요청 (DeepSeek/OpenAI 지원)
    - 응답은 견고한 JSON 파서로 파싱 (markdown fence, trailing comma 대응)
    """

    async def chat_json(self, messages: list[dict]) -> dict:
        url = settings.llm_base_url.rstrip("/") + "/chat/completions"
        headers = {
            "Authorization": f"Bearer {settings.llm_api_key}",
            "Content-Type": "application/json",
        }
        payload = {
            "model": settings.llm_model,
            "messages": messages,
            "temperature": 0.4,
            "max_tokens": settings.llm_max_tokens,
        }
        if settings.llm_json_mode:
            payload["response_format"] = {"type": "json_object"}

        async with httpx.AsyncClient(timeout=settings.llm_timeout) as client:
            resp = await client.post(url, json=payload, headers=headers)
            resp.raise_for_status()
            content = resp.json()["choices"][0]["message"]["content"]

        return self._parse_json(content)

    @staticmethod
    def _parse_json(content: str) -> dict:
        text = (content or "").strip()

        # ```json ... ``` 펜스 제거
        fence = re.search(r"```(?:json)?\s*(.*?)\s*```", text, re.DOTALL)
        if fence:
            text = fence.group(1).strip()

        candidates = [text]
        start = text.find("{")
        end = text.rfind("}")
        if start != -1 and end != -1 and end >= start:
            candidates.append(text[start:end + 1])

        for cand in candidates:
            for repaired in (cand, _repair_json(cand)):
                try:
                    return json.loads(repaired)
                except json.JSONDecodeError:
                    continue

        raise ValueError(f"LLM 응답을 JSON으로 파싱할 수 없습니다: {text[:200]}")


llm_client = LLMClient()
