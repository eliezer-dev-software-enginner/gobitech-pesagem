package my_app.screens.pesagemScreen;

import megalodonte.base.route.v2.ScreenContextInterface;

/**
 * Pesagem manual: sem leitura da balança em tempo real. O operador digita a Tara e o peso
 * bruto, e o peso líquido é calculado automaticamente.
 */
public class PesagemManualViewModel extends PesagemFormViewModel {

    public PesagemManualViewModel(ScreenContextInterface ctx) {
        super(ctx);
    }

    @Override
    protected String tipoPesagem() {
        return "manual";
    }

    @Override
    protected String tituloFormulario() {
        return "Registrar pesagem manual";
    }
}
