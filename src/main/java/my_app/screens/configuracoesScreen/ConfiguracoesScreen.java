package my_app.screens.configuracoesScreen;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.route.v2.ScreenContextInterface;
import megalodonte.base.state.State;
import megalodonte.base.state.Subscription;
import megalodonte.jsonc.Actions;
import megalodonte.jsonc.UiDefinition;
import megalodonte.jsonc.UiSession;
import my_app.domain.pesagem.TipoImpressao;

import java.util.ArrayList;
import java.util.List;

/** Presents print and branding settings from JSONC while services remain in the view model. */
public final class ConfiguracoesScreen implements ScreenComponent {
    private static final UiDefinition VIEW = UiDefinition.fromResource(ConfiguracoesScreen.class, "/views/configuracoes.view.jsonc");
    private final ConfiguracoesViewModel viewModel;
    private final UiSession session;
    private final List<Subscription> subscriptions = new ArrayList<>();

    public ConfiguracoesScreen(ScreenContextInterface context) {
        viewModel = new ConfiguracoesViewModel(context);
        session = VIEW.open(new Actions()
                .register("retry", ignored -> viewModel.load())
                .register("chooseLogo", ignored -> viewModel.handleUpdateLogoMarca())
                .register("removeLogo", ignored -> viewModel.limparLogo())
                .register("save", ignored -> viewModel.salvar()));
        session.display("status", viewModel.status, String.class)
                .display("logo", viewModel.logoHorizontal, String.class);
        var printLabel = State.of(viewModel.tipoImpressao.get().descricao());
        session.bind("printType", printLabel, String.class);
        subscriptions.add(viewModel.tipoImpressao.observe(value -> printLabel.set(value.descricao())));
        subscriptions.add(printLabel.observe(label -> {
            for (TipoImpressao type : TipoImpressao.values()) {
                if (type.descricao().equals(label)) {
                    viewModel.tipoImpressao.set(type);
                    return;
                }
            }
        }));
        subscriptions.add(viewModel.carregado.observe(loaded -> {
            session.states().state("showSettings", Boolean.class).set(loaded);
            session.states().state("showLoading", Boolean.class).set(!loaded);
        }));
        subscriptions.add(viewModel.logoVazia.observe(empty -> {
            session.states().state("showLogo", Boolean.class).set(!empty);
            session.states().state("showNoLogo", Boolean.class).set(empty);
        }));
    }

    @Override public Component render() { return session.render(); }
    @Override public void onMount() { viewModel.load(); }
    @Override public void onDestroy() {
        subscriptions.forEach(Subscription::close);
        session.close();
        viewModel.onDestroy();
    }
}
