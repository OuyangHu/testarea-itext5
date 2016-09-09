package mkl.testarea.itext5.meta;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfReader;

/**
 * @author mkl
 */
public class OpenFile
{
    /**
     * <a href="http://stackoverflow.com/questions/39284508/itext-pdfreader-not-able-to-read-pdf">
     * ITEXT PDFReader not able to read PDF
     * </a>
     * <br/>
     * <a href="http://www.fundslibrary.co.uk/FundsLibrary.DataRetrieval/Documents.aspx?type=fund_class_kiid&id=f096b13b-3d0e-4580-8d3d-87cf4d002650&user=fidelitydocumentreport">
     * Aviva_Investors_UK_Fund_Services_Limited_UK_Equity_Income_Class_2_Income_[GBP].pdf
     * </a>
     * <p>
     * Indeed, iText fails to open the PDF in question. Further analysis (cf. the answer to the question) shows
     * that the file is not properly encrypted.
     * </p>
     */
    @Test
    public void testOpenAviva_Investors_UK_Fund_Services_Limited_UK_Equity_Income_Class_2_Income_GBP() throws IOException
    {
        try ( InputStream resource = getClass().getResourceAsStream("Aviva_Investors_UK_Fund_Services_Limited_UK_Equity_Income_Class_2_Income_[GBP].pdf") )
        {
            PdfReader pdfReader = new PdfReader(resource);
            Assert.assertEquals("", "Key Investor Information", pdfReader.getInfo().get(PdfName.TITLE));
        }
    }
}