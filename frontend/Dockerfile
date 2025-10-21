FROM node:24-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm install
COPY . .
RUN chown -R app:app .
USER app
EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host"]
