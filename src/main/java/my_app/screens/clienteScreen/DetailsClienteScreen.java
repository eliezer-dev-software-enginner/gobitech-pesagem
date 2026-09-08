package my_app.screens.clienteScreen;

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
import my_app.db.models.ClienteModel;
import my_app.db.services.ClienteService;
import my_app.domain.components.Components;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pack.utilities.DatePack;
import pack.utilities.FormatterPack;

import java.util.function.Function;

public class DetailsClienteScreen implements ScreenComponent {
    private static final Logger log = LoggerFactory.getLogger(DetailsClienteScreen.class);
    private final ClienteService clienteService;
    private final State<ClienteModel> model = State.of(null);

    private final ComputedState<String> id = campo(ClienteModel::getId);
    private final ComputedState<String> loja = campo(ClienteModel::getLoja);
    private final ComputedState<String> razaoSocial = campo(ClienteModel::getRazaoSocial);
    private final ComputedState<String> cpfCnpj = campo(c -> FormatterPack.formatCpfCnpj(c.getCpfCnpj()));
    private final ComputedState<String> telefone = campo(c -> FormatterPack.formatPhone(c.getTelefone()));
    private final ComputedState<String> complemento = campo(ClienteModel::getComplemento);
    private final ComputedState<String> dataCriacao = campo(c -> DatePack.localDateTimeToBrazilianDateTime(c.getDataCriacao()));
    private final ComputedState<Components.Endereco> endereco = ComputedState.of(() -> {
        var m = model.get();
        return m == null ? null : m.getEndereco();
    }, model);

    public DetailsClienteScreen(ScreenContext ctx) {
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
    public Component render() {
        return Components.ScrollPaneDefault(
                new Card(
                        new Column(new ColumnProps().paddingAll(20))
                                .c_child(new Text("Detalhes do cliente", new TextProps().fontSize(ThemeManager.theme().typography().subtitle())))
                                .c_child(new SpacerVertical(20))
                                .c_child(Components.TextWithDetailsState("ID: ", id))
                                .c_child(Components.TextWithDetailsState("Loja: ", loja))
                                .c_child(Components.TextWithDetailsState("Razão social: ", razaoSocial))
                                .c_child(Components.TextWithDetailsState("CPF/CNPJ: ", cpfCnpj))
                                .c_child(Components.TextWithDetailsState("Telefone: ", telefone))
                                .c_child(Components.ItemDetailEnderecoState(endereco))
                                .c_child(Components.TextWithDetailsState("Complemento: ", complemento))
                                .c_child(Components.TextWithDetailsState("Data de criação: ", dataCriacao))
                )
        );
    }

    private ComputedState<String> campo(Function<ClienteModel, Object> extract) {
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
            clienteService.close();
        } catch (Exception e) {
            ErrorReporter.handle(e);
        }
    }
}
