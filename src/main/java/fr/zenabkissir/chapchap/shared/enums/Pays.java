package fr.zenabkissir.chapchap.shared.enums;

public enum Pays {
    TCHAD("Tchad"),
    MAROC("Maroc");

    private final String libelle;

    Pays(String libelle) { this.libelle = libelle; }

    public String getLibelle() { return libelle; }
}