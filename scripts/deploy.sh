#!/bin/bash

# 무중단 배포 스크립트
IMAGE_TAG=$1
CONTAINER_NAME="pullit-backend"
NEW_CONTAINER="${CONTAINER_NAME}-new"
OLD_CONTAINER="${CONTAINER_NAME}-old"

if [ -z "$IMAGE_TAG" ]; then
    echo "Usage: ./deploy.sh <image-tag>"
    exit 1
fi

echo "🚀 Starting zero-downtime deployment..."

# 1. 새 이미지 로드
echo "📦 Loading new Docker image..."
docker load < pullit-backend.tar.gz
docker tag pullit-backend:${IMAGE_TAG} pullit-backend:latest

# 2. 현재 실행 중인 컨테이너 확인
CURRENT_CONTAINER=$(docker ps --filter "name=^${CONTAINER_NAME}$" --format "{{.Names}}" | head -1)

# 3. 새 컨테이너 시작
echo "🐳 Starting new container..."
docker run -d \
    --name ${NEW_CONTAINER} \
    --network pullit_pullit-network \
    --restart unless-stopped \
    --env-file /home/ubuntu/.env \
    -e SPRING_PROFILES_ACTIVE=prod \
    pullit-backend:latest

# 4. 헬스체크
echo "🏥 Health checking new container..."
for i in {1..30}; do
    if docker exec ${NEW_CONTAINER} curl -f http://localhost:8080/api/test/health > /dev/null 2>&1; then
        echo "✅ New container is healthy!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ Health check failed. Rolling back..."
        docker stop ${NEW_CONTAINER}
        docker rm ${NEW_CONTAINER}
        exit 1
    fi
    echo "⏳ Waiting for container to be ready... ($i/30)"
    sleep 2
done

# 5. Nginx upstream 전환
echo "🔄 Updating Nginx configuration..."
cat > /home/ubuntu/app/pullit/nginx/backend-upstream.conf << EOF
upstream backend {
    server ${NEW_CONTAINER}:8080;
}
EOF

# Nginx 설정 테스트 및 리로드
docker exec pullit-nginx nginx -t
if [ $? -eq 0 ]; then
    docker exec pullit-nginx nginx -s reload
    echo "✅ Nginx reloaded successfully"
else
    echo "❌ Nginx configuration test failed. Rolling back..."
    rm /home/ubuntu/app/pullit/nginx/backend-upstream.conf
    docker stop ${NEW_CONTAINER}
    docker rm ${NEW_CONTAINER}
    exit 1
fi

# 6. 안정화 대기
echo "⏳ Waiting for traffic to switch..."
sleep 10

# 7. 기존 컨테이너 정리
if [ ! -z "$CURRENT_CONTAINER" ]; then
    echo "🗑️ Removing old container..."
    docker stop ${CURRENT_CONTAINER}
    docker rename ${CURRENT_CONTAINER} ${OLD_CONTAINER} 2>/dev/null || true
fi

# 8. 새 컨테이너 이름 변경
docker rename ${NEW_CONTAINER} ${CONTAINER_NAME}

# 9. upstream 설정 업데이트
cat > /home/ubuntu/app/pullit/nginx/backend-upstream.conf << EOF
upstream backend {
    server ${CONTAINER_NAME}:8080;
}
EOF
docker exec pullit-nginx nginx -s reload

# 10. 정리
echo "🧹 Cleaning up..."
docker rm ${OLD_CONTAINER} 2>/dev/null || true
rm pullit-backend.tar.gz
docker image prune -f

echo "✨ Deployment completed successfully!"