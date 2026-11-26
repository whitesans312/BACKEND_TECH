ENV_FILE ?= .env.parkflow
COMPOSE  := docker compose --env-file $(ENV_FILE) -f docker-compose.yml
PG_USER  ?= parkflow_admin

.PHONY: help up down build restart logs ps health db-check clean

help: ## Muestra los comandos disponibles para gestionar ParkFlow
	@echo "Comandos disponibles de ParkFlow:"
	@echo "  make up       - Construye (si hace falta) y levanta Traefik, Postgres y los 3 microservicios"
	@echo "  make down     - Detiene y elimina los contenedores conservando los volumenes de datos"
	@echo "  make build    - Reconstruye las imagenes Docker de los microservicios"
	@echo "  make restart  - Reinicia todos los servicios del stack"
	@echo "  make logs     - Muestra los logs en tiempo real (usa SERVICE=<nombre> para filtrar)"
	@echo "  make ps       - Lista el estado y healthchecks de los contenedores"
	@echo "  make health   - Verifica /actuator/health (via Traefik) y /.well-known/jwks.json"
	@echo "  make db-check - Verifica tablas y migraciones Flyway en db_identity, db_parking y db_billing"
	@echo "  make clean    - Detiene contenedores y elimina volumenes, redes e imagenes locales del stack"

up: ## Levanta toda la pila en segundo plano
	$(COMPOSE) up -d --build

down: ## Detiene y remueve los contenedores (preserva volumen de PostgreSQL)
	$(COMPOSE) down

build: ## Construye las imagenes multi-stage de los microservicios
	$(COMPOSE) build

restart: ## Reinicia la pila completa
	$(COMPOSE) down
	$(COMPOSE) up -d

logs: ## Sigue los logs de todos los servicios o de uno especifico (make logs SERVICE=parking-service)
	$(COMPOSE) logs -f --tail=100 $(SERVICE)

ps: ## Muestra el estado actual de los servicios
	$(COMPOSE) ps

health: ## Verifica el estado de salud de los 3 microservicios y JWKS a traves de Traefik
	@echo "=== Identity Health ==="
	@curl -fsS http://localhost/health/identity && echo ""
	@echo "=== Parking Health ==="
	@curl -fsS http://localhost/health/parking && echo ""
	@echo "=== Billing Health ==="
	@curl -fsS http://localhost/health/billing && echo ""
	@echo "=== Identity JWKS ==="
	@curl -fsS http://localhost/.well-known/jwks.json && echo ""
	@echo "=== Traefik Routers (8090) ==="
	@curl -fsS http://localhost:8090/api/http/routers >/dev/null && echo "Traefik dashboard API OK"
	@echo "=== Frontend (5173) ==="
	@curl -fsS http://localhost:5173 >/dev/null && echo "Frontend UI OK"

db-check: ## Valida las bases aisladas, tablas e historial de Flyway en PostgreSQL
	@echo "=== Bases de datos disponibles ==="
	@$(COMPOSE) exec -T postgres psql -U $(PG_USER) -d postgres -c "\l"
	@echo "=== db_identity: Flyway + tablas ==="
	@$(COMPOSE) exec -T postgres psql -U $(PG_USER) -d db_identity -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
	@$(COMPOSE) exec -T postgres psql -U $(PG_USER) -d db_identity -c "\dt"
	@echo "=== db_parking: Flyway (incluye V5) + tablas ==="
	@$(COMPOSE) exec -T postgres psql -U $(PG_USER) -d db_parking -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
	@$(COMPOSE) exec -T postgres psql -U $(PG_USER) -d db_parking -c "\dt"
	@echo "=== db_billing: Flyway + tablas ==="
	@$(COMPOSE) exec -T postgres psql -U $(PG_USER) -d db_billing -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
	@$(COMPOSE) exec -T postgres psql -U $(PG_USER) -d db_billing -c "\dt"

clean: ## Elimina contenedores, redes, volumenes e imagenes locales de ParkFlow
	$(COMPOSE) down -v --remove-orphans --rmi local
