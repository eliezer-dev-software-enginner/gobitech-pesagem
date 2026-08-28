package my_app.screens.pesagemScreen;

import megalodonte.router.v4.ScreenContext;

/**
 * Pesagem avulsa: caminhão diretamente na balança, sem pré-registro de entrada. O operador
 * digita a Tara (peso vazio conhecido) e captura o peso bruto da balança.
 */
public class PesagemAvulsaViewModel extends PesagemFormViewModel {

    public PesagemAvulsaViewModel(ScreenContext ctx) {
        super(ctx);
    }

    @Override
    protected String tipoPesagem() {
        return "avulsa";
    }

    @Override
    protected String tituloFormulario() {
        return "Registrar pesagem avulsa";
    }
}
