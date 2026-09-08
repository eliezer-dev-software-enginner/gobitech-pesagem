package my_app.screens.conexaoCameraScreen;

import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.base.state.State;
import megalodonte.router.v4.ScreenContext;
import my_app.db.models.ConexaoCameraModel;
import my_app.db.services.ConexaoCameraService;
import my_app.domain.components.Components;
import my_app.infra.camera.CameraSnapshotClient;
import my_app.infra.camera.FotoPesagemStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConexaoCameraViewModel {
    private static final Logger log = LoggerFactory.getLogger(ConexaoCameraViewModel.class);

    private final ScreenContext ctx;
    private final ConexaoCameraService conexaoCameraService;
    private final CameraSnapshotClient cameraSnapshotClient = new CameraSnapshotClient();

    final State<String> frenteIp = State.of("");
    final State<String> frentePorta = State.of("80");
    final State<String> frenteCanal = State.of("1");
    final State<String> frenteUsuario = State.of("");
    final State<String> frenteSenha = State.of("");
    final State<String> frentePreview = State.of(null);

    final State<String> costasIp = State.of("");
    final State<String> costasPorta = State.of("80");
    final State<String> costasCanal = State.of("1");
    final State<String> costasUsuario = State.of("");
    final State<String> costasSenha = State.of("");
    final State<String> costasPreview = State.of(null);

    public ConexaoCameraViewModel(ScreenContext ctx) {
        this.ctx = ctx;
        this.conexaoCameraService = createOrReport(ConexaoCameraService::new);
    }

    private static <T> T createOrReport(megalodonte.utils.ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            megalodonte.application.ErrorReporter.handle(e);
            throw new IllegalStateException(e);
        }
    }

    public void load() {
        Async.Run(() -> {
            try {
                var model = conexaoCameraService.buscarUnico();
                if (model == null) return;
                UI.runOnUi(() -> {
                    frenteIp.set(vazio(model.getFrenteIp()));
                    frentePorta.set(model.getFrentePorta() == null ? "80" : String.valueOf(model.getFrentePorta()));
                    frenteCanal.set(model.getFrenteCanal() == null ? "1" : String.valueOf(model.getFrenteCanal()));
                    frenteUsuario.set(vazio(model.getFrenteUsuario()));
                    frenteSenha.set(vazio(model.getFrenteSenha()));

                    costasIp.set(vazio(model.getCostasIp()));
                    costasPorta.set(model.getCostasPorta() == null ? "80" : String.valueOf(model.getCostasPorta()));
                    costasCanal.set(model.getCostasCanal() == null ? "1" : String.valueOf(model.getCostasCanal()));
                    costasUsuario.set(vazio(model.getCostasUsuario()));
                    costasSenha.set(vazio(model.getCostasSenha()));
                });
            } catch (Exception e) {
                log.error("Erro ao carregar conexão das câmeras", e);
                UI.runOnUi(() -> Components.ShowAlertError("Não foi possível carregar a conexão das câmeras."));
            }
        });
    }

    private String vazio(String valor) {
        return valor == null ? "" : valor;
    }

    public void salvar() {
        var model = new ConexaoCameraModel();
        model.setFrenteIp(blank(frenteIp.get()));
        model.setFrentePorta(parseIntOrNull(frentePorta.get()));
        model.setFrenteCanal(parseIntOrNull(frenteCanal.get()));
        model.setFrenteUsuario(blank(frenteUsuario.get()));
        model.setFrenteSenha(blank(frenteSenha.get()));

        model.setCostasIp(blank(costasIp.get()));
        model.setCostasPorta(parseIntOrNull(costasPorta.get()));
        model.setCostasCanal(parseIntOrNull(costasCanal.get()));
        model.setCostasUsuario(blank(costasUsuario.get()));
        model.setCostasSenha(blank(costasSenha.get()));

        Async.Run(() -> {
            try {
                conexaoCameraService.salvarOuAtualizar(model);
                UI.runOnUi(() -> Components.ShowPopup(ctx, "Conexão das câmeras salva com sucesso"));
            } catch (IllegalArgumentException e) {
                UI.runOnUi(() -> Components.ShowAlertError(e.getMessage()));
            } catch (Exception e) {
                log.error("Erro ao salvar conexão das câmeras", e);
                UI.runOnUi(() -> Components.ShowAlertError("Não foi possível salvar a conexão das câmeras. Tente novamente."));
            }
        });
    }

    private String blank(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private Integer parseIntOrNull(String valor) {
        try {
            return valor == null || valor.isBlank() ? null : Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void testarFrente() {
        testar(frenteIp, frentePorta, frenteUsuario, frenteSenha, frenteCanal, frentePreview, "frente");
    }

    public void testarCostas() {
        testar(costasIp, costasPorta, costasUsuario, costasSenha, costasCanal, costasPreview, "costas");
    }

    private void testar(State<String> ip, State<String> porta, State<String> usuario, State<String> senha,
                         State<String> canal, State<String> preview, String rotulo) {
        var ipValue = ip.get().trim();
        var portaValue = parseIntOrNull(porta.get());
        if (ipValue.isEmpty() || portaValue == null) {
            Components.ShowAlertError("Preencha IP e porta da câmera da " + rotulo + " antes de testar.");
            return;
        }
        var canalValue = parseIntOrNull(canal.get());

        Async.Run(() -> {
            try {
                var jpeg = cameraSnapshotClient.capturarSnapshot(ipValue, portaValue, usuario.get().trim(), senha.get().trim(),
                        canalValue == null ? 1 : canalValue);
                // Nome único por captura: o preview é um State<String> reativo — reescrever o
                // mesmo nome de arquivo de teste toda vez manteria a STRING da URI idêntica, e
                // o componente Image só recarrega quando o valor do State muda de verdade.
                var uri = FotoPesagemStorage.salvar(jpeg, "teste_camera_" + rotulo + "_" + System.currentTimeMillis() + ".jpg");
                UI.runOnUi(() -> {
                    preview.set(uri);
                    Components.ShowPopup(ctx, "Câmera da " + rotulo + " respondeu com sucesso");
                });
            } catch (Exception e) {
                log.error("Erro ao testar câmera da {}", rotulo, e);
                UI.runOnUi(() -> Components.ShowAlertError("Falha ao conectar na câmera da " + rotulo + ". Verifique IP, porta, usuário e senha."));
            }
        });
    }

    public void onDestroy() throws Exception {
        this.conexaoCameraService.close();
    }
}
