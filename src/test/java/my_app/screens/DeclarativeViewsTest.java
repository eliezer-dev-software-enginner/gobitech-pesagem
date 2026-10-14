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

    @Test void settingsViewDeclaresPrintAndLogoStates() {
        var definition = UiDefinition.fromResource(getClass(), "/views/configuracoes.view.jsonc");
        try (var session = definition.open(new Actions()
                .register("retry", ignored -> {})
                .register("chooseLogo", ignored -> {})
                .register("removeLogo", ignored -> {})
                .register("save", ignored -> {}))) {
            assertEquals("settings", session.id());
            assertEquals("Impressora a laser", session.states().string("printType").get());
            assertEquals(false, session.states().state("showLogo", Boolean.class).get());
        }
    }

    @Test void cameraViewDeclaresBothCameraFormsAndPreviewStates() {
        var definition = UiDefinition.fromResource(getClass(), "/views/conexao-camera.view.jsonc");
        try (var session = definition.open(new Actions()
                .register("testFront", ignored -> {})
                .register("testBack", ignored -> {})
                .register("save", ignored -> {}))) {
            assertEquals("cameraSettings", session.id());
            assertEquals("80", session.states().string("frontPort").get());
            assertEquals("80", session.states().string("backPort").get());
        }
    }

    @Test void detailViewsDeclareBoundValues() {
        for (var kind : new String[]{"cliente", "produto", "usuario"}) {
            var definition = UiDefinition.fromResource(getClass(), "/views/" + kind + "-details.view.jsonc");
            try (var session = definition.open(new Actions())) {
                assertEquals(kind + "Details", session.id());
                assertEquals("", session.states().string("id").get());
            }
        }
    }

    @Test void productAndUserFormsDeclareEditableStates() {
        for (var kind : new String[]{"produto", "usuario"}) {
            var definition = UiDefinition.fromResource(getClass(), "/views/" + kind + "-form.view.jsonc");
            try (var session = definition.open(new Actions().register("save", ignored -> {}))) {
                assertEquals(kind + "Form", session.id());
                assertEquals("", session.states().string("nome").get());
                assertEquals("", session.states().string("saveText").get());
            }
        }
    }

    @Test void customerFormDeclaresAddressStates() {
        var definition = UiDefinition.fromResource(getClass(), "/views/cliente-form.view.jsonc");
        try (var session = definition.open(new Actions().register("save", ignored -> {}))) {
            assertEquals("clienteForm", session.id());
            assertEquals("", session.states().string("razaoSocial").get());
            assertEquals("", session.states().string("cep").get());
            assertEquals("[]", session.states().string("ufs").get());
        }
    }

    @Test void productAndUserListsDeclareNativeTables() {
        for (var kind : new String[]{"produto", "usuario"}) {
            var definition = UiDefinition.fromResource(getClass(), "/views/" + kind + "-list.view.jsonc");
            try (var session = definition.open(new Actions()
                    .register("select", ignored -> {})
                    .register("openDetails", ignored -> {})
                    .register("export", ignored -> {})
                    .register("edit", ignored -> {})
                    .register("delete", ignored -> {})
                    .register("create", ignored -> {}))) {
                assertEquals(kind + "List", session.id());
                assertEquals("[]", session.states().string("rows").get());
            }
        }
    }
}
