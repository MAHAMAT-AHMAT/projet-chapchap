package fr.zenabkissir.chapchap.transaction.service;

import fr.zenabkissir.chapchap.shared.enums.Devise;
import fr.zenabkissir.chapchap.shared.enums.Pays;
import fr.zenabkissir.chapchap.shared.enums.TransactionStatus;
import fr.zenabkissir.chapchap.shared.enums.TransactionType;
import fr.zenabkissir.chapchap.transaction.entity.Transaction;
import org.openpdf.text.*;
import org.openpdf.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class FactureServiceImpl implements FactureService {

    private static final Color PRIMARY  = new Color(30, 58, 95);
    private static final Color SUCCESS  = new Color(25, 135, 84);
    private static final Color DANGER   = new Color(220, 53, 69);
    private static final Color BG_LIGHT = new Color(248, 250, 252);
    private static final Color BORDER   = new Color(226, 232, 240);
    private static final Color TEXT     = new Color(74, 85, 104);
    private static final Color INK      = new Color(26, 32, 44);
    private static final Color WHITE    = Color.WHITE;

    private static final Color STATUS_CONFIRMED_FG = new Color(6, 95, 70);
    private static final Color STATUS_REJECTED_FG  = new Color(153, 27, 27);
    private static final Color STATUS_PENDING_FG   = new Color(146, 64, 14);

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public byte[] genererFacture(List<Transaction> transactions, Pays pays,
                                  LocalDate dateDebut, LocalDate dateFin) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            doc.add(buildHeader(pays, dateDebut, dateFin, transactions.size()));
            doc.add(Chunk.NEWLINE);
            doc.add(buildStatsBlock(transactions, pays));
            doc.add(Chunk.NEWLINE);

            if (transactions.isEmpty()) {
                Font f = font(FontFactory.HELVETICA_OBLIQUE, 11, TEXT);
                Paragraph empty = new Paragraph("Aucune transaction pour les critères sélectionnés.", f);
                empty.setAlignment(Element.ALIGN_CENTER);
                doc.add(empty);
            } else {
                doc.add(buildTransactionTable(transactions));
            }

            doc.add(Chunk.NEWLINE);
            doc.add(buildFooter(transactions.size()));

        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération de la facture", e);
        } finally {
            if (doc.isOpen()) doc.close();
        }
        return out.toByteArray();
    }

    // ── HEADER ────────────────────────────────────────────────────────────────

    private PdfPTable buildHeader(Pays pays, LocalDate dateDebut, LocalDate dateFin,
                                   int count) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{60f, 40f});

        Font brandFont = font(FontFactory.HELVETICA_BOLD, 22, WHITE);
        Font subFont   = font(FontFactory.HELVETICA, 10, WHITE);

        PdfPCell left = new PdfPCell();
        left.setBackgroundColor(PRIMARY);
        left.setBorder(Rectangle.NO_BORDER);
        left.setPadding(20);
        left.addElement(new Paragraph("ChapChap", brandFont));
        String titre = pays != null
                ? "Rapport de transactions — " + pays.getLibelle()
                : "Rapport de transactions";
        left.addElement(new Paragraph(titre, subFont));
        table.addCell(left);

        Font secFont = font(FontFactory.HELVETICA, 8, new Color(180, 200, 220));
        Font valFont = font(FontFactory.HELVETICA_BOLD, 11, WHITE);
        Font txtFont = font(FontFactory.HELVETICA, 10, WHITE);

        PdfPCell right = new PdfPCell();
        right.setBackgroundColor(PRIMARY);
        right.setBorder(Rectangle.NO_BORDER);
        right.setPadding(20);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Paragraph periodLbl = new Paragraph("PÉRIODE", secFont);
        periodLbl.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(periodLbl);

        String periode;
        if (dateDebut != null && dateFin != null) {
            periode = dateDebut.format(DATE_FMT) + " — " + dateFin.format(DATE_FMT);
        } else if (dateDebut != null) {
            periode = "Depuis le " + dateDebut.format(DATE_FMT);
        } else if (dateFin != null) {
            periode = "Jusqu'au " + dateFin.format(DATE_FMT);
        } else {
            periode = "Toutes les dates";
        }

        Paragraph periodeVal = new Paragraph(periode, txtFont);
        periodeVal.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(periodeVal);

        right.addElement(new Paragraph(" ", font(FontFactory.HELVETICA, 5, WHITE)));

        Paragraph countLbl = new Paragraph("TRANSACTIONS", secFont);
        countLbl.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(countLbl);

        Paragraph countVal = new Paragraph(String.valueOf(count), valFont);
        countVal.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(countVal);

        table.addCell(right);
        return table;
    }

    // ── STATS ─────────────────────────────────────────────────────────────────

    private PdfPTable buildStatsBlock(List<Transaction> transactions,
                                       Pays pays) throws DocumentException {
        boolean showMAD = pays == null || pays == Pays.MAROC;
        boolean showXAF = pays == null || pays == Pays.TCHAD;

        int cols = (showMAD && showXAF) ? 2 : 1;
        PdfPTable outer = new PdfPTable(cols);
        outer.setWidthPercentage(100);

        if (showMAD) outer.addCell(wrapInCell(buildDeviseStats(transactions, Devise.MAD)));
        if (showXAF) outer.addCell(wrapInCell(buildDeviseStats(transactions, Devise.XAF)));

        return outer;
    }

    private PdfPTable buildDeviseStats(List<Transaction> transactions,
                                        Devise devise) throws DocumentException {
        BigDecimal entrees = sumConfirmees(transactions, TransactionType.ENTREE, devise);
        BigDecimal sorties = sumConfirmees(transactions, TransactionType.SORTIE, devise);
        BigDecimal solde   = entrees.subtract(sorties);
        boolean positive   = solde.compareTo(BigDecimal.ZERO) >= 0;

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{33f, 33f, 34f});

        Font titleFont  = font(FontFactory.HELVETICA_BOLD, 9, WHITE);
        Font lblFont    = font(FontFactory.HELVETICA, 8, TEXT);
        Font entreeFont = font(FontFactory.HELVETICA_BOLD, 8.5f, SUCCESS);
        Font sortieFont = font(FontFactory.HELVETICA_BOLD, 8.5f, DANGER);
        Font soldeFont  = font(FontFactory.HELVETICA_BOLD, 8.5f, positive ? SUCCESS : DANGER);

        // Titre de la section devise
        PdfPCell title = new PdfPCell(new Phrase(devise.getLibelle() + " — résumé (confirmées)", titleFont));
        title.setColspan(3);
        title.setBackgroundColor(PRIMARY);
        title.setBorder(Rectangle.NO_BORDER);
        title.setPaddingTop(7);
        title.setPaddingBottom(7);
        title.setPaddingLeft(10);
        table.addCell(title);

        table.addCell(statCell("Entrées", fmtNoBreak(entrees, devise), entreeFont, lblFont));
        table.addCell(statCell("Sorties", fmtNoBreak(sorties, devise), sortieFont, lblFont));
        table.addCell(statCell("Solde", (positive ? "+" : "") + fmtNoBreak(solde, devise), soldeFont, lblFont));

        return table;
    }

    private PdfPCell statCell(String label, String value, Font valFont, Font lblFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(BG_LIGHT);
        cell.setBorderColor(BORDER);
        cell.setPaddingTop(10);
        cell.setPaddingBottom(10);
        cell.setPaddingLeft(3);
        cell.setPaddingRight(3);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setNoWrap(true);

        Paragraph lbl = new Paragraph(label.toUpperCase(), lblFont);
        lbl.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(lbl);

        Paragraph val = new Paragraph(value, valFont);
        val.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(val);

        return cell;
    }

    private PdfPCell wrapInCell(PdfPTable inner) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3);
        cell.addElement(inner);
        return cell;
    }

    // ── TABLEAU TRANSACTIONS ──────────────────────────────────────────────────

    private PdfPTable buildTransactionTable(List<Transaction> transactions) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{14f, 10f, 22f, 30f, 24f});
        table.setHeaderRows(1);
        table.setSpacingBefore(4);

        Font headerFont = font(FontFactory.HELVETICA_BOLD, 8, WHITE);
        for (String col : new String[]{"DATE", "TYPE", "MONTANT", "PERSONNE", "STATUT"}) {
            PdfPCell h = new PdfPCell(new Phrase(col, headerFont));
            h.setBackgroundColor(PRIMARY);
            h.setBorder(Rectangle.NO_BORDER);
            h.setPaddingTop(8);
            h.setPaddingBottom(8);
            h.setPaddingLeft(6);
            h.setPaddingRight(6);
            table.addCell(h);
        }

        int row = 0;
        for (Transaction t : transactions) {
            Color bg = (row % 2 == 0) ? WHITE : BG_LIGHT;
            boolean entree = t.getType() == TransactionType.ENTREE;

            table.addCell(rowCell(t.getDate().format(DATE_FMT),
                    font(FontFactory.HELVETICA, 8, INK), bg));

            table.addCell(rowCell(entree ? "Entrée" : "Sortie",
                    font(FontFactory.HELVETICA_BOLD, 8, entree ? SUCCESS : DANGER), bg));

            String sign = entree ? "+" : "−";
            table.addCell(rowCell(sign + " " + fmt(t.getMontant(), t.getDevise()),
                    font(FontFactory.HELVETICA_BOLD, 9, entree ? SUCCESS : DANGER), bg));

            String personne = t.getPersonne() != null ? t.getPersonne().getNom() : "—";
            table.addCell(rowCell(personne, font(FontFactory.HELVETICA, 9, INK), bg));

            String statutLabel;
            Color statutColor;
            switch (t.getStatut()) {
                case CONFIRMEE -> { statutLabel = "Confirmée";  statutColor = STATUS_CONFIRMED_FG; }
                case REJETEE   -> { statutLabel = "Rejetée";    statutColor = STATUS_REJECTED_FG; }
                default        -> { statutLabel = "En attente"; statutColor = STATUS_PENDING_FG; }
            }
            table.addCell(rowCell(statutLabel,
                    font(FontFactory.HELVETICA_BOLD, 8, statutColor), bg));

            row++;
        }

        return table;
    }

    private PdfPCell rowCell(String text, Font f, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, f));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(BORDER);
        cell.setPaddingTop(7);
        cell.setPaddingBottom(7);
        cell.setPaddingLeft(6);
        cell.setPaddingRight(6);
        return cell;
    }

    // ── FOOTER ────────────────────────────────────────────────────────────────

    private Paragraph buildFooter(int count) {
        Font f = font(FontFactory.HELVETICA_OBLIQUE, 8, TEXT);
        Paragraph footer = new Paragraph(
            "Rapport du " +
            LocalDateTime.now().format(DATETIME_FMT) +
            " — " + count + " transaction(s) incluse(s).", f);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(8);
        return footer;
    }

    // ── UTILITAIRES ───────────────────────────────────────────────────────────

    private Font font(String name, float size, Color color) {
        return FontFactory.getFont(name, size, color);
    }

    private BigDecimal sumConfirmees(List<Transaction> list, TransactionType type, Devise devise) {
        return list.stream()
                .filter(t -> t.getStatut() == TransactionStatus.CONFIRMEE
                          && t.getType() == type
                          && t.getDevise() == devise)
                .map(Transaction::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String fmtNoBreak(BigDecimal montant, Devise devise) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('\u00A0');
        symbols.setDecimalSeparator(',');

        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);

        return df.format(montant) + "\u00A0" + devise.getLibelle();
    }
    private String fmt(BigDecimal montant, Devise devise) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        return df.format(montant) + " " + devise.getLibelle();
    }
}
