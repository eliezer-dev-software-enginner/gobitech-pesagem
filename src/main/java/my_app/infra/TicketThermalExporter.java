package my_app.infra;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.output.PrinterOutputStream;
import my_app.db.models.EmpresaModel;
import my_app.db.models.PesagemModel;
import my_app.db.models.UsuarioModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.print.PrintService;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Imprime o ticket de pesagem em impressora térmica 80mm (ESC/POS), no mesmo layout de campo
 * do ticket do André usado pelo {@link TicketPdfExporter}, porém adaptado pra largura da
 * bobina. Envia direto pra impressora padrão do sistema
 * ({@link PrinterOutputStream#getDefaultPrintService()}), sem salvar PDF — reusa o motor de
 * ESC/POS (escpos-coffee) do app antigo.
 */
public class TicketThermalExporter {

    private static final Logger log = LoggerFactory.getLogger(TicketThermalExporter.class);
    private static final DateTimeFormatter DTH_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    record EstiloLinha(String texto, boolean negrito, boolean fonteDupla) {}

    /**
     * Imprime o ticket da pesagem na impressora térmica padrão do sistema.
     *
     * @param empresa  cabeçalho (pode ser {@code null})
     * @param pesagem  a pesagem (geralmente a saída)
     * @param entrada  entrada vinculada, se houver (data/hora e peso de entrada)
     * @return {@code true} se conseguiu imprimir via ESC/POS
     */
    public boolean imprimir(EmpresaModel empresa, PesagemModel pesagem, PesagemModel entrada) {
        PrintService ps = PrinterOutputStream.getDefaultPrintService();
        if (ps == null) {
            log.warn("Nenhuma impressora padrão disponível para ticket térmico");
            return false;
        }
        try (OutputStream out = new PrinterOutputStream(ps)) {
            try (EscPos escpos = new EscPos(out)) {
                escpos.setCharacterCodeTable(EscPos.CharacterCodeTable.CP860_Portuguese);
                escpos.initializePrinter();
                for (EstiloLinha l : montarLinhas(empresa, pesagem, entrada)) {
                    Style s = new Style().setFontSize(Style.FontSize._1, Style.FontSize._1);
                    if (l.negrito) s = s.setBold(true);
                    if (l.fonteDupla) s = s.setFontSize(Style.FontSize._2, Style.FontSize._2);
                    escpos.writeLF(s, l.texto);
                }
                escpos.feed(4);
                escpos.cut(EscPos.CutMode.FULL);
                escpos.flush();
            }
            log.info("Ticket térmico impresso: pesagemId={} impressora={}", pesagem.getId(), ps.getName());
            return true;
        } catch (Exception e) {
            log.error("Erro ao imprimir ticket térmico: pesagemId={}", pesagem.getId(), e);
            return false;
        }
    }

    /** Monta o ticket como linhas estilizadas (largura da bobina 80mm / 32 colunas). */
    List<EstiloLinha> montarLinhas(EmpresaModel empresa, PesagemModel pesagem, PesagemModel entrada) {
        var linhas = new ArrayList<EstiloLinha>();

        raw(linhas, empresa != null && naoVazio(empresa.getNome()) ? empresa.getNome() : "Gobitech", true, true);
        raw(linhas, "Cnpj: " + (empresa != null ? nulo(empresa.getCpfCnpj()) : ""), false, false);
        raw(linhas, "Insc.est:", false, false);
        raw(linhas, "End: " + (empresa != null ? nulo(montarEnd(empresa)) : ""), false, false);
        raw(linhas, "Bairro: " + (empresa != null ? nulo(empresa.getBairro()) : ""), false, false);
        raw(linhas, "Cidade: " + (empresa != null ? nulo(montarCidade(empresa)) : ""), false, false);
        raw(linhas, "Fone: " + (empresa != null ? nulo(empresa.getTelefone()) : ""), false, false);
        vazio(linhas);

        raw(linhas, "TICKET DE PESAGEM", true, false);
        raw(linhas, "Ticket.......: " + (pesagem.getId() == null ? "" : pesagem.getId()), true, false);
        vazio(linhas);

        raw(linhas, "Placa do Veículo...: " + nulo(pesagem.getPlaca()), false, false);
        raw(linhas, "DT/H Entrada......: " + dth(entrada), false, false);
        raw(linhas, "DT/H Saída........: " + dth(pesagem), false, false);
        raw(linhas, "Operador..........: " + nomeOperador(pesagem), false, false);
        raw(linhas, "Motorista.........: " + valorOu(pesagem.getMotoristaNome(), "---"), false, false);
        raw(linhas, "Produto...........: " + (pesagem.getProduto() != null ? valorOu(pesagem.getProduto().getNome(), "---") : "---"), false, false);
        raw(linhas, "Fornecedor........:", false, false);
        raw(linhas, "Cliente...........: " + (pesagem.getCliente() != null ? valorOu(pesagem.getCliente().getLoja(), "") : ""), false, false);
        vazio(linhas);
        raw(linhas, "Peso de Entrada....: " + peso(entrada != null ? entrada.getPesoTotal() : null) + " Kg", false, false);
        raw(linhas, "Peso de Saída......: " + peso(pesagem.getPesoTotal()) + " Kg", false, false);
        raw(linhas, "Peso Líquido.......: " + peso(pesagem.getPesoFinal()) + " Kg", false, false);
        raw(linhas, "Peso Líquido Final.: " + peso(pesagem.getPesoFinal()), false, false);
        vazio(linhas);
        raw(linhas, "Observação:", false, false);

        if (pesagem.getObservacoes() != null && !pesagem.getObservacoes().isBlank()) {
            for (String obs : quebrar(pesagem.getObservacoes())) {
                raw(linhas, "  " + obs, false, false);
            }
        }
        vazio(linhas);

        raw(linhas, "---------------------------------  --------------------------------", false, false);
        raw(linhas, padEsq("ADMINISTRADOR") + padDir("MOTORISTA"), false, false);

        return linhas;
    }

    private void raw(List<EstiloLinha> linhas, String texto, boolean negrito, boolean fonteDupla) {
        linhas.add(new EstiloLinha(texto != null ? texto : "", negrito, fonteDupla));
    }

    private void vazio(List<EstiloLinha> linhas) {
        linhas.add(new EstiloLinha("", false, false));
    }

    private String dth(PesagemModel p) {
        return p != null && p.getDataCriacao() != null ? p.getDataCriacao().format(DTH_FMT) : "";
    }

    private String nomeOperador(PesagemModel pesagem) {
        UsuarioModel usuario = pesagem.getUsuario();
        if (usuario != null && naoVazio(usuario.getNome())) return usuario.getNome();
        return "---";
    }

    private String peso(BigDecimal valor) {
        return valor == null ? "0" : valor.setScale(0, RoundingMode.HALF_UP).toBigInteger().toString();
    }

    private String montarEnd(EmpresaModel empresa) {
        if (!naoVazio(empresa.getRua())) return null;
        return empresa.getRua() + (naoVazio(empresa.getNumero()) ? ", " + empresa.getNumero() : "");
    }

    private String montarCidade(EmpresaModel empresa) {
        if (!naoVazio(empresa.getCidade())) return null;
        return empresa.getCidade() + (naoVazio(empresa.getEstado()) ? " - " + empresa.getEstado() : "");
    }

    private boolean naoVazio(String v) {
        return v != null && !v.isBlank();
    }

    private String nulo(String v) {
        return (v == null || v.isBlank()) ? "" : v;
    }

    private String valorOu(String valor, String fallback) {
        return naoVazio(valor) ? valor : fallback;
    }

    private String padEsq(String texto) {
        return String.format("%-33s", texto);
    }

    private String padDir(String texto) {
        return String.format("%33s", texto);
    }

    private List<String> quebrar(String texto) {
        var linhas = new ArrayList<String>();
        var atual = new StringBuilder();
        for (String palavra : texto.split("\\s+")) {
            String candidata = atual.isEmpty() ? palavra : atual + " " + palavra;
            if (candidata.length() > 30 && !atual.isEmpty()) {
                linhas.add(atual.toString());
                atual = new StringBuilder(palavra);
            } else {
                atual = new StringBuilder(candidata);
            }
        }
        if (!atual.isEmpty()) linhas.add(atual.toString());
        return linhas;
    }
}
