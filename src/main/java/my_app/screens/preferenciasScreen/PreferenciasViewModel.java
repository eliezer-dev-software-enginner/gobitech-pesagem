package my_app.screens.preferenciasScreen;

import megalodonte.router.v4.ScreenContext;
import my_app.core.AppRoutes;
import my_app.domain.SessaoUsuario;
import my_app.domain.components.Components;

public class PreferenciasViewModel {
    private final ScreenContext ctx;

    public PreferenciasViewModel(ScreenContext ctx) {
        this.ctx = ctx;
    }

    public void signOut() {
        SessaoUsuario.logout();
        ctx.navigateAndCloseOthers(AppRoutes.Screens.AUTH.name());
    }
}
