package my_app.screens.licensaScreen;

import megalodonte.ComputedState;
import megalodonte.base.state.State;
import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.router.v4.ScreenContext;
import megalodonte.v2.ListState;
import my_app.core.AppRoutes;
import my_app.db.models.LicensaModel;
import my_app.db.services.LicensaService;
import my_app.domain.Data;
import my_app.domain.SessaoUsuario;
import my_app.domain.components.Components;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class LicensaViewModel {
    private final ScreenContext ctx;
    private final LicensaService licensaService;

    final State<LocalDate> dataExpiracao = State.of(null);
    final State<String> codigoGerado = State.of("");
    final ListState<LicensaModel> licensasState = ListState.ofEmpty();

    // "Não" por padrão — a maioria das licenças geradas (ex.: pro próprio admin) não expira.
    final State<String> definirExpiracaoSelected = State.of(Data.simNaoList.getLast());
    final ComputedState<Boolean> definirExpiracao = ComputedState.of(
            () -> definirExpiracaoSelected.get().equals(Data.simNaoList.getFirst()),
            definirExpiracaoSelected
    );

    final ComputedState<Boolean> codigoGeradoVisible = ComputedState.of(
            () -> !codigoGerado.get().isEmpty(), codigoGerado
    );

    public LicensaViewModel(ScreenContext ctx) {
        this.ctx = ctx;
        this.licensaService = createOrReport(LicensaService::new);
    }

    private static <T> T createOrReport(megalodonte.utils.ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            megalodonte.application.ErrorReporter.handle(e);
            throw new IllegalStateException(e);
        }
    }

    public boolean acessoPermitido() {
        return SessaoUsuario.isAdmin();
    }

    public void bloquearAcesso() {
        Components.ShowAlertError("Só administradores podem gerar licença.");
        ctx.navigateAndCloseOthers(AppRoutes.Screens.HOME.name());
    }

    public void carregar() {
        Async.Run(() -> {
            try {
                var lista = licensaService.listar();
                UI.runOnUi(() -> licensasState.set(lista));
            } catch (Exception e) {
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao listar licenças: " + e.getMessage()));
            }
        });
    }

    public void gerar() {
        // Consulta o toggle, não só se dataExpiracao já tem valor — sem isso, escolher uma data
        // e depois mudar de ideia pra "Não" deixaria a data antiga (ainda no state, só não mais
        // visível na tela) sendo usada mesmo assim.
        if (definirExpiracao.get() && dataExpiracao.get() == null) {
            Components.ShowAlertError("Selecione a data de expiração.");
            return;
        }

        LocalDateTime expiraEm = definirExpiracao.get() ? dataExpiracao.get().atTime(23, 59, 59) : null;

        Async.Run(() -> {
            try {
                var licensa = licensaService.gerarNova(expiraEm);
                UI.runOnUi(() -> {
                    codigoGerado.set(licensa.getValor());
                    licensasState.add(licensa);
                    Components.ShowPopup(ctx, "Licença gerada com sucesso");
                });
            } catch (Exception e) {
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao gerar licença: " + e.getMessage()));
            }
        });
    }

    public void onDestroy() throws Exception {
        licensaService.close();
    }
}
