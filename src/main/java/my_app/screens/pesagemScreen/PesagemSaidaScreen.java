package my_app.screens.pesagemScreen;

import megalodonte.base.components.Component;
import megalodonte.components.Text;
import megalodonte.props.TextProps;
import megalodonte.router.v4.ScreenContext;

/**
 * Pesagem de saída — digite a placa e os dados da última entrada são puxados; a Tara vem
 * dessa entrada pré-preenchida, mas o botão "Capturar" libera recapturá-la na balança (caminhão
 * vazio na volta); o peso bruto também é pesado na balança.
 */
public class PesagemSaidaScreen extends PesagemFormScreen {

    public PesagemSaidaScreen(ScreenContext ctx) {
        super(ctx);
    }

    @Override
    protected PesagemFormViewModel criarViewModel(ScreenContext ctx) {
        return new PesagemSaidaViewModel(ctx);
    }

    @Override
    protected Component secaoExtra() {
        return new Text("Digite a placa para buscar a pesagem de entrada deste caminhão.",
                new TextProps().fontSize(13));
    }
}
