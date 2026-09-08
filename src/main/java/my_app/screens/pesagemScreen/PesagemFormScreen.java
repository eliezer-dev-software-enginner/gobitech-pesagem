package my_app.screens.pesagemScreen;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.components.Card;
import megalodonte.components.LineHorizontal;
import megalodonte.components.SpacerVertical;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.FlowRow;
import megalodonte.props.ColumnProps;
import megalodonte.props.FlowRowProps;
import megalodonte.props.ImageProps;
import megalodonte.router.v4.ScreenContext;
import my_app.domain.components.Components;

/**
 * Layout-base das 4 telas de pesagem (Entrada, Saída, Avulsa, Manual). Monta as seções
 * comuns do formulário — dados do caminhão/motorista/cliente/produto, descontos, fotos,
 * observações e o botão de salvar — deixando cada tipo controlar, via hooks, se usa a
 * balança ao vivo, se o peso bruto é capturável, e se mostra uma seção extra no topo
 * (ex.: a busca por placa da pesagem de Saída).
 */
public abstract class PesagemFormScreen implements ScreenComponent {

    protected final ScreenContext screenContext;
    protected final PesagemFormViewModel vm;

    protected PesagemFormScreen(ScreenContext ctx) {
        this.screenContext = ctx;
        this.vm = criarViewModel(ctx);
    }

    /** Cada tipo instancia a própria ViewModel (Entrada/Saída/Avulsa/Manual). */
    protected abstract PesagemFormViewModel criarViewModel(ScreenContext ctx);

    /** Se o tipo usa a leitura da balança em tempo real (Manual sobrescreve pra {@code false}). */
    protected boolean usarBalanca() {
        return true;
    }

    /** Se o botão "Capturar" do peso bruto fica visível (Saída/Entrada/Avulsa sim; Manual não). */
    protected boolean permitirCapturarBruto() {
        return true;
    }

    /**
     * Se o campo de peso bruto aceita digitação. Quando {@code permitirCapturarBruto()} é
     * verdadeiro o campo fica somente-leitura (o valor só entra pelo botão "Capturar") —
     * este hook cobre os casos sem botão de captura onde o peso também não deve ser digitado.
     */
    protected boolean brutoEditavel() {
        return true;
    }

    /** Se o botão "Capturar" da tara fica visível (Manual não; Saída puxa da entrada). */
    protected boolean permitirCapturarTara() {
        return true;
    }

    /**
     * Se o campo da tara aceita digitação. Quando {@code permitirCapturarTara()} é verdadeiro o
     * campo fica somente-leitura (o valor só entra pelo botão "Capturar") — este hook cobre os
     * casos sem botão de captura onde a tara também não deve ser digitada (ex.: a Saída, que
     * recebe a tara da entrada).
     */
    protected boolean taraEditavel() {
        return true;
    }

    /** Seção extra no topo do formulário (Ex.: busca por placa da Saída). Vazio por padrão. */
    protected Component secaoExtra() {
        return new Column();
    }

    @Override
    public void onMount() {
        if (usarBalanca()) {
            vm.iniciarLeituraBalanca();
        }
    }

    @Override
    public void onDestroy() {
        try {
            vm.onDestroy();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Component render() {
        return Components.ScrollPaneDefault(
                new Card(
                        new Column(new ColumnProps().paddingAll(20).spacingOf(15))
                                .c_child(Components.FormTitle(vm.tituloFormulario()))
                                .c_child(secaoExtra())
                                .c_child(secaoDados())
                                .c_child(secaoPesos())
                                .c_child(secaoDescontos())
                                .c_child(secaoFotos())
                                .c_child(Components.TextAreaColumn("Observações", vm.observacoes,
                                        "Alguma observação sobre a pesagem?", 60, 160))
                                .c_child(secaoAcoes())
                )
        );
    }

    private Component secaoDados() {
        return new Column(new ColumnProps().spacingOf(10))
                .c_child(Components.FormTitle("Dados da pesagem"))
                .c_child(new FlowRow(new FlowRowProps().spacingOf(10))
                        .children(
                                Components.InputColumnUppercase(Components.obrigatorio("Placa"), vm.placa, "Ex: ABC1D23"),
                                Components.InputColumn("Nome do motorista", vm.motoristaNome, "Ex: José da Silva"),
                                Components.InputRgCpf("Documento do motorista", vm.motoristaDocumento),
                                Components.SelectColumn("Cliente", vm.clientesState, vm.clienteSelected,
                                        c -> c.getLoja(), true),
                                Components.SelectColumn("Produto", vm.produtosState, vm.produtoSelected,
                                        p -> p.getNome(), true),
                                Components.InputColumn("Nota fiscal", vm.notaFiscal, "Nº da nota (se houver)")
                        )
                );
    }

    private Component secaoPesos() {
        var pesos = new Column(new ColumnProps().spacingOf(10))
                .c_child(Components.FormTitle("Pesos"));

        if (usarBalanca()) {
            pesos.c_child(new SpacerVertical(5))
                    .c_child(Components.TextWithValue("Peso da balança agora (Kg): ", vm.pesoAoVivo))
                    .c_child(new SpacerVertical(10));
        }

        var linha = new FlowRow(new FlowRowProps().spacingOf(10));
        linha.children(
                componenteTara(),
                componenteBruto(),
                Components.InputColumnInteger("Peso líquido (Kg)", vm.pesoFinal, "", true)
        );

        return pesos.c_child(linha);
    }

    private Component componenteTara() {
        if (permitirCapturarTara()) {
            return Components.InputWithButtonRowInteger("Tara (Kg)", "Ex: 8500", "Capturar",
                    vm.pesoVeiculo, vm::capturarTara, true);
        }
        return Components.InputColumnInteger("Tara (Kg)", vm.pesoVeiculo, "Ex: 8500", !taraEditavel());
    }

    private Component componenteBruto() {
        if (permitirCapturarBruto()) {
            return Components.InputWithButtonRowInteger("Peso bruto (Kg)", "Ex: 32000", "Capturar",
                    vm.pesoTotal, vm::capturarPesoBruto, true);
        }
        return Components.InputColumnInteger("Peso bruto (Kg)", vm.pesoTotal, "Ex: 32000", !brutoEditavel());
    }

    private Component secaoDescontos() {
        return new Column(new ColumnProps().spacingOf(10))
                .c_child(new SpacerVertical(5))
                .c_child(new LineHorizontal())
                .c_child(new SpacerVertical(5))
                .c_child(Components.FormTitle("Descontos (%)"))
                .c_child(new FlowRow(new FlowRowProps().spacingOf(10))
                        .children(
                                Components.InputColumnDecimal("Avariados", vm.avariados, "0"),
                                Components.InputColumnDecimal("Ardidos", vm.ardidos, "0"),
                                Components.InputColumnDecimal("Quebra ardidos", vm.quebraArdidos, "0"),
                                Components.InputColumnDecimal("Impurezas", vm.impurezas, "0"),
                                Components.InputColumnDecimal("Quebra impurezas", vm.quebraImpurezas, "0"),
                                Components.InputColumnDecimal("Umidade", vm.umidade, "0"),
                                Components.InputColumnDecimal("Quebra umidade", vm.quebraUmidade, "0"),
                                Components.InputColumnDecimal("Outros", vm.outros, "0")
                        )
                );
    }

    private Component secaoFotos() {
        return new Column(new ColumnProps().spacingOf(10))
                .c_child(new SpacerVertical(5))
                .c_child(new LineHorizontal())
                .c_child(new SpacerVertical(5))
                .c_child(Components.FormTitle("Fotos"))
                .c_child(new FlowRow(new FlowRowProps().spacingOf(10))
                        .children(
                                Components.ImageSelector("Frente 1", vm.fotoFrente1, new ImageProps().size(100),
                                        () -> vm.escolherFoto(vm.fotoFrente1)),
                                Components.ImageSelector("Frente 2", vm.fotoFrente2, new ImageProps().size(100),
                                        () -> vm.escolherFoto(vm.fotoFrente2)),
                                Components.ImageSelector("Costas 1", vm.fotoCostas1, new ImageProps().size(100),
                                        () -> vm.escolherFoto(vm.fotoCostas1)),
                                Components.ImageSelector("Costas 2", vm.fotoCostas2, new ImageProps().size(100),
                                        () -> vm.escolherFoto(vm.fotoCostas2))
                        )
                );
    }

    private Component secaoAcoes() {
        return new Column(new ColumnProps().spacingOf(10))
                .c_child(new SpacerVertical(10))
                .c_child(Components.ButtonCadastro(vm.textoBotaoSalvar(), vm::salvar));
    }
}
