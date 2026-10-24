package com.carland.carland_service.idda;

/**
 * tr: Mark metninin marka ve modele ayrılmış hali. Ham metin ayrıca saklanır.
 * en: Brand and model taken from the Mark text. The raw text is stored separately.
 */
public record IddaMarkSplit(String brand, String model) {
}
