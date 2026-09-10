package tech.iraelie.ledgevent.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageStreamService {

    private static final String STREAM_KEY = "ledgevent:messages";
    private static final long MAX_STREAM_LENGTH = 1000;

    private final StringRedisTemplate redisTemplate;

    public ChatEvent publish(ChatMessage message) {
        Map<String, String> fields = Map.of(
                "sender", message.sender(),
                "content", message.content()
        );

        RecordId id = redisTemplate.opsForStream()
                .add(StreamRecords.newRecord().ofMap(fields).withStreamKey(STREAM_KEY));

        // Cap the log so it doesn't grow unbounded — this defines your replay window
        redisTemplate.opsForStream().trim(STREAM_KEY, MAX_STREAM_LENGTH);

        return new ChatEvent(id.getValue(), message.sender(), message.content());
    }

    public List<ChatEvent> replaySince(String lastSeenId) {
        Range<String> range = (lastSeenId == null || lastSeenId.isBlank() || lastSeenId.equals("0"))
                ? Range.unbounded()
                : Range.leftOpen(lastSeenId, "+"); // exclusive lower bound — don't resend lastSeenId itself

        List<MapRecord<String, Object, Object>> records =
                redisTemplate.opsForStream().range(STREAM_KEY, range);

        if (records == null) return List.of();

        return records.stream()
                .map(r -> new ChatEvent(
                        r.getId().getValue(),
                        String.valueOf(r.getValue().get("sender")),
                        String.valueOf(r.getValue().get("content"))
                ))
                .collect(Collectors.toList());
    }
}