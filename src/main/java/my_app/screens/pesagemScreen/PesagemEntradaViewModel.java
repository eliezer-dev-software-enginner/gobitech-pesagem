package my_app.screens.pesagemScreen;

import megalodonte.base.route.v2.ScreenContextInterface;

/**
 * Pesagem de entrada: caminhão vazio chegando pra carregar. Formulário completo
 * (dados do motorista/caminhão/cliente/produto + balança + descontos + fotos).
 */
public class PesagemEntradaViewModel extends PesagemFormViewModel {

    public PesagemEntradaViewModel(ScreenContextInterface ctx) {
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
