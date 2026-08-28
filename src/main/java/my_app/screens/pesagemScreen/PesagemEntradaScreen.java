package my_app.screens.pesagemScreen;

import megalodonte.router.v4.ScreenContext;

/** Pesagem de entrada — form completo com leitura da balança. */
public class PesagemEntradaScreen extends PesagemFormScreen {

    public PesagemEntradaScreen(ScreenContext ctx) {
        super(ctx);
    }

    @Override
    protected PesagemFormViewModel criarViewModel(ScreenContext ctx) {
        return new PesagemEntradaViewModel(ctx);
    }
}
