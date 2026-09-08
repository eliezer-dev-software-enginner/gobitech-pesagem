package my_app.screens.pesagemScreen;

import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.router.v4.ScreenContext;
import my_app.db.models.PesagemModel;
import my_app.domain.components.Components;
import my_app.domain.pesagem.PesagemRegras;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pesagem de saída: o operador digita a placa e o formulário puxa os dados da última pesagem
 * de entrada daquele caminhão (motorista, documento, cliente, produto e a Tara) — sem precisar
 * redigitar nem repesar vazio. Resta confirmar o peso bruto (balança) e os descontos.
 */
public class PesagemSaidaViewModel extends PesagemFormViewModel {

    private static final Logger log = LoggerFactory.getLogger(PesagemSaidaViewModel.class);

    private Integer entradaIdVinculada;

    public PesagemSaidaViewModel(ScreenContext ctx) {
        super(ctx);

        // Digitar a placa dispara a busca pela última Entrada daquele caminhão. Assíncrono
        // (vai no banco); quando o resultado chega, confere se a placa ainda é a mesma antes
        // de preencher (evita uma resposta atrasada sobrescrever o que já é relevante).
        placa.subscribe(valor -> puxarDadosDaEntrada(valor));
    }

    private void puxarDadosDaEntrada(String placaValue) {
        if (placaValue == null || placaValue.trim().isEmpty()) return;
        var placaAtual = placaValue.trim();

        Async.Run(() -> {
            try {
                var entrada = pesagemService.buscarUltimaEntrada(placaAtual);
                UI.runOnUi(() -> {
                    if (!placa.get().trim().equals(placaAtual)) return; // placa já mudou
                    if (entrada == null) {
                        entradaIdVinculada = null;
                        Components.ShowPopup(ctx, "Nenhuma pesagem de entrada encontrada para essa placa");
                        return;
                    }
                    entradaIdVinculada = entrada.getId();
                    preencherDaEntrada(entrada);
                });
            } catch (Exception e) {
                log.error("Erro ao buscar a entrada da placa {}", placaAtual, e);
            }
        });
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
