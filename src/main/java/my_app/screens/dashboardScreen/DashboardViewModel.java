package my_app.screens.dashboardScreen;

import megalodonte.ComputedState;
import megalodonte.application.ErrorReporter;
import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.base.state.State;
import megalodonte.router.v4.ScreenContext;
import megalodonte.utils.ThrowingSupplier;
import my_app.db.services.ClienteService;
import my_app.db.services.PesagemService;
import my_app.db.services.PreferenciasService;
import my_app.db.services.ProdutoService;
import my_app.domain.components.Components;
import my_app.infra.balanca.BalancaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

public class DashboardViewModel {
    private static final Logger log = LoggerFactory.getLogger(DashboardViewModel.class);

    private final ProdutoService produtoService;
    private final ClienteService clienteService;
    private final PesagemService pesagemService;
    private final PreferenciasService preferenciasService;

    /** Peso lido da balança em tempo real — delega pro singleton {@link BalancaService}. */
    public final State<String> pesoAoVivo = BalancaService.getInstance().pesoAoVivo();
    public final State<Boolean> lendoBalanca = BalancaService.getInstance().lendoBalanca();

    public final State<String> totalProdutos = new State<>("—");
    public final State<String> totalClientes = new State<>("—");
    public final State<String> totalPesagens = new State<>("—");
    public final State<String> totalPesagensMes = new State<>("—");


    final State<String> logoHorizontal = State.of("");
    final ComputedState<Boolean> logoVazia =
            ComputedState.of(() -> logoHorizontal.get() == null || logoHorizontal.get().isBlank(), logoHorizontal);

    public DashboardViewModel(ScreenContext ctx) {
        this.produtoService = createOrReport(ProdutoService::new);
        this.clienteService = createOrReport(ClienteService::new);
        this.pesagemService = createOrReport(PesagemService::new);
        this.preferenciasService = createOrReport(PreferenciasService::new);
    }

    public void carregar() {
        Async.Run(() -> {
            try {
                long produtos = produtoService.count();
                long clientes = clienteService.count();
                long pesagens = pesagemService.count();

                LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
                LocalDate hoje = LocalDate.now();
                long pesagensMes = pesagemService.contarPorPeriodo(inicioMes, hoje);

                var imagemPath = preferenciasService.getImagemHorizontalLogo();

                UI.runOnUi(() -> {
                    totalProdutos.set(String.valueOf(produtos));
                    totalClientes.set(String.valueOf(clientes));
                    totalPesagens.set(String.valueOf(pesagens));
                    totalPesagensMes.set(String.valueOf(pesagensMes));
                    if (imagemPath != null && !imagemPath.isBlank()) logoHorizontal.set(imagemPath);
                });
            } catch (Exception e) {
                log.error("Erro ao carregar o dashboard", e);
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao carregar o painel de resumo."));
            }
        });
    }

    private <T> T createOrReport(ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            ErrorReporter.handle(e);
            throw new IllegalStateException(e);
        }
    }

    // ---- balança (singleton compartilhado) ----

    /** Liga a leitura contínua da balança via {@link BalancaService}. */
    public void iniciarLeituraBalanca() {
        BalancaService.getInstance().iniciar();
    }

    public void onDestroy() {
        try {
            produtoService.close();
            clienteService.close();
            pesagemService.close();
        } catch (Exception e) {
            log.warn("Erro ao fechar serviços do dashboard", e);
        }
    }
}
