package my_app.screens.pesagemScreen;

import megalodonte.base.route.v2.ScreenContextInterface;

/**
 * Pesagem avulsa — caminhão direto na balança, sem entrada prévia. O operador digita a Tara
 * (peso vazio conhecido) e captura o peso bruto da balança.
 */
public class PesagemAvulsaScreen extends PesagemFormScreen {

    public PesagemAvulsaScreen(ScreenContextInterface ctx) {
        super(ctx);
    }

    @Override
    protected PesagemFormViewModel criarViewModel(ScreenContextInterface ctx) {
        return new PesagemAvulsaViewModel(ctx);
    }

    @Override
    protected boolean permitirCapturarTara() {
        return false;
    }
}
