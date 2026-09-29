package my_app.db.models;

import lombok.Getter;
import lombok.Setter;
import my_app.core.Identifier;
import net.sf.persism.annotations.Column;
import net.sf.persism.annotations.Table;

import java.time.LocalDateTime;

@Setter
@Getter
@Table("produtos")
public class ProdutoModel extends Identifier {

    private String nome;
    private String unidade;
    private String observacoes;

    private Boolean ativo;

    @Column(name = "dataCriacao")
    private LocalDateTime dataCriacao;
}
