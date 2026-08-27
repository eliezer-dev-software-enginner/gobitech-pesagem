package my_app.screens.pesagemScreen;

import javafx.stage.FileChooser;
import megalodonte.application.ErrorReporter;
import megalodonte.base.state.State;
import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.router.v4.ScreenContext;
import megalodonte.v2.ListState;
import my_app.db.models.ClienteModel;
import my_app.db.models.ConexaoCameraModel;
import my_app.db.models.DescontoModel;
import my_app.db.models.PesagemModel;
import my_app.db.models.ProdutoModel;
import my_app.db.services.ClienteService;
import my_app.db.services.ConexaoBalancaService;
import my_app.db.services.ConexaoCameraService;
import my_app.db.services.DescontoService;
import my_app.db.services.EmpresaService;
import my_app.db.services.PesagemService;
import my_app.db.services.ProdutoService;
import my_app.core.events.EntityEvent;
import my_app.core.events.EventBus;
import my_app.domain.ViewModelScreenContract;
import my_app.domain.components.Components;
import my_app.infra.TicketPdfExporter;
import my_app.infra.balanca.LeitorBalanca;
import my_app.infra.balanca.LeitorBalancaFactory;
import my_app.infra.balanca.PesagemCalculo;
import my_app.infra.camera.CameraSnapshotClient;
import my_app.infra.camera.FotoPesagemStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PesagemViewModel extends ViewModelScreenContract<PesagemModel> {
    private static final Logger log = LoggerFactory.getLogger(PesagemViewModel.class);

    private final ScreenContext ctx2;
    private final PesagemService pesagemService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final DescontoService descontoService;
    private final ConexaoBalancaService conexaoBalancaService;
    private final EmpresaService empresaService;
    private final ConexaoCameraService conexaoCameraService;
    private final TicketPdfExporter ticketPdfExporter = new TicketPdfExporter();
    private final CameraSnapshotClient cameraSnapshotClient = new CameraSnapshotClient();

    private LeitorBalanca leitorBalanca;
    final State<String> pesoAoVivo = State.of("—");
    final State<Boolean> lendoBalanca = State.of(false);

    final State<PesagemModel> pesagemSelecionada = State.of(null);

    final State<String> motoristaNome = new State<>("");
    final State<String> motoristaDocumento = new State<>("");
    final State<String> placa = new State<>("");
    final State<String> notaFiscal = new State<>("");
    final State<String> observacoes = new State<>("");

    final State<String> pesoVeiculo = new State<>("");
    final State<String> pesoTotal = new State<>("");
    final State<String> pesoFinal = new State<>("");

    final ListState<ClienteModel> clientesState = ListState.ofEmpty();
    final ListState<ProdutoModel> produtosState = ListState.ofEmpty();
    final State<ClienteModel> clienteSelected = State.of(null);
    final State<ProdutoModel> produtoSelected = State.of(null);

    final State<String> avariados = new State<>("");
    final State<String> ardidos = new State<>("");
    final State<String> quebraArdidos = new State<>("");
    final State<String> impurezas = new State<>("");
    final State<String> quebraImpurezas = new State<>("");
    final State<String> umidade = new State<>("");
    final State<String> quebraUmidade = new State<>("");
    final State<String> outros = new State<>("");

    final State<String> fotoFrente1 = State.of(null);
    final State<String> fotoFrente2 = State.of(null);
    final State<String> fotoCostas1 = State.of(null);
    final State<String> fotoCostas2 = State.of(null);

    // filtro
    final State<String> filtroPlaca = new State<>("");
    final State<String> filtroMotorista = new State<>("");
    final State<LocalDate> filtroDataInicio = State.of(null);
    final State<LocalDate> filtroDataFim = State.of(null);

    public PesagemViewModel(ScreenContext ctx) {
        super(ctx);
        this.ctx2 = ctx;
        this.pesagemService = createOrReport(PesagemService::new);
        this.clienteService = createOrReport(ClienteService::new);
        this.produtoService = createOrReport(ProdutoService::new);
        this.descontoService = createOrReport(DescontoService::new);
        this.conexaoBalancaService = createOrReport(ConexaoBalancaService::new);
        this.empresaService = createOrReport(EmpresaService::new);
        this.conexaoCameraService = createOrReport(ConexaoCameraService::new);
        carregarClientesEProdutos();

        EventBus.getInstance().subscribe(event -> {
            if (event instanceof EntityEvent<?> ee) {
                if (ee.entity() instanceof ClienteModel || ee.entity() instanceof ProdutoModel) {
                    carregarClientesEProdutos();
                }
            }
        });

        // Ao selecionar um produto, carrega o desconto padrão dele no campo "Outros" — só um
        // ponto de partida editável pelo operador. Em populateFieldsFromModel() (editar/clonar
        // uma pesagem existente), produtoSelected é setado ANTES do desconto real salvo, então
        // esse auto-preenchimento é sobrescrito pelo valor de verdade logo em seguida — não
        // inverter essa ordem lá, senão o desconto do produto passaria a sobrescrever o salvo.
        produtoSelected.subscribe(produto -> {
            if (produto == null) return;
            outros.set(produto.getDesconto() == null ? "" : produto.getDesconto().toPlainString());
        });

        // Ao digitar a placa, sugere a Tara da última pesagem "Entrada" em aberto pra ela —
        // evita pesar o caminhão vazio de novo na "Saída" da mesma visita. Busca é assíncrona
        // (precisa ir no banco); diferente do desconto do produto (que só lê um objeto já em
        // memória), aqui NÃO dá pra confiar que roda antes do populateFieldsFromModel() da
        // edição sobrescrever de volta — por isso o modoEdicao.get() é checado só quando o
        // resultado chega (UI.runOnUi), não no disparo: populateFieldsFromModel() seta a placa
        // antes de modoEdicaoState() virar true (ver ContratoTelaCrudV3.handleClickMenuEdit),
        // então checar no disparo veria false incorretamente e aplicaria a sugestão em cima da
        // Tara real de uma pesagem sendo editada.
        placa.subscribe(valor -> carregarTaraSugerida());
    }

    private void carregarTaraSugerida() {
        var placaValue = placa.get().trim();
        if (placaValue.isEmpty()) return;

        Async.Run(() -> {
            try {
                var tara = pesagemService.buscarTaraSugerida(placaValue);
                if (tara == null) return;

                UI.runOnUi(() -> {
                    if (modoEdicao.get()) return;
                    if (!placa.get().trim().equals(placaValue)) return; // placa já mudou de novo
                    pesoVeiculo.set(tara.toPlainString());
                    Components.ShowPopup(ctx2, "Tara preenchida com a última pesagem dessa placa");
                });
            } catch (Exception e) {
                log.error("Erro ao buscar tara sugerida pra placa {}", placaValue, e);
            }
        });
    }

    public void iniciarLeituraBalanca() {
        ctx2.scope().run(() -> {
            try {
                var config = conexaoBalancaService.buscarUnico();
                var leitor = LeitorBalancaFactory.criar(config);
                leitorBalanca = leitor;

                // se a tela já foi destruída (navegação pra outro lugar) enquanto
                // buscarUnico()/criar() ainda rodavam, o Router já cancelou ctx2.scope() —
                // onCancel fecha o leitor na hora, e o isCancelled() logo abaixo evita abrir
                // a porta/socket à toa depois disso.
                ctx2.scope().onCancel(leitor::parar);
                if (ctx2.scope().isCancelled()) return;

                leitor.iniciar(
                        peso -> UI.runOnUi(() -> {
                            pesoAoVivo.set(peso.toPlainString());
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

    public void capturarTara() {
        if (!lendoBalanca.get()) {
            Components.ShowAlertError("Balança não está conectada");
            return;
        }
        pesoVeiculo.set(pesoAoVivo.get());
    }

    public void capturarPesoBruto() {
        if (!lendoBalanca.get()) {
            Components.ShowAlertError("Balança não está conectada");
            return;
        }
        pesoTotal.set(pesoAoVivo.get());
    }

    /**
     * Delega pra {@link PesagemCalculo#calcularPesoLiquido}, que tem a fórmula de verdade e
     * é testável isoladamente (essa ViewModel não pode ser instanciada num teste JUnit puro).
     */
    public void calcularPesoLiquido() {
        var bruto = parseDecimal(pesoTotal.get());
        var tara = parseDecimal(pesoVeiculo.get());

        var percentualDesconto = parseDecimal(avariados.get())
                .add(parseDecimal(ardidos.get()))
                .add(parseDecimal(quebraArdidos.get()))
                .add(parseDecimal(impurezas.get()))
                .add(parseDecimal(quebraImpurezas.get()))
                .add(parseDecimal(umidade.get()))
                .add(parseDecimal(quebraUmidade.get()))
                .add(parseDecimal(outros.get()));

        var liquidoFinal = PesagemCalculo.calcularPesoLiquido(bruto, tara, percentualDesconto);

        pesoFinal.set(liquidoFinal.toPlainString());
    }

    private void carregarClientesEProdutos() {
        Async.Run(() -> {
            try {
                var clientes = clienteService.listar();
                var produtos = produtoService.listar();
                UI.runOnUi(() -> {
                    clientesState.set(clientes);
                    produtosState.set(produtos);
                });
            } catch (Exception e) {
                log.error("Erro ao carregar clientes/produtos", e);
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao carregar clientes/produtos: " + e.getMessage()));
            }
        });
    }

    @Override
    protected boolean matchesSearch(PesagemModel model, String query) {
        return contains(model.getPlaca(), query) || contains(model.getMotoristaNome(), query);
    }

    private boolean contains(String field, String query) {
        return field != null && field.toLowerCase().contains(query);
    }

    @Override
    public void populateFieldsFromModel() {
        final var data = pesagemSelecionada.get();
        if (data == null) return;

        motoristaNome.set(data.getMotoristaNome());
        motoristaDocumento.set(data.getMotoristaDocumento() == null ? "" : data.getMotoristaDocumento());
        placa.set(data.getPlaca());
        notaFiscal.set(data.getNotaFiscal() == null ? "" : data.getNotaFiscal());
        observacoes.set(data.getObservacoes() == null ? "" : data.getObservacoes());

        pesoVeiculo.set(data.getPesoVeiculo() == null ? "" : data.getPesoVeiculo().toPlainString());
        pesoTotal.set(data.getPesoTotal() == null ? "" : data.getPesoTotal().toPlainString());
        pesoFinal.set(data.getPesoFinal() == null ? "" : data.getPesoFinal().toPlainString());

        fotoFrente1.set(data.getFotoFrente1());
        fotoFrente2.set(data.getFotoFrente2());
        fotoCostas1.set(data.getFotoCostas1());
        fotoCostas2.set(data.getFotoCostas2());

        clientesState.get().stream().filter(c -> c.getId().equals(data.getClienteId())).findFirst()
                .ifPresent(clienteSelected::set);
        produtosState.get().stream().filter(p -> p.getId().equals(data.getProdutoId())).findFirst()
                .ifPresent(produtoSelected::set);

        var desconto = data.getDesconto();
        if (desconto != null) {
            avariados.set(str(desconto.getAvariados()));
            ardidos.set(str(desconto.getArdidos()));
            quebraArdidos.set(str(desconto.getQuebraArdidos()));
            impurezas.set(str(desconto.getImpurezas()));
            quebraImpurezas.set(str(desconto.getQuebraImpurezas()));
            umidade.set(str(desconto.getUmidade()));
            quebraUmidade.set(str(desconto.getQuebraUmidade()));
            outros.set(str(desconto.getOutros()));
        }
    }

    private String str(BigDecimal valor) {
        return valor == null ? "" : valor.toPlainString();
    }

    private BigDecimal parseDecimal(String valor) {
        try {
            return valor == null || valor.isBlank() ? BigDecimal.ZERO : new BigDecimal(valor.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public PesagemModel populateModelFromFields() {
        var model = modoEdicao.get() && pesagemSelecionada.get() != null
                ? pesagemSelecionada.get()
                : new PesagemModel();

        model.setMotoristaNome(motoristaNome.get().trim());
        model.setMotoristaDocumento(motoristaDocumento.get().trim());
        model.setPlaca(placa.get().trim());
        model.setNotaFiscal(notaFiscal.get().trim());
        model.setObservacoes(observacoes.get());

        model.setPesoVeiculo(parseDecimal(pesoVeiculo.get()));
        model.setPesoTotal(parseDecimal(pesoTotal.get()));
        model.setPesoFinal(parseDecimal(pesoFinal.get()));

        model.setFotoFrente1(fotoFrente1.get());
        model.setFotoFrente2(fotoFrente2.get());
        model.setFotoCostas1(fotoCostas1.get());
        model.setFotoCostas2(fotoCostas2.get());

        if (clienteSelected.get() != null) model.setClienteId(clienteSelected.get().getId());
        if (produtoSelected.get() != null) model.setProdutoId(produtoSelected.get().getId());

        return model;
    }

    private DescontoModel montarDesconto() {
        var d = new DescontoModel();
        d.setAvariados(parseDecimal(avariados.get()));
        d.setArdidos(parseDecimal(ardidos.get()));
        d.setQuebraArdidos(parseDecimal(quebraArdidos.get()));
        d.setImpurezas(parseDecimal(impurezas.get()));
        d.setQuebraImpurezas(parseDecimal(quebraImpurezas.get()));
        d.setUmidade(parseDecimal(umidade.get()));
        d.setQuebraUmidade(parseDecimal(quebraUmidade.get()));
        d.setOutros(parseDecimal(outros.get()));
        return d;
    }

    /**
     * Exporta o ticket dessa pesagem em PDF (o app original imprimia direto na térmica via
     * ESC/POS — aqui, como no plics-sw, o operador escolhe onde salvar e imprime pelo
     * visualizador de PDF padrão do sistema; ver TicketPdfExporter).
     */
    public void imprimirTicket(PesagemModel model) {
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Salvar ticket em PDF");
        fileChooser.setInitialFileName("ticket_pesagem_" + model.getId() + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File destino = fileChooser.showSaveDialog(ctx2.selfStage());
        if (destino == null) return;

        Async.Run(() -> {
            try {
                var empresa = empresaService.buscarUnico();
                ticketPdfExporter.gerar(destino, empresa, model);
                log.info("Ticket de pesagem exportado: pesagemId={} arquivo={}", model.getId(), destino.getAbsolutePath());
                abrirArquivo(destino);
                UI.runOnUi(() -> Components.ShowPopup(ctx2, "Ticket salvo em: " + destino.getAbsolutePath()));
            } catch (Exception e) {
                log.error("Erro ao gerar ticket da pesagem id={}", model.getId(), e);
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao gerar ticket: " + e.getMessage()));
            }
        });
    }

    /**
     * Abre o PDF gerado no visualizador padrão do sistema — de lá o operador já consegue
     * imprimir de verdade (Ctrl+P), sem o app precisar falar com impressora nenhuma. Se o
     * ambiente não suportar (ex.: sem gerenciador de desktop configurado), falha em silêncio;
     * o arquivo já foi salvo e o caminho aparece no popup de qualquer forma.
     */
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

    public void escolherFoto(State<String> destino) {
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Selecionar foto");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg"));
        File arquivo = fileChooser.showOpenDialog(ctx2.selfStage());
        if (arquivo != null) {
            destino.set(arquivo.toURI().toString());
        }
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
                        : my_app.utils.DateUtils.localDateParaMillis(filtroDataInicio.get());
                Long fimMillis = filtroDataFim.get() == null ? null
                        : my_app.utils.DateUtils.localDateParaMillis(filtroDataFim.get()) + 86399999L;

                var list = pesagemService.filtrar(
                        filtroPlaca.get().isBlank() ? null : filtroPlaca.get().trim(),
                        filtroMotorista.get().isBlank() ? null : filtroMotorista.get().trim(),
                        clienteSelected.get() != null ? clienteSelected.get().getId() : null,
                        produtoSelected.get() != null ? produtoSelected.get().getId() : null,
                        inicioMillis, fimMillis
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
        final var model = pesagemSelecionada.get();
        if (model == null) return;

        Components.ShowAlertAdvice("Deseja excluir a pesagem da placa " + model.getPlaca(), () -> Async.Run(() -> {
            try {
                pesagemService.excluirById(model.getId());
                UI.runOnUi(() -> {
                    allDataList.removeIf(it -> it.getId().equals(model.getId()));
                    Components.ShowPopup(ctx, "Pesagem excluída com sucesso");
                    EventBus.getInstance().publish(EntityEvent.excluido(model.getId()));
                });
            } catch (Exception e) {
                log.error("Erro ao excluir pesagem id={}", model.getId(), e);
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao tentar excluir: " + e.getMessage()));
            }
        }));
    }

    @Override
    public void handleAddOrUpdate() {
        salvar(false);
    }

    /** Igual {@link #handleAddOrUpdate()}, mas já abre o diálogo de "Salvar ticket em PDF" logo em seguida. */
    public void handleAddOrUpdateEBaixarTicket() {
        salvar(true);
    }

    private void salvar(boolean tambemBaixarTicket) {
        if (modoEdicao.get() && pesagemSelecionada.get() == null) return;

        boolean editando = modoEdicao.get();
        var model = populateModelFromFields();
        var descontoModel = montarDesconto();

        Async.Run(() -> {
            try {
                if (editando && model.getDescontoId() != null) {
                    descontoModel.setId(model.getDescontoId());
                    descontoService.atualizar(descontoModel);
                } else {
                    var descontoSalvo = descontoService.salvar(descontoModel);
                    model.setDescontoId(descontoSalvo.getId());
                }

                if (editando) {
                    pesagemService.atualizar(model);
                } else {
                    // salvar() seta id/operacao de volta no MESMO model (Persism), então
                    // capturarFotosAutomaticamente já enxerga os dois logo em seguida. Só em
                    // pesagem NOVA — reeditar uma pesagem existente não deve disparar as
                    // câmeras de novo, o caminhão pode nem estar mais lá.
                    pesagemService.salvar(model);
                    capturarFotosAutomaticamente(model);
                }

                var comRelacoes = pesagemService.buscarComRelacoes(model.getId());
                boolean finalEditando = editando;
                UI.runOnUi(() -> {
                    if (finalEditando) {
                        allDataList.updateIf(it -> it.getId().equals(comRelacoes.getId()), it -> comRelacoes);
                        Components.ShowPopup(ctx, "Pesagem atualizada com sucesso");
                        EventBus.getInstance().publish(EntityEvent.editado(comRelacoes));
                    } else {
                        allDataList.add(comRelacoes);
                        Components.ShowPopup(ctx, "Pesagem cadastrada com sucesso");
                        EventBus.getInstance().publish(EntityEvent.criado(comRelacoes));
                    }
                    // Antes de voltarParaLista(): o diálogo de salvar ainda faz sentido com o
                    // formulário na tela; a tela em si não muda, só o formIsVisible da ViewModel.
                    if (tambemBaixarTicket) imprimirTicket(comRelacoes);
                    voltarParaLista();
                });
            } catch (IllegalArgumentException e) {
                UI.runOnUi(() -> Components.ShowAlertError(e.getMessage()));
            } catch (Exception e) {
                log.error("Erro inesperado ao salvar pesagem", e);
                UI.runOnUi(() -> Components.ShowAlertError("Erro inesperado: " + e.getMessage()));
            }
        });
    }

    /**
     * Captura foto das duas câmeras Intelbras configuradas (frente/costas), na hora da
     * pesagem — não na hora de imprimir o ticket, que era o timing errado do app original (ver
     * docs/TODO.md). "Entrada" preenche o slot 1 de cada câmera, "Saída" o slot 2, mesma regra
     * que já existe pra Tara sugerida (duas visitas por placa: uma de entrada, uma de saída).
     * <p>
     * Câmera não configurada (IP em branco) é pulada, sem erro — as duas são opcionais e
     * independentes (ver ConexaoCameraService). Falha de rede/autenticação numa câmera não
     * derruba a pesagem, que já está salva no banco nesse ponto: só fica sem aquela foto.
     */
    private void capturarFotosAutomaticamente(PesagemModel pesagem) {
        ConexaoCameraModel config;
        try {
            config = conexaoCameraService.buscarUnico();
        } catch (Exception e) {
            log.error("Erro ao carregar conexão das câmeras pra pesagem id={}", pesagem.getId(), e);
            return;
        }
        if (config == null) return;

        boolean isEntrada = "Entrada".equals(pesagem.getOperacao());
        boolean mudou = false;

        if (config.getFrenteIp() != null && !config.getFrenteIp().isBlank() && config.getFrentePorta() != null) {
            String uri = capturarUmaCamera(config.getFrenteIp(), config.getFrentePorta(), config.getFrenteCanal(),
                    config.getFrenteUsuario(), config.getFrenteSenha(), pesagem.getId(), "frente", isEntrada);
            if (uri != null) {
                if (isEntrada) pesagem.setFotoFrente1(uri); else pesagem.setFotoFrente2(uri);
                mudou = true;
            }
        }

        if (config.getCostasIp() != null && !config.getCostasIp().isBlank() && config.getCostasPorta() != null) {
            String uri = capturarUmaCamera(config.getCostasIp(), config.getCostasPorta(), config.getCostasCanal(),
                    config.getCostasUsuario(), config.getCostasSenha(), pesagem.getId(), "costas", isEntrada);
            if (uri != null) {
                if (isEntrada) pesagem.setFotoCostas1(uri); else pesagem.setFotoCostas2(uri);
                mudou = true;
            }
        }

        if (mudou) {
            try {
                pesagemService.atualizar(pesagem);
            } catch (Exception e) {
                log.error("Erro ao salvar as fotos capturadas na pesagem id={}", pesagem.getId(), e);
            }
        }
    }

    private String capturarUmaCamera(String ip, Integer porta, Integer canal, String usuario, String senha,
                                      Integer pesagemId, String rotulo, boolean isEntrada) {
        try {
            var jpeg = cameraSnapshotClient.capturarSnapshot(ip, porta, usuario == null ? "" : usuario,
                    senha == null ? "" : senha, canal == null ? 1 : canal);
            String nomeArquivo = "pesagem_" + pesagemId + "_" + rotulo + "_" + (isEntrada ? "1" : "2") + ".jpg";
            return FotoPesagemStorage.salvar(jpeg, nomeArquivo);
        } catch (Exception e) {
            log.warn("Falha ao capturar foto da câmera de {} pra pesagem id={}", rotulo, pesagemId, e);
            return null;
        }
    }


    @Override
    public void clearForm() {
        motoristaNome.set("");
        motoristaDocumento.set("");
        placa.set("");
        notaFiscal.set("");
        observacoes.set("");
        pesoVeiculo.set("");
        pesoTotal.set("");
        pesoFinal.set("");
        clienteSelected.set(null);
        produtoSelected.set(null);
        avariados.set("");
        ardidos.set("");
        quebraArdidos.set("");
        impurezas.set("");
        quebraImpurezas.set("");
        umidade.set("");
        quebraUmidade.set("");
        outros.set("");
        fotoFrente1.set(null);
        fotoFrente2.set(null);
        fotoCostas1.set(null);
        fotoCostas2.set(null);
    }

    @Override
    public void onDestroy() throws Exception {
        pararLeituraBalanca();
        this.pesagemService.close();
        this.clienteService.close();
        this.produtoService.close();
        this.descontoService.close();
        this.conexaoBalancaService.close();
        this.empresaService.close();
        this.conexaoCameraService.close();
    }
}
