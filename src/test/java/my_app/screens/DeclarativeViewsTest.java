package my_app.screens;

import megalodonte.jsonc.Actions;
import megalodonte.jsonc.UiDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeclarativeViewsTest {
    @Test void loginViewHasStateAndAction() {
        var definition = UiDefinition.fromResource(getClass(), "/views/auth.view.jsonc");
        try (var session = definition.open(new Actions().register("signIn", ignored -> {}))) {
            assertEquals("auth", session.id());
            assertEquals("", session.states().string("login").get());
            assertEquals("", session.states().string("password").get());
        }
    }

    @Test void logsViewHasStateAndActions() {
        var definition = UiDefinition.fromResource(getClass(), "/views/logs.view.jsonc");
        try (var session = definition.open(new Actions()
                .register("refresh", ignored -> {})
                .register("openFolder", ignored -> {}))) {
            assertEquals("logs", session.id());
            assertEquals("Carregando...", session.states().string("contents").get());
        }
    }
}
