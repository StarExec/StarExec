-- Support syntax highlighting of benchmarks

ALTER TABLE processors ADD preserve_input BOOLEAN DEFAULT TRUE;
ALTER TABLE processors ADD time_limit TINYINT DEFAULT 15;
