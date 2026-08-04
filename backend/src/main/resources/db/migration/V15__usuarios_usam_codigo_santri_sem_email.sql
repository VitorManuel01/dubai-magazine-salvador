ALTER TABLE usuarios
    CHANGE COLUMN login codigo_santri VARCHAR(255) NOT NULL;

ALTER TABLE usuarios
    DROP COLUMN email;
