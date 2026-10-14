package my_app.screens.clienteScreen;

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
import my_app.db.models.ClienteModel;
import my_app.db.services.ClienteService;
import my_app.domain.components.Components;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pack.utilities.DatePack;
import pack.utilities.FormatterPack;

import java.util.function.Function;

public class DetailsClienteScreen implements ScreenComponent {
    private static final UiDefinition VIEW = UiDefinition.fromResource(DetailsClienteScreen.class, "/views/cliente-details.view.jsonc");
    private final UiSession session;
    private static final Logger log = LoggerFactory.getLogger(DetailsClienteScreen.class);
    private final ClienteService clienteService;
    private final State<ClienteModel> model = State.of(null);

    private final ComputedState<String> id = campo(ClienteModel::getId);
    private final ComputedState<String> razaoSocial = campo(ClienteModel::getRazaoSocial);
    private final ComputedState<String> cpfCnpj = campo(c -> FormatterPack.formatCpfCnpj(c.getCpfCnpj()));
    private final ComputedState<String> telefone = campo(c -> FormatterPack.formatPhone(c.getTelefone()));
    private final ComputedState<String> complemento = campo(ClienteModel::getComplemento);
    private final ComputedState<String> dataCriacao = campo(c -> DatePack.localDateTimeToBrazilianDateTime(c.getDataCriacao()));
    private final ComputedState<Components.Endereco> endereco = ComputedState.of(() -> {
        var m = model.get();
        return m == null ? null : m.getEndereco();
    }, model);

    private final ComputedState<String> uf = enderecoCampo(e -> e.uf());
    private final ComputedState<String> cep = enderecoCampo(e -> FormatterPack.formatCep(e.cep()));
    private final ComputedState<String> cidade = enderecoCampo(e -> e.cidade());
    private final ComputedState<String> bairro = enderecoCampo(e -> e.bairro());
    private final ComputedState<String> rua = enderecoCampo(e -> e.rua());
    private final ComputedState<String> numero = enderecoCampo(e -> e.numero());
    public DetailsClienteScreen(ScreenContextInterface ctx) {
        long id;
        try {
            id = Long.parseLong(ctx.getParams().get("id"));
        } catch (RuntimeException e) {
            log.error("Parâmetro 'id' inválido na rota de detalhes", e);
            UI.runOnUi(() -> Components.ShowAlertError("ID inválido na rota de detalhes."));
            ctx.selfStage().close();
            throw new IllegalStateException(e);
        }
        this.clienteService = createOrReport(ClienteService::new);
        session = VIEW.open(new Actions());
        session.display("id", this.id, String.class)
                .display("razaoSocial", razaoSocial, String.class)
                .display("cpfCnpj", cpfCnpj, String.class)
                .display("telefone", telefone, String.class)
                .display("complemento", complemento, String.class)
                .display("dataCriacao", dataCriacao, String.class)
                .display("uf", uf, String.class)
                .display("cep", cep, String.class)
                .display("cidade", cidade, String.class)
                .display("bairro", bairro, String.class)
                .display("rua", rua, String.class)
                .display("numero", numero, String.class);

        Async.Run(() -> {
            try {
                var m = clienteService.buscarById(id);
                UI.runOnUi(() -> model.set(m));
            } catch (Exception e) {
                log.error("Erro ao buscar cliente id={}", id, e);
                UI.runOnUi(() -> Components.ShowAlertError("Não foi possível carregar os dados do cliente."));
            }
        });
    }

    @Override
    public Component render() { return session.render(); }

    private ComputedState<String> campo(Function<ClienteModel, Object> extract) {
        return ComputedState.of(() -> {
            var m = model.get();
            if (m == null) return "";
            var value = extract.apply(m);
            return value == null ? "" : value.toString();
        }, model);
    }

    private ComputedState<String> enderecoCampo(Function<Components.Endereco, String> extract) {
        return ComputedState.of(() -> {
            var value = endereco.get();
            if (value == null) return "";
            var field = extract.apply(value);
            return field == null ? "" : field;
        }, endereco);
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
            clienteService.close();
        } catch (Exception e) {
            ErrorReporter.handle(e);
        }
    }
}
