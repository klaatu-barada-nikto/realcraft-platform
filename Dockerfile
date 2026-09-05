FROM alpine:3.19
WORKDIR /app
RUN apk add --no-cache ca-certificates tzdata
COPY server /app/server
COPY dist /app/dist
RUN mkdir -p /app/data/images /app/data/models
EXPOSE 8080
VOLUME ["/app/data"]
ENTRYPOINT ["/app/server"]