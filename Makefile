.DEFAULT_GOAL := help

COMPOSE := docker compose

.PHONY: help build deploy up down logs exec clean

help: ## コマンド一覧を表示
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk -F':.*?## ' '{printf "  \033[36m%-10s\033[0m %s\n", $$1, $$2}'

build: ## Docker イメージをビルド
	$(COMPOSE) build

deploy: ## ビルド＋アプリ再デプロイ
	$(COMPOSE) up -d --build

up: ## コンテナ起動
	$(COMPOSE) up -d

down: ## コンテナ停止
	$(COMPOSE) down

logs: ## アプリログ表示
	$(COMPOSE) logs -f app

exec: ## app コンテナに入る
	$(COMPOSE) exec app sh

clean: ## 未使用イメージ削除
	docker image prune -f
