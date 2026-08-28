package my_app.screens.pesagemScreen;

import javafx.stage.FileChooser;
import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.base.state.State;
import megalodonte.router.v4.ScreenContext;
import megalodonte.v2.ListState;
import my_app.core.events.ClienteEvent;
import my_app.core.events.EventBus;
import my_app.core.events.PesagemEvent;
import my_app.core.events.ProdutoEvent;
import my_app.db.models.ConexaoCameraModel;
import my_app.db.models.DescontoModel;
import my_app.db.models.PesagemModel;
import my_app.db.models.ClienteModel;
import my_app.db.models.ProdutoModel;
import my_app.db.services.ClienteService;
import my_app.db.services.ConexaoBalancaService;
import my_app.db.services.ConexaoCameraService;
import my_app.db.services.DescontoService;
import my_app.db.services.PesagemService;
import my_app.db.services.ProdutoService;
import my_app.domain.components.Components;
import my_app.infra.balanca.LeitorBalanca;
import my_app.infra.balanca.LeitorBalancaFactory;
import my_app.infra.balanca.PesagemCalculo;
import my_app.infra.camera.CameraSnapshotClient;
import my_app.infra.camera.FotoPesagemStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;

/**
 * Base das 4 telas de pesagem (Entrada, Saída, Avulsa, Manual) — os formulários de
 * registro, sem a listagem/histórico (que é a {@code PesagemHistoricoScreen}). Reúne o que
 * os quatro tipos têm em comum: dados do caminhão/motorista/cliente/produto, os 8 descontos,
 * as fotos, o cálculo de peso líquido, a leitura da balança e a montagem/salvamento do modelo.
 * <p>
 * Diferente das telas de CRUD (que usam {@code ViewModelScreenContract} +
 * {@code ContratoTelaCrudV3}), o formulário é uma tela pura: monta, valida e salva uma pesagem
 * do seu próprio {@link #tipoPesagem()}.
 */
public abstract class PesagemFormViewModel {

    private static final Logger log = LoggerFactory.getLogger(PesagemFormViewModel.class);

    protected final ScreenContext ctx;
    protected final PesagemService pesagemService;
    protected final ClienteService clienteService;
    protected final ProdutoService produtoService;
    protected final DescontoService descontoService;
    protected final ConexaoBalancaService conexaoBalancaService;
    protected final ConexaoCameraService conexaoCameraService;
    private final CameraSnapshotClient cameraSnapshotClient = new CameraSnapshotClient();

    private LeitorBalanca leitorBalanca;
    protected final State<String> pesoAoVivo = State.of("—");
    protected final State<Boolean> lendoBalanca = State.of(false);
    @SuppressWarnings("rawtypes")
    private final java.util.function.Consumer<Object> eventListener = this::onEntityEvent;

    protected final State<String> motoristaNome = new State<>("");
    protected final State<String> motoristaDocumento = new State<>("");
    protected final State<String> placa = new State<>("");
    protected final State<String> notaFiscal = new State<>("");
    protected final State<String> observacoes = new State<>("");

    protected final State<String> pesoVeiculo = new State<>("");
    protected final State<String> pesoTotal = new State<>("");
    protected final State<String> pesoFinal = new State<>("");

    protected final ListState<ClienteModel> clientesState = ListState.ofEmpty();
    protected final ListState<ProdutoModel> produtosState = ListState.ofEmpty();
    protected final State<ClienteModel> clienteSelected = State.of(null);
    protected final State<ProdutoModel> produtoSelected = State.of(null);

    protected final State<String> avariados = new State<>("");
    protected final State<String> ardidos = new State<>("");
    protected final State<String> quebraArdidos = new State<>("");
    protected final State<String> impurezas = new State<>("");
    protected final State<String> quebraImpurezas = new State<>("");
    protected final State<String> umidade = new State<>("");
    protected final State<String> quebraUmidade = new State<>("");
    protected final State<String> outros = new State<>("");

    protected final State<String> fotoFrente1 = State.of(null);
    protected final State<String> fotoFrente2 = State.of(null);
    protected final State<String> fotoCostas1 = State.of(null);
    protected final State<String> fotoCostas2 = State.of(null);

    protected PesagemFormViewModel(ScreenContext ctx) {
        this.ctx = ctx;
        this.pesagemService = createOrReport(PesagemService::new);
        this.clienteService = createOrReport(ClienteService::new);
        this.produtoService = createOrReport(ProdutoService::new);
        this.descontoService = createOrReport(DescontoService::new);
        this.conexaoBalancaService = createOrReport(ConexaoBalancaService::new);
        this.conexaoCameraService = createOrReport(ConexaoCameraService::new);
        carregarClientesEProdutos();

        EventBus.getInstance().subscribe(eventListener);

        // Ao selecionar um produto, carrega o desconto padrão dele no campo "Outros" — só um
        // ponto de partida editável pelo operador.
        produtoSelected.subscribe(produto -> {
            if (produto == null) return;
            outros.set(produto.getDesconto() == null ? "" : produto.getDesconto().toPlainString());
        });

        // Peso líquido recalculado dinamicamente a cada mudança de bruto, tara ou de qualquer
        // desconto — dispensa o botão "Calcular" (o campo é de exibição, não editável).
        pesoTotal.subscribe(v -> recalcularPesoLiquido());
        pesoVeiculo.subscribe(v -> recalcularPesoLiquido());
        avariados.subscribe(v -> recalcularPesoLiquido());
        ardidos.subscribe(v -> recalcularPesoLiquido());
        quebraArdidos.subscribe(v -> recalcularPesoLiquido());
        impurezas.subscribe(v -> recalcularPesoLiquido());
        quebraImpurezas.subscribe(v -> recalcularPesoLiquido());
        umidade.subscribe(v -> recalcularPesoLiquido());
        quebraUmidade.subscribe(v -> recalcularPesoLiquido());
        outros.subscribe(v -> recalcularPesoLiquido());
    }

    // Reage a cliente/produto alterado pra manter os selects atualizados — desinscrito no
    // onDestroy, senão uma ViewModel já destruída (com os services fechados) continuaria
    // processando eventos em vão.
    private void onEntityEvent(Object event) {
        if (event instanceof ClienteEvent || event instanceof ProdutoEvent) {
            carregarClientesEProdutos();
        }
    }

    /**
     * O tipo desta tela, gravado em {@code pesagens.tipo_pesagem}: "entrada", "saida",
     * "avulsa" ou "manual".
     */
    protected abstract String tipoPesagem();

    /** Título do card (e rótulo de confirmação) deste tipo de pesagem. */
    protected abstract String tituloFormulario();

    /** Texto do botão de salvar (ex.: "Registrar entrada"). */
    protected String textoBotaoSalvar() {
        return "Registrar " + tituloFormulario().toLowerCase();
    }

    protected <T> T createOrReport(megalodonte.utils.ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("Erro ao criar dependência da tela de pesagem", e);
            throw new IllegalStateException(e);
        }
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

    // ---- balança ----

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
     * Recalcula o peso líquido em tempo real a partir de bruto, tara e descontos, delegando
     * pra {@link PesagemCalculo} (testável isoladamente). Chamado a cada mudança desses
     * campos, então o valor exibido está sempre atualizado sem botão "Calcular".
     */
    private void recalcularPesoLiquido() {
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

    // ---- montagem / salvamento ----

    protected PesagemModel montarModel() {
        var model = new PesagemModel();
        model.setMotoristaNome(motoristaNome.get().trim());
        model.setMotoristaDocumento(motoristaDocumento.get().trim());
        model.setPlaca(placa.get().trim());
        model.setNotaFiscal(notaFiscal.get().trim());
        model.setObservacoes(observacoes.get());
        model.setTipoPesagem(tipoPesagem());

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

    private String str(BigDecimal valor) {
        return valor == null ? "" : valor.toPlainString();
    }

    protected BigDecimal parseDecimal(String valor) {
        try {
            return valor == null || valor.isBlank() ? BigDecimal.ZERO : new BigDecimal(valor.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Salva a pesagem (Desconto + Pesagem). Persism seta id/tipo no mesmo model, então
     * {@link #capturarFotos()} já enxerga o id logo em seguida. Publica evento pra manter o
     * histórico sincronizado.
     */
    public void salvar() {
        var model = montarModel();
        var descontoModel = montarDesconto();

        Async.Run(() -> {
            try {
                var descontoSalvo = descontoService.salvar(descontoModel);
                model.setDescontoId(descontoSalvo.getId());

                pesagemService.salvar(model);
                capturarFotos(model);

                var comRelacoes = pesagemService.buscarComRelacoes(model.getId());
                log.info("Pesagem registrada: id={} placa={} tipo={} pesoLiquido={}",
                        comRelacoes.getId(), comRelacoes.getPlaca(), comRelacoes.getTipoPesagem(), comRelacoes.getPesoFinal());
                UI.runOnUi(() -> {
                    Components.ShowPopup(ctx, "Pesagem registrada com sucesso");
                    EventBus.getInstance().publish(PesagemEvent.criado(comRelacoes));
                    limparFormulario();
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
     * Captura foto das câmeras Intelbras configuradas (frente/costas) na hora da pesagem. O
     * slot da foto (1 ou 2) depende do tipo: entrada/avulsa/manual usam o slot 1, saída o slot 2
     * (mesma lógica das duas "visitas" que existia no modelo antigo de entrada/saída). Câmera
     * não configurada é pulada sem erro; falha de rede não derruba a pesagem (já salva).
     */
    private void capturarFotos(PesagemModel pesagem) {
        ConexaoCameraModel config;
        try {
            config = conexaoCameraService.buscarUnico();
        } catch (Exception e) {
            log.error("Erro ao carregar conexão das câmeras pra pesagem id={}", pesagem.getId(), e);
            return;
        }
        if (config == null) return;

        boolean slot2 = "saida".equals(tipoPesagem());
        boolean mudou = false;

        if (config.getFrenteIp() != null && !config.getFrenteIp().isBlank() && config.getFrentePorta() != null) {
            String uri = capturarUmaCamera(config.getFrenteIp(), config.getFrentePorta(), config.getFrenteCanal(),
                    config.getFrenteUsuario(), config.getFrenteSenha(), pesagem.getId(), "frente", slot2);
            if (uri != null) {
                if (slot2) pesagem.setFotoFrente2(uri); else pesagem.setFotoFrente1(uri);
                mudou = true;
            }
        }

        if (config.getCostasIp() != null && !config.getCostasIp().isBlank() && config.getCostasPorta() != null) {
            String uri = capturarUmaCamera(config.getCostasIp(), config.getCostasPorta(), config.getCostasCanal(),
                    config.getCostasUsuario(), config.getCostasSenha(), pesagem.getId(), "costas", slot2);
            if (uri != null) {
                if (slot2) pesagem.setFotoCostas2(uri); else pesagem.setFotoCostas1(uri);
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
                                     Integer pesagemId, String rotulo, boolean slot2) {
        try {
            var jpeg = cameraSnapshotClient.capturarSnapshot(ip, porta, usuario == null ? "" : usuario,
                    senha == null ? "" : senha, canal == null ? 1 : canal);
            String nomeArquivo = "pesagem_" + pesagemId + "_" + rotulo + "_" + (slot2 ? "2" : "1") + ".jpg";
            return FotoPesagemStorage.salvar(jpeg, nomeArquivo);
        } catch (Exception e) {
            log.warn("Falha ao capturar foto da câmera de {} pra pesagem id={}", rotulo, pesagemId, e);
            return null;
        }
    }

    public void escolherFoto(State<String> destino) {
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Selecionar foto");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg"));
        File arquivo = fileChooser.showOpenDialog(ctx.selfStage());
        if (arquivo != null) {
            destino.set(arquivo.toURI().toString());
        }
    }

    public void limparFormulario() {
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

    public void onDestroy() throws Exception {
        EventBus.getInstance().unsubscribe(eventListener);
        pararLeituraBalanca();
        this.pesagemService.close();
        this.clienteService.close();
        this.produtoService.close();
        this.descontoService.close();
        this.conexaoBalancaService.close();
        this.conexaoCameraService.close();
    }
}
