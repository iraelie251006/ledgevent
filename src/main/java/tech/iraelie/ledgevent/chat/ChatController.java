package tech.iraelie.ledgevent.chat;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class ChatController {
    @MessageMapping("/chat.send")
    @SendTo("/topic/messages")
    public ChatMessage handleMessage(ChatMessage incoming, Principal principal) {
        return new ChatMessage(principal.getName(), incoming.content());
    }
}
