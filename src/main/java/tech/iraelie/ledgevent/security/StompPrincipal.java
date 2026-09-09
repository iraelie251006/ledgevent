package tech.iraelie.ledgevent.security;

import java.security.Principal;

public record StompPrincipal(String name) implements Principal {

    @Override
    public String getName() {
        return name;
    }
}
