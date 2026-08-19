"""Simula uma balança TCP que só manda o peso quando você mandar — um valor por vez, sob
demanda, em vez de ficar mandando sozinha a cada segundo (ver simular_balanca_tcp.py pra essa
versão automática).

Útil pra testar sequências exatas (ex.: capturar uma Tara com um valor certo, depois um Peso
bruto com outro valor certo, sem o ruído aleatório atrapalhar conferir a conta do peso líquido).

Uso:
    python3 scripts/simular_balanca_tcp_manual.py [porta]

Depois, na tela "Conexão da balança" do app: Tipo TCP, IP 127.0.0.1, Porta (a mesma passada
aqui). Cada linha que você digitar no terminal e confirmar com Enter é mandada como o peso atual
— o app atualiza "Peso na balança agora" com ela. Enter vazio reenvia o último valor mandado
(útil pra simular "não mudou nada, clica Capturar de novo").
"""

import socket
import sys

PORTA = int(sys.argv[1]) if len(sys.argv) > 1 else 5000


def main():
    servidor = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    servidor.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    servidor.bind(("127.0.0.1", PORTA))
    servidor.listen(1)
    print(f"Balança falsa (TCP, manual) escutando em 127.0.0.1:{PORTA}")
    print("Configure a tela 'Conexão da balança' com esse IP/porta e abra a tela de Pesagem.")
    print("Ctrl+C pra parar.\n")

    conn, endereco = servidor.accept()
    print(f"Conectado: {endereco}\n")

    ultimo_peso = "0"
    try:
        with conn:
            while True:
                entrada = input(f"Peso a enviar [{ultimo_peso}]: ").strip()
                peso = entrada if entrada else ultimo_peso
                ultimo_peso = peso
                conn.sendall(f"{peso}\n".encode("utf-8"))
                print(f"  enviado: {peso} kg\n")
    except (BrokenPipeError, ConnectionResetError):
        print("\nApp desconectou (tela de Pesagem fechada/trocada).")


if __name__ == "__main__":
    try:
        main()
    except (KeyboardInterrupt, EOFError):
        print("\nBalança falsa encerrada.")
