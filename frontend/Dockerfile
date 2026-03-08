FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
# Modify API URLs to use relative /api/ for Nginx proxying at build time
RUN sed -i 's/http:\/\/localhost:8080\/api/\/api/g' src/app/services/mod/mod.service.ts && \
    sed -i 's/http:\/\/localhost:8080\/api\/auth/\/api\/auth/g' src/app/services/auth/auth.service.ts && \
    sed -i 's/http:\/\/localhost:8080\/api/\/api/g' src/app/components/admin-dashboard/admin-dashboard.component.ts && \
    sed -i 's/http:\/\/localhost:8080\/api\/auth\/discord\/login/\/api\/auth\/discord\/login/g' src/app/components/login/login.component.html && \
    sed -i 's/http:\/\/localhost:8080\/api\/auth\/discord\/login/\/api\/auth\/discord\/login/g' src/app/components/register/register.component.html

RUN npm run build --configuration production

FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Copy Angular 17+ browser dist path
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
