package my_app.db.models;

import lombok.Getter;
import lombok.Setter;
import my_app.core.Identifier;
import net.sf.persism.annotations.Column;
import net.sf.persism.annotations.Table;

import java.time.LocalDateTime;

@Setter
@Getter
@Table("usuarios")
public class UsuarioModel extends Identifier {

    private String login;
    private String senha;
    private String nome;
    private String telefone;

    private Boolean ativo;
    private Boolean admin;

    @Column(name = "dataCriacao")
    private LocalDateTime dataCriacao;
}
