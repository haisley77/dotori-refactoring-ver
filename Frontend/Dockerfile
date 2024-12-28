FROM node:16-alpine3.11 as build-stage

WORKDIR /front

# package.json 및 package-lock.json 복사
COPY package*.json ./

# 의존성 설치
RUN npm install

# 애플리케이션 소스 코드 복사
COPY . .

RUN npm run build

# 기본 포트로 80을 노출
EXPOSE 80

# Nginx 실행
CMD ["npm", "run", "dev"]
