"""Simula uma balança conectada via TCP, para testar sem hardware real.

O leitor TCP do app (my_app.infra.balanca.LeitorBalancaTcp) não manda handshake nem comando
nenhum — só abre o socket e fica lendo o que a balança transmitir continuamente. Então simular é
só: abrir uma porta TCP e mandar texto com números periodicamente.

Uso:
    python3 scripts/simular_balanca_tcp.py [porta] [peso_base]

    python3 scripts/simular_balanca_tcp.py            # porta 5000, peso base 32000
    python3 scripts/simular_balanca_tcp.py 5000 8500   # porta 5000, peso base 8500 (ex: tara)

Depois, na tela "Conexão da balança" do app, configure:
    Tipo de conexão: TCP
    Endereço IP: 127.0.0.1
    Porta: (a mesma que você passou aqui, ex: 5000)

O peso "ao vivo" no topo da tela de Pesagem deve começar a atualizar sozinho.
"""

import random
import socket
import sys
import time

PORTA = int(sys.argv[1]) if len(sys.argv) > 1 else 5000
PESO_BASE = float(sys.argv[2]) if len(sys.argv) > 2 else 32000.0


def main():
    servidor = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    servidor.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    servidor.bind(("127.0.0.1", PORTA))
    servidor.listen(1)
    print(f"Balança falsa (TCP) escutando em 127.0.0.1:{PORTA} — peso base {PESO_BASE} kg")
    print("Configure a tela 'Conexão da balança' com esse IP/porta e abra a tela de Pesagem.")
    print("Ctrl+C pra parar.\n")

    while True:
        conn, endereco = servidor.accept()
        print(f"Conectado: {endereco}")
        try:
            with conn:
                while True:
                    # pequeno ruído em volta do peso base, pra imitar uma balança de verdade
                    # "tremendo" um pouco em vez de ficar sempre no valor exato
                    peso = PESO_BASE + random.uniform(-2, 2)
                    linha = f"{peso:.1f}\n"
                    conn.sendall(linha.encode("utf-8"))
                    print(f"  enviado: {linha.strip()} kg", end="\r")
                    time.sleep(1)
        except (BrokenPipeError, ConnectionResetError):
            print("\nApp desconectou (tela de Pesagem fechada/trocada). Esperando nova conexão...")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nBalança falsa encerrada.")
