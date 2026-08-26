FROM nginx:1.27-alpine

COPY deploy/nginx/conf.d/default.conf /etc/nginx/conf.d/default.conf
COPY deploy/artifacts/landing /usr/share/nginx/html/landing
COPY deploy/artifacts/user-web /usr/share/nginx/html/web
COPY deploy/artifacts/merchant-web /usr/share/nginx/html/merchant
COPY deploy/artifacts/admin-web /usr/share/nginx/html/admin

RUN mkdir -p /usr/share/nginx/html/downloads
