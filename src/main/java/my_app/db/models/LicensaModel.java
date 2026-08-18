package my_app.db.models;

import lombok.Getter;
import lombok.Setter;
import net.sf.persism.annotations.Column;
import net.sf.persism.annotations.Table;

import java.time.LocalDateTime;

@Setter
@Getter
@Table("licensas")
public class LicensaModel {

    @Column(primary = true)
    private Integer id;

    private String valor;

    @Column(name = "expira_em")
    private LocalDateTime expiraEm;

    @Column(name = "dataCriacao")
    private LocalDateTime dataCriacao;

    public boolean expirada() {
        return expiraEm != null && expiraEm.isBefore(LocalDateTime.now());
    }
}
