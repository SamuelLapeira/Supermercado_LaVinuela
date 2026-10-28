-- V009: búsqueda de texto de productos con el analizador ngram (tolera fragmentos de palabra) y la
-- colación insensible a acentos y mayúsculas. No hay lematización en español: se medirá la relevancia con el
-- catálogo real y, si no basta, la búsqueda está detrás del puerto ProductSearch para poder cambiar de motor.
ALTER TABLE products
  ADD FULLTEXT INDEX ft_products_search (name, short_description) WITH PARSER ngram;
