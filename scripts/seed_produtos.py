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


# A tela de Produtos só aceita 3 unidades (my_app.domain.Data.unidadesDeMedidaList):
# "Quilos", "Toneladas", "Gramas" — nada de "sc"/"ton"/"m³", que não existem no dropdown do app.
QUILOS = "Quilos"
TONELADAS = "Toneladas"

# (nome, unidade, observacoes, desconto)
PRODUTOS = [
    ("Soja", TONELADAS, "Grão a granel", 0),
    ("Milho", TONELADAS, "Grão a granel", 0),
    ("Trigo", TONELADAS, "Grão a granel", 0),
    ("Sorgo", TONELADAS, "Grão a granel", 0),
    ("Algodão em caroço", TONELADAS, "Fibra + caroço", 2.5),
    ("Arroz em casca", TONELADAS, "Grão a granel", 0),
    ("Feijão", TONELADAS, "Grão a granel", 0),
    ("Farelo de Soja", TONELADAS, "Subproduto do esmagamento da soja", 0),
    ("Farelo de Trigo", TONELADAS, "Subproduto da moagem do trigo", 0),
    ("Adubo/Fertilizante", QUILOS, "Carga a granel", 3.0),
    ("Calcário", TONELADAS, "Corretivo de solo, carga a granel", 0),
    ("Ração Animal", QUILOS, "Ensacada ou a granel", 0),
    ("Areia", TONELADAS, "Material de construção, carga a granel", 0),
    ("Brita", TONELADAS, "Material de construção, carga a granel", 0),
    ("Cana-de-açúcar", TONELADAS, "Carga a granel", 5.0),
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
