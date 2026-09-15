ALTER TABLE preferencias ADD COLUMN tipo_impressao TEXT NOT NULL DEFAULT 'laser'
    CHECK (tipo_impressao IN ('laser', 'termica'));
