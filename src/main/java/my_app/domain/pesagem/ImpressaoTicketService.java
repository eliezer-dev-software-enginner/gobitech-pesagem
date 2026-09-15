package my_app.domain.pesagem;

import my_app.db.models.EmpresaModel;
import my_app.db.models.PesagemModel;
import my_app.db.services.EmpresaService;
import my_app.db.services.PesagemService;
import my_app.db.services.PreferenciasService;
import my_app.infra.TicketLaserExporter;
import my_app.infra.TicketThermalExporter;

import java.sql.SQLException;

public class ImpressaoTicketService {
    @FunctionalInterface
    public interface ImpressoraTicket {
        boolean imprimir(EmpresaModel empresa, PesagemModel pesagem, PesagemModel entrada);
    }

    private final PreferenciasService preferencias;
    private final PesagemService pesagens;
    private final EmpresaService empresas;
    private final ImpressoraTicket termica;
    private final ImpressoraTicket laser;

    public ImpressaoTicketService(PreferenciasService preferencias, PesagemService pesagens, EmpresaService empresas) {
        this(preferencias, pesagens, empresas,
                new TicketThermalExporter()::imprimir, new TicketLaserExporter()::imprimir);
    }

    public ImpressaoTicketService(PreferenciasService preferencias, PesagemService pesagens,
                                 EmpresaService empresas, ImpressoraTicket termica, ImpressoraTicket laser) {
        this.preferencias = preferencias;
        this.pesagens = pesagens;
        this.empresas = empresas;
        this.termica = termica;
        this.laser = laser;
    }

    public void imprimir(int pesagemId) throws SQLException {
        var tipo = preferencias.buscarTipoImpressao();
        var pesagem = pesagens.buscarComRelacoes(pesagemId);
        if (pesagem == null) throw new IllegalArgumentException("Pesagem não encontrada.");
        var entrada = pesagens.buscarEntradaVinculada(pesagem);
        var empresa = empresas.buscarUnico();
        boolean enviada = switch (tipo) {
            case TERMICA -> termica.imprimir(empresa, pesagem, entrada);
            case LASER -> laser.imprimir(empresa, pesagem, entrada);
        };
        if (!enviada) {
            throw new IllegalArgumentException("Não foi possível enviar o ticket. Verifique a impressora padrão do sistema e o tipo escolhido em Configurações.");
        }
    }
}
