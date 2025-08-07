#!/bin/bash

# API 엔드포인트 URL
API_URL="http://localhost:8080/admin/users"

# API 인증 토큰 (정적 토큰 또는 관리자 토큰)
# 운영 환경에서는 환경 변수로 관리하는 것을 권장합니다.
AUTH_TOKEN="Bearer ${ADMIN_AUTH_TOKEN}"

# API 호출
curl -X DELETE "${API_URL}" \
     -H "Content-Type: application/json" \
     -H "Authorization: ${AUTH_TOKEN}"
