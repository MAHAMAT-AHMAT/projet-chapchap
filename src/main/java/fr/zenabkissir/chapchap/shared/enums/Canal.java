package fr.zenabkissir.chapchap.shared.enums;

public enum Canal {
    VIREMENT("Virement bancaire"),
    ESPECES("Espèces"),
    AUTRE("Autre");

    private final String libelle;

    Canal(String libelle) { this.libelle = libelle; }

    public String getLibelle() { return libelle; }
}
