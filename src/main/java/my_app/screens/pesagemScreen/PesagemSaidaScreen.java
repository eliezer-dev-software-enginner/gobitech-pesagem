package my_app.screens.pesagemScreen;

import megalodonte.base.components.Component;
import megalodonte.components.Text;
import megalodonte.props.TextProps;
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
    protected Component secaoExtra() {
        return new Text("Digite a placa para buscar a pesagem de entrada deste caminhão.",
                new TextProps().fontSize(13));
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
