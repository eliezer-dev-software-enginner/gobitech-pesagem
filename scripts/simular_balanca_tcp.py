"""Simula uma balança conectada via TCP, para testar sem hardware real.

O leitor TCP do app (my_app.infra.balanca.LeitorBalancaTcp) não manda handshake nem comando
nenhum — só abre o socket e fica lendo o que a balança transmitir continuamente. Então simular é
só: abrir uma porta TCP e mandar texto com números periodicamente.

O peso não fica parado em volta de um único valor — ele **sobe** de um piso (pensado pra virar
Tara, capturada logo no início da conexão) até um teto (pensado pra virar Peso bruto, capturado
depois de esperar um pouco), ao longo de alguns segundos, e depois se estabiliza no teto. Assim,
capturar Tara cedo e Peso bruto mais tarde sempre dá bruto > tara — sem o risco de um ruído
aleatório fazer uma leitura posterior vir menor que uma anterior (o que geraria peso líquido
negativo, sem representar nenhum cenário real).

Uso:
    python3 scripts/simular_balanca_tcp.py [porta] [peso_tara] [peso_bruto] [duracao_rampa_seg]

    python3 scripts/simular_balanca_tcp.py                    # porta 5000, 8500 -> 32000 em 20s
    python3 scripts/simular_balanca_tcp.py 5000 8500 32000 20  # os mesmos valores, explícitos

Depois, na tela "Conexão da balança" do app, configure:
    Tipo de conexão: TCP
    Endereço IP: 127.0.0.1
    Porta: (a mesma que você passou aqui, ex: 5000)

O peso "ao vivo" no topo da tela de Pesagem deve começar a atualizar sozinho. Capture a Tara
assim que conectar (ainda perto do piso) e o Peso bruto depois de esperar a rampa terminar (perto
do teto) — a contagem da rampa reinicia a cada nova conexão (cada vez que abre a tela de
Pesagem), então dá pra repetir o teste quantas vezes quiser sem reiniciar o script.
"""

import random
import socket
import sys
import time

PORTA = int(sys.argv[1]) if len(sys.argv) > 1 else 5000
PESO_TARA = float(sys.argv[2]) if len(sys.argv) > 2 else 8500.0
PESO_BRUTO = float(sys.argv[3]) if len(sys.argv) > 3 else 32000.0
DURACAO_RAMPA = float(sys.argv[4]) if len(sys.argv) > 4 else 20.0

# Ruído bem menor que a distância entre tara e bruto, pra nunca "furar" o piso/teto de um jeito
# que atrapalhe a ordem — só dá aquele tremidinho de balança de verdade.
RUIDO = 1.0


def main():
    servidor = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    servidor.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    servidor.bind(("127.0.0.1", PORTA))
    servidor.listen(1)
    print(f"Balança falsa (TCP) escutando em 127.0.0.1:{PORTA}")
    print(f"Peso sobe de {PESO_TARA} kg (capture como Tara logo no início) até {PESO_BRUTO} kg "
          f"em {DURACAO_RAMPA:.0f}s (capture como Peso bruto depois de esperar) — bruto sempre > tara.")
    print("Configure a tela 'Conexão da balança' com esse IP/porta e abra a tela de Pesagem.")
    print("Ctrl+C pra parar.\n")

    while True:
        conn, endereco = servidor.accept()
        print(f"Conectado: {endereco} — rampa reiniciada")
        inicio = time.monotonic()
        try:
            with conn:
                while True:
                    decorrido = time.monotonic() - inicio
                    progresso = min(decorrido / DURACAO_RAMPA, 1.0)
                    base = PESO_TARA + (PESO_BRUTO - PESO_TARA) * progresso
                    peso = base + random.uniform(-RUIDO, RUIDO)
                    linha = f"{peso:.1f}\n"
                    conn.sendall(linha.encode("utf-8"))
                    print(f"  enviado: {linha.strip()} kg (rampa {progresso * 100:.0f}%)", end="\r")
                    time.sleep(1)
        except (BrokenPipeError, ConnectionResetError):
            print("\nApp desconectou (tela de Pesagem fechada/trocada). Esperando nova conexão...")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nBalança falsa encerrada.")
