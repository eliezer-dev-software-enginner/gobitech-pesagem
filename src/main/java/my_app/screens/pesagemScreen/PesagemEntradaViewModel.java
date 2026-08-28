package my_app.screens.pesagemScreen;

import megalodonte.router.v4.ScreenContext;

/**
 * Pesagem de entrada: caminhão vazio chegando pra carregar. Formulário completo
 * (dados do motorista/caminhão/cliente/produto + balança + descontos + fotos).
 */
public class PesagemEntradaViewModel extends PesagemFormViewModel {

    public PesagemEntradaViewModel(ScreenContext ctx) {
        super(ctx);
    }

    @Override
    protected String tipoPesagem() {
        return "entrada";
    }

    @Override
    protected String tituloFormulario() {
        return "Registrar pesagem de entrada";
    }
}
