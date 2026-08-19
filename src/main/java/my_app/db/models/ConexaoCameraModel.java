package my_app.db.models;

import lombok.Getter;
import lombok.Setter;
import net.sf.persism.annotations.Column;
import net.sf.persism.annotations.Table;

import java.time.LocalDateTime;

/**
 * Configuração das duas câmeras Intelbras fixas (frente/costas) usadas pra capturar foto
 * automaticamente na hora da pesagem — ver {@code my_app.infra.camera.CameraSnapshotClient}.
 * Singleton (uma linha só), igual {@link ConexaoBalancaModel}: uma balança de pesagem tem uma
 * única instalação física de câmeras, não múltiplas configurações concorrentes.
 */
@Setter
@Getter
@Table("conexao_camera")
public class ConexaoCameraModel {

    @Column(primary = true)
    private Integer id;

    @Column(name = "frente_ip")
    private String frenteIp;

    @Column(name = "frente_porta")
    private Integer frentePorta;

    @Column(name = "frente_canal")
    private Integer frenteCanal;

    @Column(name = "frente_usuario")
    private String frenteUsuario;

    @Column(name = "frente_senha")
    private String frenteSenha;

    @Column(name = "costas_ip")
    private String costasIp;

    @Column(name = "costas_porta")
    private Integer costasPorta;

    @Column(name = "costas_canal")
    private Integer costasCanal;

    @Column(name = "costas_usuario")
    private String costasUsuario;

    @Column(name = "costas_senha")
    private String costasSenha;

    @Column(name = "dataCriacao")
    private LocalDateTime dataCriacao;
}
