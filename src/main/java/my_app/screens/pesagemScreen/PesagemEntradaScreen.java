package my_app.screens.pesagemScreen;

import megalodonte.base.route.v2.ScreenContextInterface;

/** Pesagem de entrada — form completo com leitura da balança. */
public class PesagemEntradaScreen extends PesagemFormScreen {

    public PesagemEntradaScreen(ScreenContextInterface ctx) {
        super(ctx);
    }

    @Override
    protected PesagemFormViewModel criarViewModel(ScreenContextInterface ctx) {
        return new PesagemEntradaViewModel(ctx);
    }

    @Override
    protected boolean permitirCapturarBruto() {
        return false;
    }

    @Override
    protected boolean brutoEditavel() {
        return false;
    }
}
