package my_app.screens.configuracoesScreen;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.components.Card;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Container;
import megalodonte.props.ColumnProps;
import megalodonte.props.ContainerProps;
import megalodonte.router.v4.ScreenContext;
import megalodonte.v2.Show;
import my_app.domain.components.Components;
import my_app.domain.pesagem.TipoImpressao;

public class ConfiguracoesScreen implements ScreenComponent {
    private final ConfiguracoesViewModel vm;

    public ConfiguracoesScreen(ScreenContext ctx) {
        vm = new ConfiguracoesViewModel(ctx);
    }

    @Override
    public void onMount() {
        vm.load();
    }

    @Override
    public void onDestroy() {
        vm.onDestroy();
    }

    @Override
    public Component render() {
        return new Container(new ContainerProps().paddingAll(10)).children(
                new Card(new Column(new ColumnProps().paddingAll(20).spacingOf(15))
                        .c_child(Components.FormTitle("Configurações"))
                        .c_child(Show.when(vm.carregado,
                                () -> new Column(new ColumnProps().spacingOf(15))
                                        .c_child(Components.SelectColumn(Components.obrigatorio("Tipo de impressão"),
                                                ConfiguracoesViewModel.tiposImpressao, vm.tipoImpressao, TipoImpressao::descricao))
                                        .c_child(new Text("Os tickets serão enviados à impressora padrão do sistema."))
                                        .c_child(new Text("Térmica: bobina de 80 mm. Laser: duas vias em uma folha A4."))
                                        .c_child(Components.ButtonCadastro("Salvar", vm::salvar)),
                                () -> new Column(new ColumnProps().spacingOf(15))
                                        .c_child(new Text(vm.status))
                                        .c_child(Components.ButtonCadastro("Tentar novamente", vm::load)))))
        );
    }
}
