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
@Table("descontos")
public class DescontoModel extends Identifier {

    private BigDecimal avariados;
    private BigDecimal ardidos;

    @Column(name = "quebra_ardidos")
    private BigDecimal quebraArdidos;

    private BigDecimal impurezas;

    @Column(name = "quebra_impurezas")
    private BigDecimal quebraImpurezas;

    private BigDecimal umidade;

    @Column(name = "quebra_umidade")
    private BigDecimal quebraUmidade;

    private BigDecimal outros;

    @Column(name = "dataCriacao")
    private LocalDateTime dataCriacao;

    public BigDecimal somaPercentuais() {
        return nz(avariados).add(nz(ardidos)).add(nz(quebraArdidos))
                .add(nz(impurezas)).add(nz(quebraImpurezas))
                .add(nz(umidade)).add(nz(quebraUmidade))
                .add(nz(outros));
    }

    private BigDecimal nz(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
