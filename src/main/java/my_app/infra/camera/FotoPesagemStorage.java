package my_app.infra.camera;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Salva os JPEGs capturados das câmeras em disco — diferente da foto escolhida manualmente
 * (FileChooser, {@code PesagemViewModel.escolherFoto}), que só guarda a URI do arquivo original
 * onde o usuário já tinha a imagem, uma foto de câmera não tem "arquivo original" nenhum: são
 * bytes crus vindos de uma resposta HTTP, precisam de um lugar próprio pra existir.
 */
public class FotoPesagemStorage {

    private FotoPesagemStorage() {
    }

    private static Path pastaFotos() {
        return Paths.get(System.getProperty("user.home"), ".gobitech", "fotos");
    }

    /** @return URI do arquivo salvo, no mesmo formato que {@code PesagemModel.fotoX} já espera (compatível com o componente {@code Image}). */
    public static String salvar(byte[] jpegBytes, String nomeArquivo) throws IOException {
        var pasta = pastaFotos();
        Files.createDirectories(pasta);
        var arquivo = pasta.resolve(nomeArquivo);
        Files.write(arquivo, jpegBytes);
        return arquivo.toUri().toString();
    }
}
