package my_app.screens.dashboardScreen;

import megalodonte.application.ErrorReporter;
import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.base.state.State;
import megalodonte.router.v4.ScreenContext;
import megalodonte.utils.ThrowingSupplier;
import my_app.db.services.ClienteService;
import my_app.db.services.PesagemService;
import my_app.db.services.ProdutoService;
import my_app.domain.components.Components;
import pack.utilities.DatePack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

public class DashboardViewModel {
    private static final Logger log = LoggerFactory.getLogger(DashboardViewModel.class);

    private final ProdutoService produtoService;
    private final ClienteService clienteService;
    private final PesagemService pesagemService;

    public final State<String> totalProdutos = new State<>("—");
    public final State<String> totalClientes = new State<>("—");
    public final State<String> totalPesagens = new State<>("—");
    public final State<String> totalPesagensMes = new State<>("—");

    public DashboardViewModel(ScreenContext ctx) {
        this.produtoService = createOrReport(ProdutoService::new);
        this.clienteService = createOrReport(ClienteService::new);
        this.pesagemService = createOrReport(PesagemService::new);
    }

    public void carregar() {
        Async.Run(() -> {
            try {
                int produtos = produtoService.listar().size();
                int clientes = clienteService.listar().size();
                int pesagens = pesagemService.listar().size();

                long inicioMes = DatePack.localDateParaMillis(LocalDate.now().withDayOfMonth(1));
                long agora = System.currentTimeMillis();
                // filtrar() já anexa Cliente/Produto/Desconto a cada pesagem (feito pra tela de
                // listagem) — redundante aqui, só queremos o tamanho, mas o volume mensal de
                // pesagens é baixo o bastante pra não valer a pena criar um COUNT(*) dedicado.
                int pesagensMes = pesagemService.filtrar(null, null, null, null, inicioMes, agora).size();

                UI.runOnUi(() -> {
                    totalProdutos.set(String.valueOf(produtos));
                    totalClientes.set(String.valueOf(clientes));
                    totalPesagens.set(String.valueOf(pesagens));
                    totalPesagensMes.set(String.valueOf(pesagensMes));
                });
            } catch (Exception e) {
                log.error("Erro ao carregar o dashboard", e);
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao carregar o dashboard: " + e.getMessage()));
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
