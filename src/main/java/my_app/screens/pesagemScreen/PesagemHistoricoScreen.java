package my_app.screens.pesagemScreen;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.components.Card;
import megalodonte.components.SimpleTable;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.FlowRow;
import megalodonte.components.layout_components.Row;
import megalodonte.components.layout_components.Stack;
import megalodonte.props.CardProps;
import megalodonte.props.ColumnProps;
import megalodonte.props.FlowRowProps;
import megalodonte.props.RowProps;
import megalodonte.props.SimpleTableProps;
import megalodonte.base.route.v2.ScreenContextInterface;
import my_app.core.AppRoutes;
import my_app.db.models.EmpresaModel;
import my_app.db.models.PesagemModel;
import my_app.domain.ContratoTelaCrudV3;
import my_app.domain.ViewModelScreenContract;
import my_app.domain.components.Components;
import my_app.infra.RelatorioPesagemPdfExporter;
import pack.utilities.DatePack;
import org.kordamp.ikonli.entypo.Entypo;

import java.io.File;
import java.util.List;
import static my_app.domain.pesagem.RelatorioPesagemDados.pesoStr;

/**
 * Histórico de pesagens — a única tela de pesagem que é listagem CRUD
 * ({@code ContratoTelaCrudV3}). Mostra todas as pesagens registradas, com filtro, busca,
 * detalhes (duplo-clique), exclusão e exportação em PDF. Não cria nem edita: as 4 formas
 * de registro são as telas de formulário acessadas pela sidebar.
 */
public class PesagemHistoricoScreen implements ScreenComponent, ContratoTelaCrudV3<PesagemModel> {

    private final PesagemHistoricoViewModel vm;
    private final ScreenContextInterface screenContext;

    public PesagemHistoricoScreen(ScreenContextInterface ctx) {
        this.screenContext = ctx;
        this.vm = new PesagemHistoricoViewModel(ctx);
    }

    @Override
    public void onMount() {
        vm.fetchListData();
    }

    @Override
    public void onDestroy() {
        ContratoTelaCrudV3.super.onDestroy();
    }

    @Override
    public Component render() {
        return mainView();
    }

    @Override
    public String downloadListaPrefixo() {
        return "relatório";
    }

    @Override
    public ViewModelScreenContract<PesagemModel> viewModel() {
        return vm;
    }

    @Override
    public Component extraListContent() {
        return filtroSection();
    }

    private Component filtroSection() {
        return new Card(
                new Column(new ColumnProps().paddingAll(15))
                        .c_child(Components.FormTitle("Filtrar pesagens"))
                        .c_child(new FlowRow(new FlowRowProps().spacingOf(10))
                                .children(
                                        Components.InputColumn("Placa", vm.filtroPlaca, "Ex: ABC1D23"),
                                        Components.InputColumn("Motorista", vm.filtroMotorista, "Ex: João"),
                                        Components.SelectColumn("Tipo", PesagemHistoricoViewModel.tiposPesagemOpcoes, vm.filtroTipo, it -> it),
                                        Components.SelectColumn("Cliente", vm.clientesFiltroState, vm.filtroCliente,
                                                c -> c.getId() == null ? "Todos" : c.getLoja(), true),
                                        Components.DatePickerColumn(vm.filtroDataInicio, "Data início"),
                                        Components.DatePickerColumn(vm.filtroDataFim, "Data fim")
                                )
                        )
                        .c_child(Components.ButtonCadastro("Filtrar", vm::aplicarFiltro))
        );
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
                .column("Tipo", PesagemModel::getTipoPesagem)
                .column("Cliente", it -> it.getCliente() != null ? it.getCliente().getLoja() : "-")
                .column("Produto", it -> it.getProduto() != null ? it.getProduto().getNome() : "-")
                .column("Peso líquido (Kg)", it -> pesoStr(it.getPesoFinal()))
                .column("Data", it -> DatePack.localDateTimeToBrazilianDateTime(it.getDataCriacao()))
                .build()
                .onItemSelectChange(vm.selected::set)
                //.onItemDoubleClick(it -> showItemDetailsComAcoes(it, this.screenContext, 500));
                .onItemDoubleClick(it -> screenContext.spawnWindow(AppRoutes.Screens.DETAILS_PESAGEM.name() + "/" + it.getId()));

        return simpleTable;
    }


    /**
     * Sobrescreve o layout padrão do contrato pra trocar a barra de ações flutuante: mostra
     * "Baixar lista" e "Excluir", sem "Criar novo"/"Editar" (as pesagens são criadas nas telas
     * de formulário, não aqui).
     */
    @Override
    public Component mainView() {
        var conteudo = Components.ScrollPaneDefault(
                new Column(new ColumnProps().fillWidth().spacingOf(15))
                        .children(
                                extraListContent(),
                                new Card(
                                        new Column(new ColumnProps().fillWidth().spacingOf(15))
                                                .children(
                                                        Components.searchInput(vm.searchState, "Pesquisar"),
                                                        table()
                                                ),
                                        new CardProps().fillWidth().paddingAll(20).bgColor("#ffffff")
                                )
                        )
        );

        return new Stack()
                .children(conteudo)
                .childInCorner(acoesLista(), Stack.Corner.BOTTOM_RIGHT, 20)
                .fillHeight();
    }

    private Row acoesLista() {
        return new Row(new RowProps().spacingOf(10).hugWidth()).children(
                Components.actionButton("Imprimir", "white", "#16a34a", Entypo.PRINT,  ()-> {
                   var m = vm.selected.get();
                   if(m == null){
                       Components.ShowAlertError("Selecione o item primeiro");
                       return;
                   }
                   vm.imprimirTicket(m);
                }),
                //botaoAcao("Exportar relatório", "black", "#CDD7D6", Entypo.DOWNLOAD, this::handleClickBaixarLista),
                //botaoAcao("Exportar relatório", "black", "#CDD7D6", Entypo.DOWNLOAD, this::handleClickBaixarLista),
                Components.actionButton("Exportar relatório", "black", "#CDD7D6", Entypo.DOWNLOAD, this::handleClickBaixarLista),
                Components.actionButton("Excluir", "white", "#E55934", Entypo.TRASH, this::handleClickMenuDelete)
        );
    }

    @Override
    public void exportPdf(File destino, EmpresaModel empresa, List<PesagemModel> snapshotFiltrado) throws Exception {
        var idsEntrada = snapshotFiltrado.stream()
                .filter(p -> "saida".equals(p.getTipoPesagem()))
                .map(PesagemModel::getEntradaId).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        var idsNoFiltro = snapshotFiltrado.stream().map(PesagemModel::getId).toList();
        idsEntrada.removeAll(idsNoFiltro);
        List<PesagemModel> entradas;
        try (var service = new my_app.db.services.PesagemService()) {
            entradas = service.buscarComRelacoesPorIds(idsEntrada);
        }
        var dados = my_app.domain.pesagem.RelatorioPesagemDados.montar(snapshotFiltrado, entradas);
        var headers = List.of("Ticket", "Tara (Kg)", "Entrada", "Horário", "Saída", "Horário",
                "Placa", "Produto", "Cliente", "Peso bruto", "Peso líquido");
        RelatorioPesagemPdfExporter.exportar(destino, empresa, "Relatório resumo de entradas e saídas",
                headers, dados.linhas(), dados.observacoes(), dados.linhas().size(),
                dados.totalLiquido().stripTrailingZeros().toPlainString());
    }
}
