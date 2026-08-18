"""Popula a tabela `produtos` do banco local com produtos de exemplo.

Uso:
    python scripts/seed_produtos.py

Usa o mesmo caminho de banco que o app resolve em runtime (DB.resolveDbPath()):
~/.gobitech/erp.db no Linux, %APPDATA%/gobitech/erp.db no Windows.
"""

import os
import sqlite3
from datetime import datetime, timezone
from pathlib import Path


def resolve_db_path() -> Path:
    if os.name == "nt":
        base = Path(os.environ["APPDATA"]) / "gobitech"
    else:
        base = Path.home() / ".gobitech"
    base.mkdir(parents=True, exist_ok=True)
    return base / "erp.db"


# (nome, unidade, observacoes, desconto)
PRODUTOS = [
    ("Soja", "sc", "Grão a granel", 0),
    ("Milho", "sc", "Grão a granel", 0),
    ("Trigo", "sc", "Grão a granel", 0),
    ("Sorgo", "sc", "Grão a granel", 0),
    ("Algodão em caroço", "ton", "Fibra + caroço", 2.5),
    ("Arroz em casca", "sc", "Grão a granel", 0),
    ("Feijão", "sc", "Grão a granel", 0),
    ("Farelo de Soja", "ton", "Subproduto do esmagamento da soja", 0),
    ("Farelo de Trigo", "ton", "Subproduto da moagem do trigo", 0),
    ("Adubo/Fertilizante", "ton", "Carga a granel", 3.0),
    ("Calcário", "ton", "Corretivo de solo, carga a granel", 0),
    ("Ração Animal", "ton", "Ensacada ou a granel", 0),
    ("Areia", "m³", "Material de construção, carga a granel", 0),
    ("Brita", "m³", "Material de construção, carga a granel", 0),
    ("Cana-de-açúcar", "ton", "Carga a granel", 5.0),
]


def main():
    db_path = resolve_db_path()
    print(f"Banco: {db_path}")

    conn = sqlite3.connect(db_path)
    try:
        cur = conn.cursor()
        agora = datetime.now(timezone.utc).astimezone().strftime("%Y-%m-%d %H:%M:%S")

        inseridos = 0
        ignorados = 0
        for nome, unidade, observacoes, desconto in PRODUTOS:
            cur.execute(
                """
                INSERT OR IGNORE INTO produtos (nome, unidade, observacoes, ativo, dataCriacao, desconto)
                VALUES (?, ?, ?, 1, ?, ?)
                """,
                (nome, unidade, observacoes, agora, desconto),
            )
            if cur.rowcount:
                inseridos += 1
                print(f"  + {nome}")
            else:
                ignorados += 1
                print(f"  = {nome} (já existia, ignorado)")

        conn.commit()
        print(f"\n{inseridos} produto(s) inserido(s), {ignorados} já existente(s).")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
