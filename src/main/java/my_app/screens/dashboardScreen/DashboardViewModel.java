package my_app.screens.dashboardScreen;

import megalodonte.application.ErrorReporter;
import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.base.state.State;
import megalodonte.router.v4.ScreenContext;
import megalodonte.utils.ThrowingSupplier;
import my_app.db.services.ClienteService;
import my_app.db.services.ConexaoBalancaService;
import my_app.db.services.PesagemService;
import my_app.db.services.ProdutoService;
import my_app.domain.components.Components;
import my_app.infra.balanca.LeitorBalanca;
import my_app.infra.balanca.LeitorBalancaFactory;
import my_app.infra.balanca.PesagemCalculo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DashboardViewModel {
    private static final Logger log = LoggerFactory.getLogger(DashboardViewModel.class);

    private final ProdutoService produtoService;
    private final ClienteService clienteService;
    private final PesagemService pesagemService;
    private final ConexaoBalancaService conexaoBalancaService;

    private final ScreenContext ctx;
    private LeitorBalanca leitorBalanca;

    /** Peso lido da balança em tempo real (mesmo do formulário de pesagem). */
    public final State<String> pesoAoVivo = State.of("—");
    public final State<Boolean> lendoBalanca = State.of(false);

    public final State<String> totalProdutos = new State<>("—");
    public final State<String> totalClientes = new State<>("—");
    public final State<String> totalPesagens = new State<>("—");
    public final State<String> totalPesagensMes = new State<>("—");

    public DashboardViewModel(ScreenContext ctx) {
        this.ctx = ctx;
        this.produtoService = createOrReport(ProdutoService::new);
        this.clienteService = createOrReport(ClienteService::new);
        this.pesagemService = createOrReport(PesagemService::new);
        this.conexaoBalancaService = createOrReport(ConexaoBalancaService::new);
    }

    public void carregar() {
        Async.Run(() -> {
            try {
                int produtos = produtoService.listar().size();
                int clientes = clienteService.listar().size();
                int pesagens = pesagemService.listar().size();

                LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
                LocalDate hoje = LocalDate.now();
                // filtrar() já anexa Cliente/Produto/Desconto a cada pesagem (feito pra tela de
                // listagem) — redundante aqui, só queremos o tamanho, mas o volume mensal de
                // pesagens é baixo o bastante pra não valer a pena criar um COUNT(*) dedicado.
                int pesagensMes = pesagemService.filtrar(null, null, null, null, inicioMes, hoje, null).size();

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

    // ---- balança ----

    /** Liga a leitura contínua da balança, atualizando {@link #pesoAoVivo} (mesmo do formulário). */
    public void iniciarLeituraBalanca() {
        ctx.scope().run(() -> {
            try {
                var config = conexaoBalancaService.buscarUnico();
                var leitor = LeitorBalancaFactory.criar(config);
                leitorBalanca = leitor;

                ctx.scope().onCancel(leitor::parar);
                if (ctx.scope().isCancelled()) return;

                leitor.iniciar(
                        peso -> UI.runOnUi(() -> {
                            pesoAoVivo.set(arrInt(peso));
                            lendoBalanca.set(true);
                        }),
                        erro -> UI.runOnUi(() -> {
                            lendoBalanca.set(false);
                            Components.ShowAlertError(erro);
                        })
                );
            } catch (Exception e) {
                log.error("Erro ao conectar com a balança", e);
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao conectar com a balança: " + e.getMessage()));
            }
        });
    }

    public void pararLeituraBalanca() {
        if (leitorBalanca != null) leitorBalanca.parar();
        lendoBalanca.set(false);
    }

    /** Converte um peso pra inteiro (sem casas decimais), como o André prefere. */
    private String arrInt(BigDecimal valor) {
        var inteiro = PesagemCalculo.arredondarInteiro(valor);
        return inteiro == null ? "" : inteiro.toBigInteger().toString();
    }

    public void onDestroy() {
        try {
            pararLeituraBalanca();
            produtoService.close();
            clienteService.close();
            pesagemService.close();
            conexaoBalancaService.close();
        } catch (Exception e) {
            log.warn("Erro ao fechar serviços do dashboard", e);
        }
    }
}
