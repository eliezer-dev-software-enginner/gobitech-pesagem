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
@Table("produtos")
public class ProdutoModel extends Identifier {

    private String nome;
    private String unidade;
    private String observacoes;

    // Desconto padrão do produto (previsto no DER original, nunca implementado) — carregado
    // no campo "Outros" da Pesagem ao selecionar o produto, editável por pesagem.
    private BigDecimal desconto;

    private Boolean ativo;

    @Column(name = "dataCriacao")
    private LocalDateTime dataCriacao;
}
