package my_app.infra;

import my_app.db.models.ClienteModel;
import my_app.db.models.EmpresaModel;
import my_app.db.models.PesagemModel;
import my_app.db.models.ProdutoModel;
import my_app.db.models.UsuarioModel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TicketThermalExporterTest {

    private final TicketThermalExporter exporter = new TicketThermalExporter();

    private PesagemModel pesagemBasica() {
        var pesagem = new PesagemModel();
        pesagem.setId(3);
        pesagem.setPlaca("RED1234");
        pesagem.setMotoristaNome("JOÃO");
        pesagem.setTipoPesagem("saida");
        pesagem.setPesoVeiculo(new BigDecimal("2540.00"));
        pesagem.setPesoTotal(new BigDecimal("9980.00"));
        pesagem.setPesoFinal(new BigDecimal("7440.00"));
        pesagem.setObservacoes("Observação de teste");
        pesagem.setDataCriacao(LocalDateTime.of(2025, 8, 17, 14, 15, 56));

        var usuario = new UsuarioModel();
        usuario.setNome("ADMINISTRADOR");
        pesagem.setUsuario(usuario);

        var produto = new ProdutoModel();
        produto.setNome("SOJA");
        pesagem.setProduto(produto);

        var cliente = new ClienteModel();
        cliente.setLoja("CHÃOS EIRELI");
        pesagem.setCliente(cliente);

        return pesagem;
    }

    private PesagemModel entradaBasica() {
        var entrada = new PesagemModel();
        entrada.setId(1);
        entrada.setPesoTotal(new BigDecimal("2540.00"));
        entrada.setDataCriacao(LocalDateTime.of(2025, 8, 17, 14, 15, 7));
        return entrada;
    }

    @Test
    void montaLinhasNoFormatoDoAndre() {
        var empresa = new EmpresaModel();
        empresa.setNome("BALANÇAS GOBITECH");
        empresa.setCpfCnpj("12345678000199");
        empresa.setInscricaoEstadual("10.123.456-7");
        empresa.setCidade("FORMOSA");
        empresa.setEstado("GO");
        empresa.setTelefone("61-99653-2857");

        var linhas = exporter.montarLinhas(empresa, pesagemBasica(), entradaBasica());

        var textos = linhas.stream().map(TicketThermalExporterTest::texto).toList();
        String tudo = String.join("\n", textos);

        assertTrue(tudo.contains("BALANÇAS GOBITECH"));
        assertTrue(tudo.contains("Cnpj: 12345678000199"));
        assertTrue(tudo.contains("Insc.est: 10.123.456-7"));
        assertTrue(tudo.contains("Cidade: FORMOSA - GO"));
        assertTrue(tudo.contains("TICKET DE PESAGEM"));
        assertTrue(tudo.contains("Ticket.......: 3"));
        assertTrue(tudo.contains("Placa do Veículo...: RED1234"));
        assertTrue(tudo.contains("DT/H Entrada......: 17/08/2025 14:15:07"));
        assertTrue(tudo.contains("DT/H Saída........: 17/08/2025 14:15:56"));
        assertTrue(tudo.contains("Operador..........: ADMINISTRADOR"));
        assertTrue(tudo.contains("Motorista.........: JOÃO"));
        assertTrue(tudo.contains("Produto...........: SOJA"));
        assertTrue(tudo.contains("Cliente...........: CHÃOS EIRELI"));
        assertTrue(tudo.contains("Peso de Entrada....: 2540 Kg"));
        assertTrue(tudo.contains("Peso de Saída......: 9980 Kg"));
        assertTrue(tudo.contains("Peso Líquido Inicial: 7440 Kg"));
        assertTrue(tudo.contains("Observação:"));
        assertTrue(tudo.contains("Observação de teste"));
        assertTrue(tudo.contains("ADMINISTRADOR"));
        assertTrue(tudo.contains("MOTORISTA"));

        // Cabeçalho em fonte dupla + negrito
        assertTrue(linhas.get(0).fonteDupla());
        assertTrue(linhas.get(0).negrito());
    }

    @Test
    void funcionaSemEmpresaSemEntradaESemRelacoes() {
        var pesagem = pesagemBasica();
        pesagem.setMotoristaNome(null);
        pesagem.setProduto(null);
        pesagem.setCliente(null);
        pesagem.setObservacoes(null);

        var linhas = exporter.montarLinhas(null, pesagem, null);
        var textos = linhas.stream().map(TicketThermalExporterTest::texto).toList();
        String tudo = String.join("\n", textos);

        assertTrue(tudo.contains("Gobitech"));
        assertTrue(tudo.contains("RED1234"));
        assertTrue(tudo.contains("Motorista.........: ---"));
        assertTrue(tudo.contains("Produto...........: ---"));
        assertTrue(tudo.contains("Operador..........: ADMINISTRADOR"));
    }

    private static String texto(TicketThermalExporter.EstiloLinha linha) {
        return linha.texto();
    }

    @Test
    void imprimeTaraDaEntradaEDescontosSemFornecedor() {
        var p = pesagemBasica();
        p.setPesoVeiculo(new BigDecimal("8500"));
        p.setPesoTotal(new BigDecimal("32000"));
        p.setPesoFinal(new BigDecimal("22325"));
        var d = new my_app.db.models.DescontoModel();
        d.setQuebraArdidos(new BigDecimal("2"));
        d.setQuebraImpurezas(new BigDecimal("3"));
        d.setArdidos(new BigDecimal("7"));
        d.setImpurezas(new BigDecimal("8"));
        p.setDesconto(d);
        var entrada = entradaBasica();
        entrada.setPesoTotal(BigDecimal.ZERO);
        entrada.setPesoVeiculo(new BigDecimal("8500"));
        var texto = String.join("\n", exporter.montarLinhas(null, p, entrada).stream()
                .map(TicketThermalExporter.EstiloLinha::texto).toList());
        assertFalse(texto.contains("Fornecedor"));
        assertTrue(texto.contains("Peso de Entrada....: 8500 Kg"));
        assertTrue(texto.contains("Quebra ardidos\n  2% / 470 Kg"));
        assertTrue(texto.contains("Quebra impurezas\n  3% / 705 Kg"));
        assertTrue(texto.contains("Total descontado: 1175 Kg"));
        assertTrue(texto.contains("Peso Líquido Final.: 22325 Kg"));
    }

    @Test
    void descontosSoloExibicaoMostramPercentualSemKgNaTermica() {
        var p = pesagemBasica();
        p.setPesoVeiculo(new BigDecimal("8500"));
        p.setPesoTotal(new BigDecimal("32000"));
        p.setPesoFinal(new BigDecimal("23500"));
        var d = new my_app.db.models.DescontoModel();
        d.setImpurezas(new BigDecimal("20"));
        d.setUmidade(new BigDecimal("10"));
        p.setDesconto(d);
        var texto = String.join("\n", exporter.montarLinhas(null, p, null).stream()
                .map(TicketThermalExporter.EstiloLinha::texto).toList());
        assertTrue(texto.contains("Impurezas\n  20% / -"));
        assertTrue(texto.contains("Umidade\n  10% / -"));
        assertTrue(texto.contains("Total descontado: -"));
        assertFalse(texto.contains("Avariados"));
        assertFalse(texto.contains("Quebra ardidos"));
        assertFalse(texto.contains("Outros"));
    }

    @Test
    void assinaturasComLinhaAcimaDeCadaNome() {
        var linhas = exporter.montarLinhas(null, pesagemBasica(), null);
        var textos = linhas.stream().map(TicketThermalExporterTest::texto).toList();

        String linhaSublinhado = textos.get(textos.size() - 2);
        String linhaNomes = textos.get(textos.size() - 1);

        assertTrue(linhaSublinhado.startsWith("_".repeat("ADMINISTRADOR".length())),
                "Linha de sublinhado deve começar abaixo do ADMINISTRADOR: " + linhaSublinhado);
        assertTrue(linhaSublinhado.trim().endsWith("_".repeat("MOTORISTA".length())),
                "Linha de sublinhado deve terminar no MOTORISTA: " + linhaSublinhado);
        assertTrue(linhaNomes.startsWith("ADMINISTRADOR"), "Nome esquerdo deve iniciar a linha");
        assertTrue(linhaNomes.endsWith("MOTORISTA"), "Nome direito deve terminar a linha");
        assertTrue(linhaNomes.indexOf("MOTORISTA") > linhaNomes.indexOf("ADMINISTRADOR") + "ADMINISTRADOR".length(),
                "Nomes devem sair espaçados: " + linhaNomes);
        assertTrue(linhaNomes.length() <= 32, "Linha de nomes deve caber na bobina: " + linhaNomes);
    }

    @Test
    void soImprimeDescontosAplicados() {
        var p = pesagemBasica();
        p.setPesoVeiculo(new BigDecimal("8500"));
        p.setPesoTotal(new BigDecimal("32000"));
        p.setPesoFinal(new BigDecimal("22325"));
        var d = new my_app.db.models.DescontoModel();
        d.setQuebraUmidade(new BigDecimal("5"));
        p.setDesconto(d);
        var texto = String.join("\n", exporter.montarLinhas(null, p, null).stream()
                .map(TicketThermalExporter.EstiloLinha::texto).toList());
        assertTrue(texto.contains("Quebra umidade\n  5% / 1175 Kg"));
        assertTrue(texto.contains("Total descontado: 1175 Kg"));
        assertFalse(texto.contains("Avariados"));
        assertFalse(texto.contains("Ardidos"));
        assertFalse(texto.contains("Impurezas"));
        assertFalse(texto.contains("Outros"));
        assertFalse(texto.contains("Quebra ardidos"));
        assertFalse(texto.contains("Quebra impurezas"));
        assertFalse(texto.contains("Umidade"));
    }

    @Test
    void semDescontosImprimeMensagemEmVezDaLista() {
        var texto = String.join("\n", exporter.montarLinhas(null, pesagemBasica(), null).stream()
                .map(TicketThermalExporter.EstiloLinha::texto).toList());
        assertTrue(texto.contains("DESCONTOS"));
        assertTrue(texto.contains("Nenhum desconto aplicado."));
        assertFalse(texto.contains("Avariados"));
        assertFalse(texto.contains("Total descontado"));
    }
}
