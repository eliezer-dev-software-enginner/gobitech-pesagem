package my_app.screens.pesagemScreen;

import megalodonte.router.v4.ScreenContext;

/**
 * Pesagem avulsa — caminhão direto na balança, sem entrada prévia. O operador digita a Tara
 * (peso vazio conhecido) e captura o peso bruto da balança.
 */
public class PesagemAvulsaScreen extends PesagemFormScreen {

    public PesagemAvulsaScreen(ScreenContext ctx) {
        super(ctx);
    }

    @Override
    protected PesagemFormViewModel criarViewModel(ScreenContext ctx) {
        return new PesagemAvulsaViewModel(ctx);
    }
}
