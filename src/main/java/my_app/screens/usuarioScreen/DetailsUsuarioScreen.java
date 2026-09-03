package my_app.screens.usuarioScreen;

import megalodonte.ComputedState;
import megalodonte.application.ErrorReporter;
import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.state.State;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.Card;
import megalodonte.components.SpacerVertical;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.props.ColumnProps;
import megalodonte.props.TextProps;
import megalodonte.router.v4.ScreenContext;
import megalodonte.utils.ThrowingSupplier;
import my_app.db.models.UsuarioModel;
import my_app.db.services.UsuarioService;
import my_app.domain.components.Components;
import pack.utilities.DatePack;

import java.util.function.Function;

public class DetailsUsuarioScreen implements ScreenComponent {
    private final UsuarioService usuarioService;
    private final State<UsuarioModel> model = State.of(null);

    private final ComputedState<String> id = campo(UsuarioModel::getId);
    private final ComputedState<String> nome = campo(UsuarioModel::getNome);
    private final ComputedState<String> login = campo(UsuarioModel::getLogin);
    private final ComputedState<String> telefone = campo(UsuarioModel::getTelefone);
    private final ComputedState<String> admin = campo(u -> Boolean.TRUE.equals(u.getAdmin()) ? "Sim" : "Não");
    private final ComputedState<String> dataCriacao = campo(u -> DatePack.localDateTimeToBrazilianDateTime(u.getDataCriacao()));

    public DetailsUsuarioScreen(ScreenContext ctx) {
        long id = Long.parseLong(ctx.getParams().get("id"));
        this.usuarioService = createOrReport(UsuarioService::new);

        Async.Run(() -> {
            try {
                var m = usuarioService.buscarById(id);
                UI.runOnUi(() -> model.set(m));
            } catch (Exception e) {
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao buscar usuário: " + e.getMessage()));
            }
        });
    }

    @Override
    public Component render() {
        return Components.ScrollPaneDefault(
                new Card(
                        new Column(new ColumnProps().paddingAll(20))
                                .c_child(new Text("Detalhes do usuário", new TextProps().fontSize(ThemeManager.theme().typography().subtitle())))
                                .c_child(new SpacerVertical(20))
                                .c_child(Components.TextWithDetailsState("ID: ", id))
                                .c_child(Components.TextWithDetailsState("Nome: ", nome))
                                .c_child(Components.TextWithDetailsState("Login: ", login))
                                .c_child(Components.TextWithDetailsState("Telefone: ", telefone))
                                .c_child(Components.TextWithDetailsState("Administrador: ", admin))
                                .c_child(Components.TextWithDetailsState("Data de criação: ", dataCriacao))
                )
        );
    }

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
        try {
            usuarioService.close();
        } catch (Exception e) {
            ErrorReporter.handle(e);
        }
    }
}
