package my_app.screens.pesagemScreen;

import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.router.v4.ScreenContext;
import my_app.db.models.PesagemModel;
import my_app.domain.components.Components;
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
        motoristaNome.set(entrada.getMotoristaNome() == null ? "" : entrada.getMotoristaNome());
        motoristaDocumento.set(entrada.getMotoristaDocumento() == null ? "" : entrada.getMotoristaDocumento());
        notaFiscal.set(entrada.getNotaFiscal() == null ? "" : entrada.getNotaFiscal());

        if (entrada.getPesoVeiculo() != null) {
            pesoVeiculo.set(arrInt(entrada.getPesoVeiculo()));
        }

        if (entrada.getClienteId() != null) {
            clientesState.get().stream()
                    .filter(c -> c.getId().equals(entrada.getClienteId()))
                    .findFirst()
                    .ifPresent(clienteSelected::set);
        }
        if (entrada.getProdutoId() != null) {
            produtosState.get().stream()
                    .filter(p -> p.getId().equals(entrada.getProdutoId()))
                    .findFirst()
                    .ifPresent(produtoSelected::set);
        }
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
