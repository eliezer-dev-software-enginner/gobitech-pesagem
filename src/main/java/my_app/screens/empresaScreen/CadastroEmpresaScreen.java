package my_app.screens.empresaScreen;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Container;
import megalodonte.router.v4.ScreenContext;
import my_app.domain.Data;
import my_app.domain.components.Components;
import megalodonte.components.*;
import megalodonte.components.layout_components.Row;
import megalodonte.props.*;

public class CadastroEmpresaScreen implements ScreenComponent {
    private final EmpresaViewModel vm;

    public CadastroEmpresaScreen(ScreenContext ctx) {
        vm = new EmpresaViewModel(ctx);
    }

    public void onMount() {
        vm.fetchData();
    }

    @Override
    public void onDestroy() {
        try {
            vm.onDestroy();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Component render() {
        return new Scroll(
                new Container(new ContainerProps().paddingAll(5))
                        .c_child(new SpacerVertical(10))
                        .c_child(form())
        );
    }

    Component form() {
        return new Card(new Column()
                .c_child(Components.FormTitle("Informações da empresa"))
                .c_child(new SpacerVertical(ThemeManager.theme().spacing().md()))
                .c_child(TopWithImage())
                .c_child(new Row(new RowProps().bottomVertically().spacingOf(ThemeManager.theme().spacing().sm()))
                        .children(
                                Components.InputColumnCnpjAlfanumerico("CPF/CNPJ", vm.cpfCnpj),
                                Components.InputColumn("Email", vm.email, "Ex: contato@empresa.com.br")
                        )
                )
                .c_child(new SpacerVertical(ThemeManager.theme().spacing().sm()))
                .c_child(Components.FormTitle("Endereço"))
                .c_child(new Row(new RowProps().bottomVertically().spacingOf(ThemeManager.theme().spacing().sm()))
                        .children(
                                Components.InputColumnCep("Cep", vm.cep),
                                Components.SelectColumn("UF", Data.ufList, vm.estadoSelected, it -> it),
                                Components.InputColumn("Cidade", vm.cidade, "Ex: Formosa"),
                                Components.InputColumn("Bairro", vm.bairro, "Ex: Centro"),
                                Components.InputColumn("Rua", vm.rua, "Ex: R. Dez"),
                                Components.InputColumn("Número", vm.numero, "Ex: 36")
                        )
                )
                .c_child(new SpacerVertical(20))
                .c_child(Components.ButtonCadastro("Salvar", vm::handleSave)));
    }

    Row TopWithImage() {
        var left = new Row(new RowProps().bottomVertically().spacingOf(ThemeManager.theme().spacing().sm()))
                .children(
                        Components.InputColumn("Nome", vm.nome, "Ex: Balanças Gobitech"),
                        Components.InputColumnPhone("Telefone/Celular", vm.telefone));

        return new Row()
                .r_child(left)
                .r_child(new SpacerHorizontal(ThemeManager.theme().spacing().sm()))
                .r_child(Components.ImageSelector("Mudar logomarca", vm.logoMarca,
                        new ImageProps().size(100), vm::handleUpdateLogoMarca));
    }
}
