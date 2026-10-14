package my_app.screens.usuarioScreen;

import megalodonte.ComputedState;
import megalodonte.jsonc.Actions;
import megalodonte.jsonc.UiDefinition;
import megalodonte.jsonc.UiSession;
import megalodonte.application.ErrorReporter;
import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.state.State;
import megalodonte.base.route.v2.ScreenContextInterface;
import megalodonte.utils.ThrowingSupplier;
import my_app.db.models.UsuarioModel;
import my_app.db.services.UsuarioService;
import my_app.domain.components.Components;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pack.utilities.DatePack;

import java.util.function.Function;

public class DetailsUsuarioScreen implements ScreenComponent {
    private static final UiDefinition VIEW = UiDefinition.fromResource(DetailsUsuarioScreen.class, "/views/usuario-details.view.jsonc");
    private final UiSession session;
    private static final Logger log = LoggerFactory.getLogger(DetailsUsuarioScreen.class);
    private final UsuarioService usuarioService;
    private final State<UsuarioModel> model = State.of(null);

    private final ComputedState<String> id = campo(UsuarioModel::getId);
    private final ComputedState<String> nome = campo(UsuarioModel::getNome);
    private final ComputedState<String> login = campo(UsuarioModel::getLogin);
    private final ComputedState<String> telefone = campo(UsuarioModel::getTelefone);
    private final ComputedState<String> perfil = campo(u -> Boolean.TRUE.equals(u.getAdmin()) ? "Admin" : "Operador");
    private final ComputedState<String> dataCriacao = campo(u -> DatePack.localDateTimeToBrazilianDateTime(u.getDataCriacao()));

    public DetailsUsuarioScreen(ScreenContextInterface ctx) {
        long id;
        try {
            id = Long.parseLong(ctx.getParams().get("id"));
        } catch (RuntimeException e) {
            log.error("Parâmetro 'id' inválido na rota de detalhes", e);
            UI.runOnUi(() -> Components.ShowAlertError("ID inválido na rota de detalhes."));
            ctx.selfStage().close();
            throw new IllegalStateException(e);
        }
        this.usuarioService = createOrReport(UsuarioService::new);
        session = VIEW.open(new Actions());
        session.display("id", this.id, String.class)
                .display("nome", nome, String.class)
                .display("login", login, String.class)
                .display("telefone", telefone, String.class)
                .display("perfil", perfil, String.class)
                .display("dataCriacao", dataCriacao, String.class);

        Async.Run(() -> {
            try {
                var m = usuarioService.buscarById(id);
                UI.runOnUi(() -> model.set(m));
            } catch (Exception e) {
                log.error("Erro ao buscar usuário id={}", id, e);
                UI.runOnUi(() -> Components.ShowAlertError("Não foi possível carregar os dados do usuário."));
            }
        });
    }

    @Override
    public Component render() { return session.render(); }

    private ComputedState<String> campo(Function<UsuarioModel, Object> extract) {
        return ComputedState.of(() -> {
            var m = model.get();
            if (m == null) return "";
            var value = extract.apply(m);
            return value == null ? "" : value.toString();
        }, model);
    }

    protected <T> T createOrReport(ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            ErrorReporter.handle(e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void onDestroy() {
        session.close();
        try {
            usuarioService.close();
        } catch (Exception e) {
            ErrorReporter.handle(e);
        }
    }
}
