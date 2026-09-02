-- Inscrição estadual da empresa, exibida no cabeçalho de relatórios e tickets ("Insc.est").
-- SQLite não permite ADD COLUMN com NOT NULL sem default nesta migração simples, então é
-- opcional (null) — o cadastro preenche quando quiser.
ALTER TABLE empresas ADD COLUMN inscricao_estadual TEXT;
