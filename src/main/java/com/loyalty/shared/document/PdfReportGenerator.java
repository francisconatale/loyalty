package com.loyalty.shared.document;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
public class PdfReportGenerator {

    public byte[] generateCouponVoucher(String customerName, String couponCode, String discountDescription) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document();

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font codeFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 16);

            document.add(new Paragraph("LOYALTY ENGINE - REWARD VOUCHER", titleFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Issued for: " + customerName, bodyFont));
            document.add(new Paragraph("Benefit: " + discountDescription, bodyFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("COUPON CODE: " + couponCode, codeFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Present this voucher at checkout to claim your reward.", bodyFont));

            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate PDF voucher", e);
        }
    }
}
