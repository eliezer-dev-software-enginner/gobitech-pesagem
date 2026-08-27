package my_app.db.models;

import lombok.Getter;
import lombok.Setter;
import my_app.core.Identifier;
import net.sf.persism.annotations.Column;
import net.sf.persism.annotations.Table;

import java.time.LocalDateTime;

@Setter
@Getter
@Table("preferencias")
public class PreferenciasModel extends Identifier {

    @Column(name = "dataCriacao")
    private LocalDateTime dataCriacao;

    @Column(name = "primeiro_acesso")
    private Integer primeiroAcesso;

    public boolean isFirstAccess() {
        return primeiroAcesso != null && primeiroAcesso == 1;
    }
}
