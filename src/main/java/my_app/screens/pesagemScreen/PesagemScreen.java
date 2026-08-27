package my_app.screens.pesagemScreen;

import javafx.stage.Stage;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.Button;
import megalodonte.components.Card;
import megalodonte.components.LineHorizontal;
import megalodonte.components.SimpleTable;
import megalodonte.components.SpacerVertical;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.FlowRow;
import megalodonte.components.layout_components.Row;
import megalodonte.props.ButtonProps;
import megalodonte.props.ColumnProps;
import megalodonte.props.FlowRowProps;
import megalodonte.props.ImageProps;
import megalodonte.props.RowProps;
import megalodonte.props.SimpleTableProps;
import megalodonte.props.TextProps;
import megalodonte.router.v4.ScreenContext;
import my_app.db.models.PesagemModel;
import my_app.domain.ContratoTelaCrudV3;
import my_app.domain.ViewModelScreenContract;
import my_app.domain.components.Components;
import my_app.infra.CsvExporter;
import my_app.utils.DateUtils;

public class PesagemScreen implements ScreenComponent, ContratoTelaCrudV3<PesagemModel> {
    private final PesagemViewModel vm;
    private final ScreenContext screenContext;

    public PesagemScreen(ScreenContext ctx) {
        this.screenContext = ctx;
        this.vm = new PesagemViewModel(ctx);
    }

    @Override
    public void onMount() {
        vm.fetchListData();
        vm.iniciarLeituraBalanca();
    }

    @Override
    public void onDestroy() {
        vm.pararLeituraBalanca();
        ContratoTelaCrudV3.super.onDestroy();
    }

    @Override
    public Component render() {
        return mainView();
    }

    @Override
    public Component extraListContent() {
        return filtroSection();
    }

    private Component filtroSection() {
        return new Card(
                new Column(new ColumnProps().paddingAll(15))
                        .c_child(Components.FormTitle("Filtrar pesagens"))
                        .c_child(new SpacerVertical(10))
                        .c_child(new FlowRow(new FlowRowProps().spacingOf(10))
                                .children(
                                        Components.InputColumn("Placa", vm.filtroPlaca, "Ex: ABC1D23"),
                                        Components.InputColumn("Motorista", vm.filtroMotorista, "Ex: João"),
                                        Components.DatePickerColumn(vm.filtroDataInicio, "Data início"),
                                        Components.DatePickerColumn(vm.filtroDataFim, "Data fim")
                                )
                        )
                        .c_child(new SpacerVertical(10))
                        .c_child(Components.ButtonCadastro("Filtrar", vm::aplicarFiltro))
        );
    }

    @Override
    public Component form() {
        return new Column(new ColumnProps().spacingOf(15))
                .c_child(new Card(
                        new Column(new ColumnProps().paddingAll(20))
                                .c_child(Components.FormTitle("Registrar pesagem"))
                                .c_child(new SpacerVertical(20))
                                .c_child(new FlowRow(new FlowRowProps().spacingOf(10))
                                        .children(
                                                Components.InputColumn("Placa", vm.placa, "Ex: ABC1D23"),
                                                Components.InputColumn("Nome do motorista", vm.motoristaNome, "Ex: José da Silva"),
                                                Components.InputColumn("Documento do motorista", vm.motoristaDocumento, "CPF/RG"),
                                                Components.SelectColumn("Cliente", vm.clientesState, vm.clienteSelected, c -> c.getLoja(), true),
                                                Components.SelectColumn("Produto", vm.produtosState, vm.produtoSelected, p -> p.getNome(), true),
                                                Components.InputColumn("Nota fiscal", vm.notaFiscal, "Nº da nota (se houver)")
                                        )
                                )
                                .c_child(new SpacerVertical(10))
                                .c_child(new LineHorizontal())
                                .c_child(new SpacerVertical(10))
                                .c_child(Components.FormTitle("Pesos"))
                                .c_child(new SpacerVertical(5))
                                .c_child(Components.TextWithValue("Peso na balança agora (Kg): ", vm.pesoAoVivo))
                                .c_child(new SpacerVertical(10))
                                .c_child(new FlowRow(new FlowRowProps().spacingOf(10))
                                        .children(
                                                Components.InputWithButtonRow("Tara (Kg)", "Ex: 8500", "Capturar", vm.pesoVeiculo, vm::capturarTara),
                                                Components.InputWithButtonRow("Peso bruto (Kg)", "Ex: 32000", "Capturar", vm.pesoTotal, vm::capturarPesoBruto),
                                                Components.InputWithButtonRow("Peso líquido (Kg)", "Ex: 23500", "Calcular", vm.pesoFinal, vm::calcularPesoLiquido)
                                        )
                                )
                                .c_child(new SpacerVertical(10))
                                .c_child(new LineHorizontal())
                                .c_child(new SpacerVertical(10))
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
                                )
                                .c_child(new SpacerVertical(10))
                                .c_child(new LineHorizontal())
                                .c_child(new SpacerVertical(10))
                                .c_child(Components.FormTitle("Fotos"))
                                .c_child(new FlowRow(new FlowRowProps().spacingOf(10))
                                        .children(
                                                Components.ImageSelector("Frente 1", vm.fotoFrente1, new ImageProps().size(100), () -> vm.escolherFoto(vm.fotoFrente1)),
                                                Components.ImageSelector("Frente 2", vm.fotoFrente2, new ImageProps().size(100), () -> vm.escolherFoto(vm.fotoFrente2)),
                                                Components.ImageSelector("Costas 1", vm.fotoCostas1, new ImageProps().size(100), () -> vm.escolherFoto(vm.fotoCostas1)),
                                                Components.ImageSelector("Costas 2", vm.fotoCostas2, new ImageProps().size(100), () -> vm.escolherFoto(vm.fotoCostas2))
                                        )
                                )
                                .c_child(new SpacerVertical(10))
                                .c_child(Components.TextAreaColumn("Observações", vm.observacoes, "Alguma observação sobre a pesagem?", 60, 160))
                                .c_child(new SpacerVertical(20))
                                .c_child(Components.actionButtons(vm.btnText, this::handleAddOrUpdate))
                                .c_child(new SpacerVertical(10))
                                .c_child(new Button("Salvar e baixar ticket", new ButtonProps()
                                        .fillWidth().height(31).fontSize(16)
                                        .textColor("white").bgColor("#16a34a"))
                                        .onClick(this::handleAddOrUpdateEBaixarTicket))
                ));
    }

    /** Mesmo salvamento de {@link #handleAddOrUpdate()}, mas já oferece o PDF do ticket em seguida. */
    private void handleAddOrUpdateEBaixarTicket() {
        try {
            vm.handleAddOrUpdateEBaixarTicket();
            vm.modoEdicaoState().set(false);
        } catch (Exception e) {
            Components.ShowAlertError(e.getMessage());
        }
    }

    @Override
    public ViewModelScreenContract viewModel() {
        return vm;
    }

    @Override
    public SimpleTable<PesagemModel> table() {
        var simpleTable = new SimpleTable<PesagemModel>(new SimpleTableProps().maxHeight(Components.TABLE_MAX_HEIGHT));
        simpleTable.fromData(vm.filteredList)
                .header()
                .columns()
                .column("ID", PesagemModel::getId, 60.0)
                .column("Placa", PesagemModel::getPlaca)
                .column("Motorista", PesagemModel::getMotoristaNome)
                .column("Operação", PesagemModel::getOperacao)
                .column("Cliente", it -> it.getCliente() != null ? it.getCliente().getLoja() : "-")
                .column("Produto", it -> it.getProduto() != null ? it.getProduto().getNome() : "-")
                .column("Peso líquido (Kg)", it -> String.valueOf(it.getPesoFinal()))
                .column("Data", it -> DateUtils.localDateTimeToBrazilianDateTime(it.getDataCriacao()))
                .build()
                .onItemSelectChange(vm.pesagemSelecionada::set)
                .onItemDoubleClick(it -> showItemDetailsComAcoes(it, this.screenContext, 500));

        return simpleTable;
    }

    /**
     * Sobrescreve o padrão do ContratoTelaCrudV3 (Editar/Clonar/Excluir) só pra acrescentar
     * "Imprimir ticket" — ação específica de pesagem, não faz sentido nas outras telas de
     * CRUD que também implementam esse contrato.
     */
    @Override
    public void showItemDetailsComAcoes(PesagemModel model, ScreenContext ctx, int height) {
        Stage[] modalStage = new Stage[1];
        Runnable fechar = () -> {
            if (modalStage[0] != null) modalStage[0].close();
        };

        Component conteudo = new Column(new ColumnProps().fillWidth().spacingOf(15))
                .children(
                        itemDetails(model),
                        new Row(new RowProps().fillWidth().spacingOf(10))
                                .children(
                                        new Button("Imprimir ticket", new ButtonProps().bgColor("#16a34a").textColor("white"))
                                                .onClick(() -> vm.imprimirTicket(model)),
                                        new Button("Editar", new ButtonProps().bgColor("#2563eb").textColor("white"))
                                                .onClick(() -> {
                                                    fechar.run();
                                                    handleClickMenuEdit();
                                                }),
                                        new Button("Clonar", new ButtonProps().bgColor("#6b7280").textColor("white"))
                                                .onClick(() -> {
                                                    fechar.run();
                                                    handleClickMenuClone();
                                                }),
                                        new Button("Excluir", new ButtonProps().bgColor("#ef4444").textColor("white"))
                                                .onClick(() -> {
                                                    fechar.run();
                                                    handleClickMenuDelete();
                                                })
                                )
                );

        modalStage[0] = Components.ShowModal(conteudo, ctx, height);
    }

    @Override
    public void exportCsv(java.io.File destino) throws Exception {
        var headers = java.util.List.of("ID", "Placa", "Motorista", "Operacao", "Cliente", "Produto", "Peso liquido (Kg)", "Data");
        var rows = vm.filteredList.get().stream().map(p -> java.util.List.of(
                String.valueOf(p.getId()),
                p.getPlaca() != null ? p.getPlaca() : "",
                p.getMotoristaNome() != null ? p.getMotoristaNome() : "",
                p.getOperacao() != null ? p.getOperacao() : "",
                p.getCliente() != null ? p.getCliente().getLoja() : "-",
                p.getProduto() != null ? p.getProduto().getNome() : "-",
                String.valueOf(p.getPesoFinal()),
                DateUtils.localDateTimeToBrazilianDateTime(p.getDataCriacao())
        )).toList();
        CsvExporter.exportar(destino, headers, rows);
    }

    public Component itemDetails(PesagemModel model) {
        return new Column(new ColumnProps().paddingAll(20))
                .c_child(new Text("Detalhes da pesagem", new TextProps().fontSize(ThemeManager.theme().typography().subtitle())))
                .c_child(new SpacerVertical(20))
                .c_child(Components.TextWithDetails("ID: ", model.getId()))
                .c_child(Components.TextWithDetails("Placa: ", model.getPlaca()))
                .c_child(Components.TextWithDetails("Motorista: ", model.getMotoristaNome()))
                .c_child(Components.TextWithDetails("Documento do motorista: ", model.getMotoristaDocumento()))
                .c_child(Components.TextWithDetails("Operação: ", model.getOperacao()))
                .c_child(Components.TextWithDetails("Cliente: ", model.getCliente() != null ? model.getCliente().getLoja() : "-"))
                .c_child(Components.TextWithDetails("Produto: ", model.getProduto() != null ? model.getProduto().getNome() : "-"))
                .c_child(Components.TextWithDetails("Nota fiscal: ", model.getNotaFiscal()))
                .c_child(Components.TextWithDetails("Tara: ", model.getPesoVeiculo() + " Kg"))
                .c_child(Components.TextWithDetails("Peso bruto: ", model.getPesoTotal() + " Kg"))
                .c_child(Components.TextWithDetails("Peso líquido: ", model.getPesoFinal() + " Kg"))
                .c_child(Components.TextWithDetails("Data de criação: ", DateUtils.localDateTimeToBrazilianDateTime(model.getDataCriacao())))
                .c_child(Components.TextWithDetails("Observações: ", model.getObservacoes(), true));
    }
}
