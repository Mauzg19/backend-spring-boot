#!/bin/sh
exec java \
  -Dspring.ai.ollama.base-url=${SPRING_AI_OLLAMA_BASE_URL:-http://ollama:11434} \
  -Dspring.ai.ollama.chat.model=${SPRING_AI_OLLAMA_CHAT_MODEL:-phi} \
  -Dspring.datasource.url=${SPRING_DATASOURCE_URL:-jdbc:mysql://mysql:3306/restaurante_ing} \
  -Dspring.datasource.username=${SPRING_DATASOURCE_USERNAME:-root} \
  -Dspring.datasource.password=${SPRING_DATASOURCE_PASSWORD:-Mauricio19} \
  -jar app.jar
