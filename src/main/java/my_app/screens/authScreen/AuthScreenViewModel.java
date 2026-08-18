package my_app.screens.authScreen;

import megalodonte.base.state.State;
import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.router.v4.ScreenContext;
import my_app.core.AppRoutes;
import my_app.db.services.LicensaService;
import my_app.db.services.PreferenciasService;
import my_app.db.services.UsuarioService;
import my_app.domain.SessaoUsuario;
import my_app.domain.components.Components;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthScreenViewModel {

    private static final Logger log = LoggerFactory.getLogger(AuthScreenViewModel.class);

    private final UsuarioService usuarioService;
    private final LicensaService licensaService;
    private final PreferenciasService preferenciasService;

    final State<String> loginState = State.of("");
    final State<String> passwordState = State.of("");

    public AuthScreenViewModel() {
        this.usuarioService = createOrReport(UsuarioService::new);
        this.licensaService = createOrReport(LicensaService::new);
        this.preferenciasService = createOrReport(PreferenciasService::new);
    }

    private static <T> T createOrReport(megalodonte.utils.ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            megalodonte.application.ErrorReporter.handle(e);
            throw new IllegalStateException(e);
        }
    }

    void entrar(ScreenContext ctx) {
        String loginValue = loginState.get().trim();
        String senhaValue = passwordState.get().trim();

        if (loginValue.isEmpty() || senhaValue.isEmpty()) {
            Components.ShowAlertError("Informe login e senha");
            return;
        }

        Async.Run(() -> {
            try {
                var usuario = usuarioService.autenticar(loginValue, senhaValue);
                if (usuario == null) {
                    UI.runOnUi(() -> Components.ShowAlertError("Login ou senha inválidos"));
                    return;
                }

                // Admin sempre entra, mesmo com licença expirada/ausente — é ele quem
                // precisa logar pra gerar uma nova (evita ficar trancado pra fora).
                if (!Boolean.TRUE.equals(usuario.getAdmin())) {
                    var licensaMaisRecente = licensaService.buscarMaisRecente();
                    if (licensaMaisRecente != null && licensaMaisRecente.expirada()) {
                        UI.runOnUi(() -> Components.ShowAlertError(
                                "Licença expirada. Contate o administrador para gerar uma nova."));
                        return;
                    }
                }

                SessaoUsuario.login(usuario);
                marcarPrimeiroAcessoConcluido();

                UI.runOnUi(() -> {
                    Components.ShowPopup(ctx, "Seja bem-vindo(a), " + usuario.getNome() + "!");
                    ctx.navigate(AppRoutes.Screens.HOME.name());
                });
            } catch (Exception e) {
                log.error("Erro ao fazer login", e);
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao entrar: " + e.getMessage()));
            }
        });
    }

    private void marcarPrimeiroAcessoConcluido() throws Exception {
        var prefs = preferenciasService.listar();
        if (prefs.isEmpty()) return;
        var pref = prefs.getFirst();
        if (!pref.isFirstAccess()) return;
        pref.setPrimeiroAcesso(0);
        preferenciasService.atualizar(pref);
    }

    public void onDestroy() throws Exception {
        usuarioService.close();
        licensaService.close();
        preferenciasService.close();
    }
}
