package my_app.screens.pesagemScreen;

import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.base.route.v2.ScreenContextInterface;
import my_app.db.models.PesagemModel;
import my_app.domain.components.Components;
import my_app.domain.pesagem.PesagemRegras;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pesagem de saída: o operador digita a placa e o formulário puxa os dados da última pesagem
 * de entrada daquele caminhão (motorista, documento, cliente, produto e a Tara) — sem precisar
 * redigitar nem repesar vazio. Resta confirmar o peso bruto (balança) e os descontos.
 * Ao alterar a placa, descarta imediatamente o vínculo e os dados herdados da entrada anterior;
 * respostas de buscas anteriores não podem preencher o formulário novamente.
 */
public class PesagemSaidaViewModel extends PesagemFormViewModel {

    private static final Logger log = LoggerFactory.getLogger(PesagemSaidaViewModel.class);

    private Integer entradaIdVinculada;
    private long versaoBusca;

    public PesagemSaidaViewModel(ScreenContextInterface ctx) {
        super(ctx);

        // A consulta ao banco é assíncrona; a versão distingue buscas sucessivas mesmo
        // quando o operador volta a digitar a mesma placa.
        placa.subscribe(valor -> {
            versaoBusca++;
            if (entradaIdVinculada != null) {
                entradaIdVinculada = null;
                limparDadosDaEntrada();
            }
            puxarDadosDaEntrada(valor, versaoBusca);
        });
    }

    private void puxarDadosDaEntrada(String placaValue, long versao) {
        if (placaValue == null || placaValue.trim().isEmpty()) return;
        var placaAtual = placaValue.trim();

        Async.Run(() -> {
            try {
                var entrada = pesagemService.buscarUltimaEntrada(placaAtual);
                UI.runOnUi(() -> {
                    if (versao != versaoBusca || !placa.get().trim().equals(placaAtual)) return;
                    if (entrada == null) {
                        Components.ShowPopup(ctx, "Nenhuma pesagem de entrada encontrada para essa placa");
                        return;
                    }
                    entradaIdVinculada = entrada.getId();
                    preencherDaEntrada(entrada);
                });
            } catch (Exception e) {
                log.error("Erro ao buscar a entrada da placa {}", placaAtual, e);
                UI.runOnUi(() -> {
                    if (versao == versaoBusca) Components.ShowAlertError("Não foi possível buscar a entrada. Tente novamente.");
                });
            }
        });
    }

    private void limparDadosDaEntrada() {
        motoristaNome.set("");
        motoristaDocumento.set("");
        notaFiscal.set("");
        pesoVeiculo.set("");
        pesoTotal.set("");
        clienteSelected.set(null);
        produtoSelected.set(null);
    }

    private void preencherDaEntrada(PesagemModel entrada) {
        var dados = PesagemRegras.preencherDaEntrada(entrada, clientesState.get(), produtosState.get());
        motoristaNome.set(dados.motoristaNome());
        motoristaDocumento.set(dados.motoristaDocumento());
        notaFiscal.set(dados.notaFiscal());
        pesoVeiculo.set(dados.pesoVeiculo());
        pesoTotal.set(dados.pesoTotal());
        if (dados.cliente() != null) clienteSelected.set(dados.cliente());
        if (dados.produto() != null) produtoSelected.set(dados.produto());
    }

    @Override
    protected String tipoPesagem() {
        return "saida";
    }

    @Override
    protected void aoMontarModel(PesagemModel model) {
        model.setEntradaId(entradaIdVinculada);
    }

    @Override
    protected String tituloFormulario() {
        return "Registrar pesagem de saída";
    }
}
