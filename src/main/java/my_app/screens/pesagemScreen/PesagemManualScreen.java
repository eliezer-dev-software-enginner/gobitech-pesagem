package my_app.screens.pesagemScreen;

import megalodonte.router.v4.ScreenContext;

/**
 * Pesagem manual — sem leitura da balança em tempo real: o operador digita a Tara e o peso
 * bruto, e o peso líquido é calculado automaticamente.
 */
public class PesagemManualScreen extends PesagemFormScreen {

    public PesagemManualScreen(ScreenContext ctx) {
        super(ctx);
    }

    @Override
    protected PesagemFormViewModel criarViewModel(ScreenContext ctx) {
        return new PesagemManualViewModel(ctx);
    }

    @Override
    protected boolean usarBalanca() {
        return false;
    }

    @Override
    protected boolean permitirCapturarTara() {
        return false;
    }

    @Override
    protected boolean permitirCapturarBruto() {
        return false;
    }
}
