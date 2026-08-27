# /docker-compose.yml (운영 환경 : 컨테이너 실행용 - 실제 적용값)
# /recommend-service/app/config/setting.py (아무 설정도 없을 경우 이 셋팅으로 동작 - 기본값)
# /recommend-service/.env (개발 환경 : 로컬 직접 실행용)

from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    # 서버 설정
    app_port: int = 8085
    app_name: str = "recommend-service"

    # Eureka 설정
    eureka_server_url: str = "http://localhost:8761/eureka"
    eureka_instance_host: str = "localhost"

    # Auth Server
    jwt_issuer_uri: str = "http://localhost:8080"
    jwk_set_uri: str = "http://auth-server:9000/oauth2/jwks"

    # 서비스 URL
    enrollment_service_url: str = "http://localhost:8083"
    course_service_url: str = "http://localhost:8082"
    movie_service_url: str = "http://localhost:8082"
    default_movie_genre: str = "DRAMA"

    # Kafka
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_consumer_group_id: str = "recommend-service"
    kafka_topic_booking_completed: str = "booking.completed"

    # LLM (OpenAI-compatible) - 추천 알고리즘 context injection
    llm_api_key: str = ""
    llm_base_url: str = "https://api.deepseek.com/v1"
    llm_model: str = "deepseek-chat"
    llm_timeout: float = 25.0
    llm_max_snacks: int = 3
    llm_json_mode: bool = True
    llm_max_tokens: int = 2048

    class Config:
        env_file = ".env"


settings = Settings()
