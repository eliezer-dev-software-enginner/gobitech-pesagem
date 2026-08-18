package my_app.db.models;

import lombok.Getter;
import lombok.Setter;
import net.sf.persism.annotations.Column;
import net.sf.persism.annotations.Table;

import java.time.LocalDateTime;

@Setter
@Getter
@Table("conexao_balanca")
public class ConexaoBalancaModel {

    @Column(primary = true)
    private Integer id;

    @Column(name = "tipo_conexao")
    private String tipoConexao; // "Serial" ou "TCP"

    @Column(name = "porta_com")
    private String portaCom;

    @Column(name = "baud_rate")
    private Integer baudRate;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "ip_port")
    private Integer ipPort;

    @Column(name = "dataCriacao")
    private LocalDateTime dataCriacao;
}
