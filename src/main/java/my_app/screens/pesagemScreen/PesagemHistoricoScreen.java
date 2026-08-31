package my_app.screens.pesagemScreen;

import javafx.stage.Stage;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.Button;
import megalodonte.components.Card;
import megalodonte.components.SimpleTable;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Container;
import megalodonte.components.layout_components.FlowRow;
import megalodonte.components.layout_components.Row;
import megalodonte.components.layout_components.Stack;
import megalodonte.props.ButtonProps;
import megalodonte.props.CardProps;
import megalodonte.props.ColumnProps;
import megalodonte.props.ContainerProps;
import megalodonte.props.FlowRowProps;
import megalodonte.props.RowProps;
import megalodonte.props.SimpleTableProps;
import megalodonte.props.TextProps;
import megalodonte.router.v4.ScreenContext;
import megalodonte.v2.Show;
import my_app.db.models.EmpresaModel;
import my_app.db.models.PesagemModel;
import my_app.domain.ContratoTelaCrudV3;
import my_app.domain.ViewModelScreenContract;
import my_app.domain.components.Components;
import my_app.infra.ListaPdfExporter;
import my_app.infra.RelatorioPesagemPdfExporter;
import my_app.utils.DateUtils;
import org.kordamp.ikonli.entypo.Entypo;

import java.io.File;
import java.util.List;

/**
 * Histórico de pesagens — a única tela de pesagem que é listagem CRUD
 * ({@code ContratoTelaCrudV3}). Mostra todas as pesagens registradas, com filtro, busca,
 * detalhes (duplo-clique), exclusão e exportação em PDF. Não cria nem edita: as 4 formas
 * de registro são as telas de formulário acessadas pela sidebar.
 */
public class PesagemHistoricoScreen implements ScreenComponent, ContratoTelaCrudV3<PesagemModel> {

    private final PesagemHistoricoViewModel vm;
    private final ScreenContext screenContext;

    public PesagemHistoricoScreen(ScreenContext ctx) {
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
    public ViewModelScreenContract viewModel() {
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
                .column("Peso líquido (Kg)", it -> String.valueOf(it.getPesoFinal()))
                .column("Data", it -> DateUtils.localDateTimeToBrazilianDateTime(it.getDataCriacao()))
                .build()
                .onItemSelectChange(vm.selected::set)
                .onItemDoubleClick(it -> showItemDetailsComAcoes(it, this.screenContext, 500));

        return simpleTable;
    }

    /**
     * O histórico não tem "Criar novo"/"Editar" (formulários são telas à parte) — só o
     * "Imprimir ticket" (daqui da lista) e "Excluir".
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
                                        new Button("Excluir", new ButtonProps().bgColor("#ef4444").textColor("white"))
                                                .onClick(() -> {
                                                    fechar.run();
                                                    handleClickMenuDelete();
                                                })
                                )
                );

        modalStage[0] = Components.ShowModal(conteudo, ctx, height);
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
                botaoAcao("Exportar relatório", "black", "#CDD7D6", Entypo.DOWNLOAD, this::handleClickBaixarLista),
                botaoAcao("Excluir", "white", "#E55934", Entypo.TRASH, this::handleClickMenuDelete)
        );
    }

    private Button botaoAcao(String title, String color, String bgColor, org.kordamp.ikonli.Ikon ikon,
                             megalodonte.base.async.RunnableThrowing onclick) {
        return new Button(title, new ButtonProps()
                .bgColor(bgColor)
                .textColor(color))
                .onClick(onclick)
                .icon(Components.ikon(ikon, 10, color));
    }

    // Formulário inline não se aplica ao histórico — mas o contrato pede o método.
    @Override
    @Deprecated(forRemoval = true)
    public Component form() {
        return new Column();
    }

    @Override
    @Deprecated(forRemoval = true)
    public Component itemDetails(PesagemModel model) {
        return new Column(new ColumnProps().paddingAll(20))
                .c_child(new megalodonte.components.Text("Detalhes da pesagem", new TextProps().fontSize(ThemeManager.theme().typography().subtitle())))
                .c_child(new megalodonte.components.SpacerVertical(20))
                .c_child(Components.TextWithDetails("ID: ", model.getId()))
                .c_child(Components.TextWithDetails("Placa: ", model.getPlaca()))
                .c_child(Components.TextWithDetails("Motorista: ", model.getMotoristaNome()))
                .c_child(Components.TextWithDetails("Documento do motorista: ", model.getMotoristaDocumento()))
                .c_child(Components.TextWithDetails("Tipo: ", model.getTipoPesagem()))
                .c_child(Components.TextWithDetails("Cliente: ", model.getCliente() != null ? model.getCliente().getLoja() : "-"))
                .c_child(Components.TextWithDetails("Produto: ", model.getProduto() != null ? model.getProduto().getNome() : "-"))
                .c_child(Components.TextWithDetails("Nota fiscal: ", model.getNotaFiscal()))
                .c_child(Components.TextWithDetails("Tara: ", model.getPesoVeiculo() + " Kg"))
                .c_child(Components.TextWithDetails("Peso bruto: ", model.getPesoTotal() + " Kg"))
                .c_child(Components.TextWithDetails("Peso líquido: ", model.getPesoFinal() + " Kg"))
                .c_child(Components.TextWithDetails("Data de criação: ", DateUtils.localDateTimeToBrazilianDateTime(model.getDataCriacao())))
                .c_child(Components.TextWithDetails("Observações: ", model.getObservacoes(), true));
    }

    @Override
    public void exportPdf(File destino, EmpresaModel empresa, List<PesagemModel> snapshotFiltrado) throws Exception {
        // Uma linha por par Entrada+Saída (mesma placa/visita), igual ao relatório do André.
        // Tara = peso veículo da entrada; bruto/líquido do registro consolidado (a saída, quando
        // há par). Avulsas/manuais e saídas sem a entrada no snapshot entram como linha própria
        // na coluna Entrada (são eventos únicos) — nada some do relatório.
        var porId = new java.util.HashMap<Integer, PesagemModel>();
        for (var p : snapshotFiltrado) porId.put(p.getId(), p);

        var consumidas = new java.util.HashSet<Integer>();
        var rows = new java.util.ArrayList<List<String>>();

        var saidas = snapshotFiltrado.stream()
                .filter(p -> "saida".equals(p.getTipoPesagem()))
                .sorted(java.util.Comparator.comparing(PesagemModel::getDataCriacao))
                .toList();

        for (var saida : saidas) {
            var entradaId = saida.getEntradaId();
            var entrada = entradaId != null ? porId.get(entradaId) : null;
            if (entrada != null && "entrada".equals(entrada.getTipoPesagem()) && !consumidas.contains(entradaId)) {
                consumidas.add(entradaId);
                consumidas.add(saida.getId());
                rows.add(linhaPar(entrada, saida));
            } else {
                consumidas.add(saida.getId());
                rows.add(linhaEventoUnico(saida));
            }
        }

        for (var p : snapshotFiltrado) {
            if (consumidas.contains(p.getId())) continue;
            rows.add(linhaEventoUnico(p));
        }

        var totalLiquido = rows.isEmpty() ? java.math.BigDecimal.ZERO
                : rows.stream()
                .map(r -> r.get(10))
                .map(s -> s.isBlank() ? java.math.BigDecimal.ZERO : new java.math.BigDecimal(s))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        var observacoes = snapshotFiltrado.stream()
                .map(PesagemModel::getObservacoes)
                .filter(o -> o != null && !o.isBlank())
                .distinct()
                .reduce((a, b) -> a + "; " + b)
                .orElse("----");

        var rodape = List.of(
                "Observação (todas as linhas): " + observacoes,
                "Quantidade total entradas: " + rows.size(),
                "Total peso líquido: " + totalLiquido.stripTrailingZeros().toPlainString()
        );

        var headers = List.of("Ticket", "Tara (Kg)", "Entrada", "Horário", "Saída", "Horário",
                "Placa", "Produto", "Cliente", "Peso bruto", "Peso líquido");
        RelatorioPesagemPdfExporter.exportar(destino, "Relatório resumo de entradas e saídas", headers, rows, rodape);
    }

    private List<String> linhaPar(PesagemModel entrada, PesagemModel saida) {
        return List.of(
                String.valueOf(entrada.getId()),
                pesoStr(entrada.getPesoVeiculo()),
                dataHora(entrada.getDataCriacao(), false),
                dataHora(entrada.getDataCriacao(), true),
                dataHora(saida.getDataCriacao(), false),
                dataHora(saida.getDataCriacao(), true),
                entrada.getPlaca() != null ? entrada.getPlaca() : "",
                entrada.getProduto() != null ? entrada.getProduto().getNome() : "---",
                entrada.getCliente() != null ? entrada.getCliente().getLoja() : "",
                pesoStr(saida.getPesoTotal()),
                pesoStr(saida.getPesoFinal())
        );
    }

    private List<String> linhaEventoUnico(PesagemModel p) {
        return List.of(
                String.valueOf(p.getId()),
                pesoStr(p.getPesoVeiculo()),
                dataHora(p.getDataCriacao(), false),
                dataHora(p.getDataCriacao(), true),
                "", "",
                p.getPlaca() != null ? p.getPlaca() : "",
                p.getProduto() != null ? p.getProduto().getNome() : "---",
                p.getCliente() != null ? p.getCliente().getLoja() : "",
                pesoStr(p.getPesoTotal()),
                pesoStr(p.getPesoFinal())
        );
    }

    /**
     * Formata um valor de peso (Tara/Peso bruto/Peso líquido) como número inteiro, sem casas
     * decimais — o relatório não deve exibir frações de Kg, e valores de ponto flutuante
     * (ex: 31999.900390625, resultado de imprecisão de double) nunca devem ir pro PDF como
     * estão, porque inflam a largura das colunas e derrubam o layout da tabela.
     */
    private String pesoStr(Number valor) {
        if (valor == null) return "0";
        return String.valueOf(Math.round(valor.doubleValue()));
    }

    /** data (dd/MM/yyyy) ou hora (HH:mm:ss) de um LocalDateTime. */
    private String dataHora(java.time.LocalDateTime dataHora, boolean soHora) {
        if (dataHora == null) return "";
        if (soHora) return dataHora.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        return dataHora.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}