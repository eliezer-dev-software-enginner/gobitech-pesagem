package my_app.db.models;

import lombok.Getter;
import lombok.Setter;
import my_app.domain.components.Components;
import net.sf.persism.annotations.Column;
import net.sf.persism.annotations.Table;

import java.time.LocalDateTime;

@Setter
@Getter
@Table("clientes")
public class ClienteModel {

    @Column(primary = true)
    private Integer id;

    private String loja;

    @Column(name = "razao_social")
    private String razaoSocial;

    private String cpfCnpj;
    private String telefone;

    private String cep;
    private String uf;
    private String cidade;
    private String bairro;
    private String rua;
    private String numero;
    private String complemento;

    private Boolean ativo;

    @Column(name = "dataCriacao")
    private LocalDateTime dataCriacao;

    public Components.Endereco getEndereco() {
        return new Components.Endereco(uf, cep, cidade, bairro, rua, numero);
    }
}
