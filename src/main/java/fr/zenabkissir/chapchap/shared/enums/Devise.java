package fr.zenabkissir.chapchap.shared.enums;

public enum Devise {
    MAD("MAD"),
    XAF("FCFA");

    private final String libelle;

    Devise(String libelle) { this.libelle = libelle; }

    public String getLibelle() { return libelle; }
}
