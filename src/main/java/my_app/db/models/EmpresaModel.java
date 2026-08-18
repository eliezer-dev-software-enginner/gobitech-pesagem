package my_app.db.models;

import lombok.Getter;
import lombok.Setter;
import net.sf.persism.annotations.Column;
import net.sf.persism.annotations.Table;

import java.time.LocalDateTime;

@Setter
@Getter
@Table("empresas")
public class EmpresaModel {

    @Column(primary = true)
    private Integer id;

    private String nome;
    private String cpfCnpj;
    private String telefone;
    private String email;

    @Column(name = "endereco_cep")
    private String cep;

    @Column(name = "endereco_cidade")
    private String cidade;

    @Column(name = "endereco_estado")
    private String estado;

    @Column(name = "endereco_bairro")
    private String bairro;

    @Column(name = "endereco_rua")
    private String rua;

    @Column(name = "endereco_numero")
    private String numero;

    private String logomarca;

    @Column(name = "dataCriacao")
    private LocalDateTime dataCriacao;
}
