package fr.zenabkissir.chapchap.transaction.service;

import fr.zenabkissir.chapchap.shared.enums.TransactionType;
import fr.zenabkissir.chapchap.transaction.entity.Transaction;
import fr.zenabkissir.chapchap.transaction.repository.TransactionRepository;
import org.openpdf.text.*;
import org.openpdf.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class RecuServiceImpl implements RecuService {

    private static final Color PRIMARY = new Color(30, 58, 95);
    private static final Color ACCENT  = new Color(232, 160, 32);
    private static final Color SUCCESS = new Color(25, 135, 84);
    private static final Color DANGER  = new Color(220, 53, 69);
    private static final Color BG_LIGHT = new Color(248, 250, 252);
    private static final Color BORDER   = new Color(226, 232, 240);
    private static final Color TEXT     = new Color(74, 85, 104);
    private static final Color INK      = new Color(26, 32, 44);
    private static final Color WHITE    = Color.WHITE;

    private static final Color STATUS_CONFIRMED_BG = new Color(209, 250, 229);
    private static final Color STATUS_CONFIRMED_FG = new Color(6, 95, 70);
    private static final Color STATUS_REJECTED_BG  = new Color(254, 226, 226);
    private static final Color STATUS_REJECTED_FG  = new Color(153, 27, 27);
    private static final Color STATUS_PENDING_BG   = new Color(254, 243, 199);
    private static final Color STATUS_PENDING_FG   = new Color(146, 64, 14);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TransactionRepository transactionRepository;

    public RecuServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public byte[] genererRecu(Long transactionId) {
        Transaction t = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction introuvable : " + transactionId));
        return buildPdf(t);
    }

    private byte[] buildPdf(Transaction t) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            doc.add(buildHeader(t));
            doc.add(Chunk.NEWLINE);
            doc.add(buildMontantBlock(t));
            doc.add(Chunk.NEWLINE);
            doc.add(buildInfoTable(t));
            doc.add(Chunk.NEWLINE);
            doc.add(buildFooter(t));

        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du reçu", e);
        } finally {
            if (doc.isOpen()) doc.close();
        }
        return out.toByteArray();
    }

    // ── HEADER ────────────────────────────────────────────────────────────────

    private PdfPTable buildHeader(Transaction t) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{60f, 40f});

        Font brandFont   = font(FontFactory.HELVETICA_BOLD, 22, WHITE);
        Font subFont     = font(FontFactory.HELVETICA, 10, WHITE);
        Font refLblFont  = font(FontFactory.HELVETICA, 8, new Color(180, 200, 220));
        Font refValFont  = font(FontFactory.HELVETICA_BOLD, 13, ACCENT);
        Font dateLblFont = font(FontFactory.HELVETICA, 8, new Color(180, 200, 220));
        Font dateValFont = font(FontFactory.HELVETICA, 10, WHITE);

        PdfPCell left = new PdfPCell();
        left.setBackgroundColor(PRIMARY);
        left.setBorder(Rectangle.NO_BORDER);
        left.setPadding(20);
        left.addElement(new Paragraph("ChapChap", brandFont));
        left.addElement(new Paragraph("Reçu de transaction", subFont));
        table.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBackgroundColor(PRIMARY);
        right.setBorder(Rectangle.NO_BORDER);
        right.setPadding(20);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Paragraph refLbl = new Paragraph("RÉFÉRENCE", refLblFont);
        refLbl.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(refLbl);

        Paragraph refVal = new Paragraph("TXN-" + String.format("%06d", t.getId()), refValFont);
        refVal.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(refVal);

        right.addElement(new Paragraph(" ", font(FontFactory.HELVETICA, 6, WHITE)));

        Paragraph dateLbl = new Paragraph("DATE D'ÉMISSION", dateLblFont);
        dateLbl.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(dateLbl);

        Paragraph dateVal = new Paragraph(LocalDate.now().format(DATE_FMT), dateValFont);
        dateVal.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(dateVal);

        table.addCell(right);
        return table;
    }

    // ── MONTANT ───────────────────────────────────────────────────────────────

    private PdfPTable buildMontantBlock(Transaction t) throws DocumentException {
        boolean entree     = t.getType() == TransactionType.ENTREE;
        Color montantColor = entree ? SUCCESS : DANGER;
        String sign        = entree ? "+" : "−";

        Font typeFont    = font(FontFactory.HELVETICA_BOLD, 10, TEXT);
        Font montantFont = font(FontFactory.HELVETICA_BOLD, 30, montantColor);

        PdfPTable outer = new PdfPTable(1);
        outer.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(BG_LIGHT);
        cell.setBorderColor(BORDER);
        cell.setPadding(22);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph typeLabel = new Paragraph(entree ? "ENTRÉE" : "SORTIE", typeFont);
        typeLabel.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(typeLabel);

        Paragraph montant = new Paragraph(sign + " " + formatMontant(t.getMontant(), t), montantFont);
        montant.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(montant);

        cell.addElement(new Paragraph(" ", font(FontFactory.HELVETICA, 5, WHITE)));
        cell.addElement(buildStatutBadge(t));

        outer.addCell(cell);
        return outer;
    }

    private PdfPTable buildStatutBadge(Transaction t) throws DocumentException {
        String label;
        Color bg, fg;
        switch (t.getStatut()) {
            case CONFIRMEE -> { label = "  CONFIRMÉE  ";  bg = STATUS_CONFIRMED_BG; fg = STATUS_CONFIRMED_FG; }
            case REJETEE   -> { label = "  REJETÉE  ";    bg = STATUS_REJECTED_BG;  fg = STATUS_REJECTED_FG; }
            default        -> { label = "  EN ATTENTE  "; bg = STATUS_PENDING_BG;   fg = STATUS_PENDING_FG; }
        }

        PdfPTable badge = new PdfPTable(1);
        badge.setWidthPercentage(30);
        badge.setHorizontalAlignment(Element.ALIGN_CENTER);

        Font badgeFont = font(FontFactory.HELVETICA_BOLD, 9, fg);
        PdfPCell cell = new PdfPCell(new Phrase(label, badgeFont));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPaddingTop(5);
        cell.setPaddingBottom(5);

        badge.addCell(cell);
        return badge;
    }

    // ── INFO TABLE ────────────────────────────────────────────────────────────

    private PdfPTable buildInfoTable(Transaction t) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{35f, 65f});

        Font labelFont = font(FontFactory.HELVETICA_BOLD, 8, TEXT);
        Font valueFont = font(FontFactory.HELVETICA, 10, INK);

        addSectionTitle(table, "DÉTAILS DE LA TRANSACTION");
        addRow(table, labelFont, valueFont, "Référence",           "TXN-" + String.format("%06d", t.getId()));
        addRow(table, labelFont, valueFont, "Date de transaction",  t.getDate().format(DATE_FMT));
        addRow(table, labelFont, valueFont, "Type",                 t.getType() == TransactionType.ENTREE ? "Entrée" : "Sortie");
        addRow(table, labelFont, valueFont, "Devise",               t.getDevise().getLibelle());
        if (t.getCanal() != null) {
            addRow(table, labelFont, valueFont, "Canal", t.getCanal().getLibelle());
        }
        if (t.getDescription() != null && !t.getDescription().isBlank()) {
            addRow(table, labelFont, valueFont, "Description", t.getDescription());
        }

        addSectionTitle(table, "PERSONNE CONCERNÉE");
        if (t.getPersonne() != null) {
            addRow(table, labelFont, valueFont, "Nom", t.getPersonne().getNom());
            if (t.getPersonne().getTelephone() != null && !t.getPersonne().getTelephone().isBlank()) {
                addRow(table, labelFont, valueFont, "Téléphone", t.getPersonne().getTelephone());
            }
        } else {
            addRow(table, labelFont, valueFont, "Personne", "—");
        }

        return table;
    }

    private void addSectionTitle(PdfPTable table, String title) {
        Font f = font(FontFactory.HELVETICA_BOLD, 8, WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(title, f));
        cell.setColspan(2);
        cell.setBackgroundColor(PRIMARY);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(8);
        cell.setPaddingBottom(8);
        cell.setPaddingLeft(10);
        cell.setPaddingRight(10);
        table.addCell(cell);
    }

    private void addRow(PdfPTable table, Font labelFont, Font valueFont, String label, String value) {
        PdfPCell lbl = new PdfPCell(new Phrase(label.toUpperCase(), labelFont));
        lbl.setBackgroundColor(BG_LIGHT);
        lbl.setBorderColor(BORDER);
        lbl.setPaddingTop(9);
        lbl.setPaddingBottom(9);
        lbl.setPaddingLeft(10);
        lbl.setPaddingRight(10);

        PdfPCell val = new PdfPCell(new Phrase(value, valueFont));
        val.setBorderColor(BORDER);
        val.setPaddingTop(9);
        val.setPaddingBottom(9);
        val.setPaddingLeft(10);
        val.setPaddingRight(10);

        table.addCell(lbl);
        table.addCell(val);
    }

    // ── FOOTER ────────────────────────────────────────────────────────────────

    private Paragraph buildFooter(Transaction t) {
        Font f = font(FontFactory.HELVETICA_OBLIQUE, 8, TEXT);
        Paragraph footer = new Paragraph(
            "Ce reçu constitu la preuve de l'opération référencée TXN-" +
            String.format("%06d", t.getId()) + ".", f);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(8);
        return footer;
    }

    // ── UTILITAIRES ───────────────────────────────────────────────────────────

    private Font font(String name, float size, Color color) {
        return FontFactory.getFont(name, size, color);
    }

    private String formatMontant(BigDecimal montant, Transaction t) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        return df.format(montant) + " " + t.getDevise().getLibelle();
    }
}
