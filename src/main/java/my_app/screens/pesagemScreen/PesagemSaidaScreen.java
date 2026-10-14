package my_app.screens.pesagemScreen;

import megalodonte.base.route.v2.ScreenContextInterface;

/**
 * Pesagem de saída — digite a placa e os dados da última entrada são puxados. A entrada
 * permanece somente leitura e apenas a saída pode ser capturada na balança.
 */
public class PesagemSaidaScreen extends PesagemFormScreen {

    public PesagemSaidaScreen(ScreenContextInterface ctx) {
        super(ctx);
    }

    @Override
    protected PesagemFormViewModel criarViewModel(ScreenContextInterface ctx) {
        return new PesagemSaidaViewModel(ctx);
    }

    @Override
    protected boolean mostrarAvisoSaida() {
        return true;
    }

    @Override
    protected boolean permitirCapturarTara() {
        return false;
    }

    @Override
    protected boolean taraEditavel() {
        return false;
    }
}
