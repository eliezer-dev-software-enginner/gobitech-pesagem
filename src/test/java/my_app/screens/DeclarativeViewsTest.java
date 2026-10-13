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

    @Test void dashboardViewBindsLiveWeightAndBranding() {
        var definition = UiDefinition.fromResource(getClass(), "/views/dashboard.view.jsonc");
        try (var session = definition.open(new Actions())) {
            assertEquals("dashboard", session.id());
            assertEquals("—", session.states().string("weight").get());
            assertEquals(false, session.states().state("showLogo", Boolean.class).get());
        }
    }

    @Test void scaleConnectionViewHasBoundSerialAndTcpFields() {
        var definition = UiDefinition.fromResource(getClass(), "/views/conexao-balanca.view.jsonc");
        try (var session = definition.open(new Actions().register("save", ignored -> {}))) {
            assertEquals("scaleConnection", session.id());
            assertEquals("Serial", session.states().string("connectionType").get());
            assertEquals("9600", session.states().string("baudRate").get());
        }
    }
}
