# ledgevent

A distributed, delivery-guaranteed WebSocket messaging backbone built with Spring Boot sequenced message replay, horizontal scaling via Redis without sticky sessions, backpressure-aware slow-consumer eviction, and graceful connection draining on deploy.

This is **not** a chat app demo. Chat is just the thin client used to exercise the system. The point of this project is to answer the questions tutorials skip: what happens when a client disconnects mid-conversation and reconnects does it lose messages? What happens when you scale to multiple instances do you need sticky sessions? What happens when a client can't keep up with the message rate does the server buffer forever and OOM? What happens when you deploy a new version while 500 clients are connected do they get dropped?

## Why "ledgevent"

Every message is treated like an entry in a ledger: sequenced, ordered, and replayable. Combined with "event" for the pub/sub messaging model underneath.

## Architecture

```
Client (SockJS/STOMP) ──▶ Instance A ─┐
                                        ├─▶ Redis Pub/Sub (fanout across instances)
Client (SockJS/STOMP) ──▶ Instance B ─┘
                                        │
                          Redis Stream (replay buffer, capped)
```

- **No sticky sessions.** A client's WebSocket connection can live on any instance; messages published from any node reach subscribers on every other node via Redis Pub/Sub.
- **No database.** Everything stored (replay buffer, presence) is short-lived by design capped Redis Streams / TTL'd keys, not durable records. See the design notes below for when a database *would* be the right call.

## Why no relational database

This system's job is guaranteeing delivery over an unreliable transport for a short replay window not archiving message history or running queries. A capped Redis Stream is the right tool for "catch me up since I disconnected"; it does not need to be the right tool for "show me all messages from the last 30 days," which is a different problem this project doesn't solve (and says so on purpose rather than bolting on a database nobody queries).

## Tech stack

- Java 21 (LTS) — virtual threads for connection scaling
- Spring Boot 4.1 — WebSocket + STOMP, Actuator, Security, Validation
- Redis — Pub/Sub fanout + Streams-based replay buffer
- Micrometer + Prometheus + Grafana — observability
- Testcontainers — integration tests against real Redis
- JWT (jjwt) — handshake auth + mid-connection re-auth
- Docker / AWS ECS — deployment target for horizontal scaling proof

## Roadmap

- [ ] **Phase 1 — Baseline STOMP + JWT handshake auth**
  WebSocket config, STOMP endpoint, JWT validated at handshake via a `HandshakeInterceptor`.
- [ ] **Phase 2 — Sequence numbers + Redis replay buffer**
  Every message gets a monotonic sequence number. Reconnecting clients send their last-seen sequence and receive the gap.
- [ ] **Phase 3 — Horizontal scaling via Redis Pub/Sub**
  Two instances, no sticky sessions. A message published on instance B reaches a client connected to instance A.
- [ ] **Phase 4 — Backpressure and slow-consumer eviction**
  `ConcurrentWebSocketSessionDecorator` with buffer/time limits; slow clients are detected and evicted instead of buffered indefinitely.
- [ ] **Phase 5 — Graceful shutdown / connection draining**
  SIGTERM triggers a clean "reconnect elsewhere" notice to clients before the hard kill — zero silent message loss during rolling deploys.
- [ ] **Phase 6 — Mid-connection re-auth**
  Short-lived JWTs are refreshed over the open socket instead of forcing a full reconnect when they expire.
- [ ] **Phase 7 — Observability layer**
  Micrometer metrics (active connections, message latency percentiles, eviction count, reconnect rate) wired into Prometheus/Grafana.
- [ ] **Phase 8 — Load testing and AWS deployment**
  k6/Gatling WebSocket load test to find the actual breaking point; deployed on ECS Fargate + ElastiCache Redis to prove horizontal scaling for real, not just in docker-compose.

## Local development

```bash
# start Redis
docker compose up -d redis

# run the app
./mvnw spring-boot:run
```

## Testing

```bash
./mvnw test
```

Integration tests spin up a real Redis instance via Testcontainers rather than mocking the client — this project's whole point is proving distributed behavior, so the tests need to exercise real Redis semantics.

## License

MIT — see [LICENSE](LICENSE).