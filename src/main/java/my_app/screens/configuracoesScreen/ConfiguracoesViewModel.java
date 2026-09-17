package my_app.screens.configuracoesScreen;

import javafx.stage.FileChooser;
import megalodonte.ComputedState;
import megalodonte.base.UI;
import megalodonte.base.state.State;
import megalodonte.router.v4.ScreenContext;
import my_app.core.events.EventBus;
import my_app.core.events.PreferenciasEvent;
import my_app.db.services.PreferenciasService;
import my_app.domain.components.Components;
import my_app.domain.pesagem.TipoImpressao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConfiguracoesViewModel {
    private static final Logger log = LoggerFactory.getLogger(ConfiguracoesViewModel.class);
    static final List<TipoImpressao> tiposImpressao = List.of(TipoImpressao.values());

    final State<String> logoHorizontal = State.of("");
    final ComputedState<Boolean> logoVazia =
            ComputedState.of(() -> logoHorizontal.get() == null || logoHorizontal.get().isBlank(), logoHorizontal);

    final State<TipoImpressao> tipoImpressao = State.of(TipoImpressao.LASER);
    final State<Boolean> carregado = State.of(false);
    final State<String> status = State.of("Carregando configurações...");

    private final AtomicBoolean ocupado = new AtomicBoolean();
    private final ScreenContext ctx;
    private volatile boolean destruido;

    public ConfiguracoesViewModel(ScreenContext ctx) {
        this.ctx = ctx;
    }

    public void load() {
        if (destruido || !ocupado.compareAndSet(false, true)) return;
        status.set("Carregando configurações...");
        ctx.scope().run(() -> {
            try (var service = new PreferenciasService()) {
                var tipo = service.buscarTipoImpressao();
                var imagemPath = service.getImagemHorizontalLogo();
                UI.runOnUi(() -> {
                    if (destruido) return;
                    tipoImpressao.set(tipo);
                    carregado.set(true);
                    logoHorizontal.set(imagemPath == null || imagemPath.isBlank() ? "" : imagemPath);
                });
            } catch (Exception e) {
                log.error("Erro ao carregar configurações", e);
                UI.runOnUi(() -> {
                    if (!destruido) status.set("Não foi possível carregar as configurações. Tente novamente.");
                });
            } finally {
                ocupado.set(false);
            }
        });
    }

    public void salvar() {
        if (destruido || !carregado.get() || !ocupado.compareAndSet(false, true)) return;
        var tipo = tipoImpressao.get();
        ctx.scope().run(() -> {
            try (var service = new PreferenciasService()) {
                service.salvarConfiguracoes(tipo, logoHorizontal.get());
                UI.runOnUi(() -> {
                    if (!destruido) Components.ShowPopup(ctx, "Configurações salvas com sucesso");
                    EventBus.getInstance().publish(PreferenciasEvent.salvas());
                });
            } catch (IllegalArgumentException e) {
                UI.runOnUi(() -> {
                    if (!destruido) Components.ShowAlertError(e.getMessage());
                });
            } catch (Exception e) {
                log.error("Erro ao salvar configurações", e);
                UI.runOnUi(() -> {
                    if (!destruido) Components.ShowAlertError("Não foi possível salvar as configurações. Tente novamente.");
                });
            } finally {
                ocupado.set(false);
            }
        });
    }

    public void handleUpdateLogoMarca() {
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Selecionar imagem");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg"));

        File arquivo = fileChooser.showOpenDialog(this.ctx.selfStage());
        if (arquivo != null) {
            logoHorizontal.set(arquivo.toURI().toString());
        }
    }

    public void limparLogo() {
        logoHorizontal.set("");
    }

    public void onDestroy() {
        destruido = true;
    }
}
