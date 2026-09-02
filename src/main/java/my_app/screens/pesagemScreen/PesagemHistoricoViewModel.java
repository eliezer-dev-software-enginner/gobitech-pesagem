package my_app.screens.pesagemScreen;

import megalodonte.base.async.Async;
import megalodonte.base.UI;
import megalodonte.base.state.State;
import megalodonte.router.v4.ScreenContext;
import my_app.core.events.PesagemEvent;
import my_app.core.events.EventBus;
import my_app.db.models.PesagemModel;
import my_app.db.services.EmpresaService;
import my_app.db.services.PesagemService;
import my_app.domain.ViewModelScreenContract;
import my_app.domain.components.Components;
import my_app.infra.TicketPdfExporter;
import my_app.infra.TicketThermalExporter;
import pack.utilities.DatePack;
import my_app.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.stage.FileChooser;
import java.io.File;
import java.time.LocalDate;

/**
 * ViewModel do histórico de pesagens (a única tela de pesagem que é CRUD de listagem —
 * ver {@code PesagemHistoricoScreen}). Não cria nem edita pesagem: isso é feito nas 4 telas
 * de formulário; aqui é só consulta dos registros já salvos, com filtro e exclusão.
 */
public class PesagemHistoricoViewModel extends ViewModelScreenContract<PesagemModel> {

    private static final Logger log = LoggerFactory.getLogger(PesagemHistoricoViewModel.class);

    private final PesagemService pesagemService;
    private final EmpresaService empresaService;
    private final TicketPdfExporter ticketPdfExporter = new TicketPdfExporter();
    private final TicketThermalExporter ticketThermalExporter = new TicketThermalExporter();
    @SuppressWarnings("rawtypes")
    private final java.util.function.Consumer<Object> eventListener = this::onEntityEvent;

    final State<String> filtroPlaca = new State<>("");
    final State<String> filtroMotorista = new State<>("");
    final State<LocalDate> filtroDataInicio = State.of(null);
    final State<LocalDate> filtroDataFim = State.of(null);

    public PesagemHistoricoViewModel(ScreenContext ctx) {
        super(ctx);
        this.pesagemService = createOrReport(PesagemService::new);
        this.empresaService = createOrReport(EmpresaService::new);

        EventBus.getInstance().subscribe(eventListener);
    }

    // Reage a pesagem criada/excluída pra manter a lista atualizada — desinscrito no
    // onDestroy, senão uma ViewModel já destruída (com o service fechado) continuaria
    // processando eventos e quebraria com session nula.
    private void onEntityEvent(Object event) {
        if (event instanceof PesagemEvent) {
            fetchListData();
        }
    }

    @Override
    protected boolean matchesSearch(PesagemModel model, String query) {
        return contains(model.getPlaca(), query) || contains(model.getMotoristaNome(), query);
    }

    private boolean contains(String field, String query) {
        return field != null && field.toLowerCase().contains(query);
    }

    @Override
    public void fetchListData() {
        Async.Run(() -> {
            try {
                var list = pesagemService.listarComRelacoes();
                UI.runOnUi(() -> allDataList.set(list));
            } catch (Exception e) {
                log.error("Erro ao buscar pesagens", e);
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao buscar pesagens: " + e.getMessage()));
            }
        });
    }

    public void aplicarFiltro() {
        Async.Run(() -> {
            try {
                Long inicioMillis = filtroDataInicio.get() == null ? null
                        : DatePack.localDateParaMillis(filtroDataInicio.get());
                Long fimMillis = filtroDataFim.get() == null ? null
                        : DatePack.localDateParaMillis(filtroDataFim.get()) + 86399999L;

                var list = pesagemService.filtrar(
                        filtroPlaca.get().isBlank() ? null : filtroPlaca.get().trim(),
                        filtroMotorista.get().isBlank() ? null : filtroMotorista.get().trim(),
                        null, null, inicioMillis, fimMillis
                );
                UI.runOnUi(() -> allDataList.set(list));
            } catch (Exception e) {
                log.error("Erro ao filtrar pesagens", e);
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao filtrar: " + e.getMessage()));
            }
        });
    }

    @Override
    public void handleClickMenuDelete() {
        final var model = selected.get();
        if (model == null) return;

        Components.ShowAlertAdvice("Deseja excluir a pesagem da placa " + model.getPlaca(), () -> Async.Run(() -> {
            try {
                pesagemService.excluirById(model.getId());
                UI.runOnUi(() -> {
                    allDataList.removeIf(it -> it.getId().equals(model.getId()));
                    Components.ShowPopup(ctx, "Pesagem excluída com sucesso");
                    EventBus.getInstance().publish(PesagemEvent.excluido());
                });
            } catch (Exception e) {
                log.error("Erro ao excluir pesagem id={}", model.getId(), e);
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao tentar excluir: " + e.getMessage()));
            }
        }));
    }

    // O histórico não abre formulário de edição/criação (isso é feito nas 4 telas de pesagem) —
    // os métodos abaixo existem por exigência do contrato, mas não têm efeito.
    @Override
    public void populateFieldsFromModel() {
    }

    @Override
    public PesagemModel populateModelFromFields() {
        return null;
    }

    @Override
    public void clearForm() {
    }

    @Override
    public void handleAddOrUpdate() {
    }

    @Override
    public void onDestroy() throws Exception {
        EventBus.getInstance().unsubscribe(eventListener);
        this.pesagemService.close();
        this.empresaService.close();
    }

    /**
     * Exporta o ticket dessa pesagem em PDF e abre no visualizador padrão do sistema.
     */
    public void imprimirTicket(PesagemModel model) {
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Salvar ticket em PDF");
        fileChooser.setInitialFileName("ticket - " + Utils.timestampParaArquivo() + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File destino = fileChooser.showSaveDialog(ctx.selfStage());
        if (destino == null) return;

        Async.Run(() -> {
            try {
                var empresa = empresaService.buscarUnico();
                var comRelacoes = pesagemService.buscarComRelacoes(model.getId());
                var entrada = pesagemService.buscarEntradaVinculada(comRelacoes);
                ticketPdfExporter.gerar(destino, empresa, comRelacoes, entrada);
                log.info("Ticket de pesagem exportado: pesagemId={} arquivo={}", model.getId(), destino.getAbsolutePath());
                abrirArquivo(destino);
                UI.runOnUi(() -> Components.ShowPopup(ctx, "Ticket salvo em: " + destino.getAbsolutePath()));
            } catch (Exception e) {
                log.error("Erro ao gerar ticket da pesagem id={}", model.getId(), e);
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao gerar ticket: " + e.getMessage()));
            }
        });
    }

    /**
     * Imprime o ticket dessa pesagem na impressora térmica 80mm (padrão do sistema).
     */
    public void imprimirTicketTermica(PesagemModel model) {
        Async.Run(() -> {
            try {
                var empresa = empresaService.buscarUnico();
                var comRelacoes = pesagemService.buscarComRelacoes(model.getId());
                var entrada = pesagemService.buscarEntradaVinculada(comRelacoes);
                boolean ok = ticketThermalExporter.imprimir(empresa, comRelacoes, entrada);
                UI.runOnUi(() -> {
                    if (ok) {
                        Components.ShowPopup(ctx, "Ticket enviado para a impressora térmica");
                    } else {
                        Components.ShowAlertError("Não foi possível imprimir na impressora térmica. Verifique se há uma impressora padrão configurada.");
                    }
                });
            } catch (Exception e) {
                log.error("Erro ao imprimir ticket térmico da pesagem id={}", model.getId(), e);
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao imprimir ticket térmico: " + e.getMessage()));
            }
        });
    }

    private void abrirArquivo(File arquivo) {
        try {
            if (java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                java.awt.Desktop.getDesktop().open(arquivo);
            }
        } catch (Exception e) {
            log.warn("Erro ao abrir arquivo no visualizador padrão: {}", arquivo.getAbsolutePath(), e);
        }
    }
}
