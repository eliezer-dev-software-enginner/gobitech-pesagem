-- A pesagem de saída referencia a pesagem de entrada que a originou, pra permitir
-- agrupar o par Entrada+Saída num relatório (mesma placa/visita). Coluna opcional:
-- entradas, avulsas e manuais ficam com NULL.
ALTER TABLE pesagens ADD COLUMN entrada_id INTEGER;
