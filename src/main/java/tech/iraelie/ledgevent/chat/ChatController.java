package tech.iraelie.ledgevent.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
//import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatController {
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageStreamService messageStreamService;

    @MessageMapping("/chat.send")
//    @SendTo("/topic/messages")
    public ChatEvent handleMessage(ChatMessage incoming, Principal principal) {
        ChatMessage message = new ChatMessage(principal.getName(), incoming.content());

        messagingTemplate.convertAndSend(
                "/topic/messages",
                message
        );

        return messageStreamService.publish(message);
    }

    @MessageMapping("/chat.replay")
    public void handleReplay(ReplayRequest request, Principal principal) {
        List<ChatEvent> events = messageStreamService.replaySince(request.lastId());
        messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/replay", events);
    }
}
