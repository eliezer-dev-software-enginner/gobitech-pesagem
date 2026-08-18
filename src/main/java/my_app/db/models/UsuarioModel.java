package my_app.db.models;

import lombok.Getter;
import lombok.Setter;
import net.sf.persism.annotations.Column;
import net.sf.persism.annotations.Table;

import java.time.LocalDateTime;

@Setter
@Getter
@Table("usuarios")
public class UsuarioModel {

    @Column(primary = true)
    private Integer id;

    private String login;
    private String senha;
    private String nome;
    private String telefone;

    private Boolean ativo;
    private Boolean admin;

    @Column(name = "dataCriacao")
    private LocalDateTime dataCriacao;
}
