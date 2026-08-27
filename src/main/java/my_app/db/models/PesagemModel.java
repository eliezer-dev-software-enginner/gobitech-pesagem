package my_app.db.models;

import lombok.Getter;
import lombok.Setter;
import my_app.core.Identifier;
import net.sf.persism.annotations.Column;
import net.sf.persism.annotations.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@Table("pesagens")
public class PesagemModel extends Identifier {

    @Column(name = "motorista_nome")
    private String motoristaNome;

    @Column(name = "motorista_documento")
    private String motoristaDocumento;

    private String placa;
    private String operacao;

    @Column(name = "nota_fiscal")
    private String notaFiscal;

    private String observacoes;

    @Column(name = "peso_veiculo")
    private BigDecimal pesoVeiculo;

    @Column(name = "peso_total")
    private BigDecimal pesoTotal;

    @Column(name = "peso_final")
    private BigDecimal pesoFinal;

    @Column(name = "foto_frente_1")
    private String fotoFrente1;

    @Column(name = "foto_frente_2")
    private String fotoFrente2;

    @Column(name = "foto_costas_1")
    private String fotoCostas1;

    @Column(name = "foto_costas_2")
    private String fotoCostas2;

    @Column(name = "cliente_id")
    private Integer clienteId;

    @Column(name = "produto_id")
    private Integer produtoId;

    @Column(name = "desconto_id")
    private Integer descontoId;

    @Column(name = "dataCriacao")
    private LocalDateTime dataCriacao;

    // relações resolvidas em runtime pelo Service, não pelo Persism
    private transient ClienteModel cliente;
    private transient ProdutoModel produto;
    private transient DescontoModel desconto;
}
