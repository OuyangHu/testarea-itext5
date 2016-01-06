package mkl.testarea.signature.analyze;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerId;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenInfo;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Store;

/**
 * This class is meant to eventually become a tool for analyzing signatures.
 * More and more tests shall be added to indicate the issues of the upcoming
 * test signatures.
 * 
 * @author mklink
 */
public class SignatureAnalyzer
{
    private DigestCalculatorProvider digCalcProvider = new BcDigestCalculatorProvider();
    /**
     * @throws IOException 
     * @throws TSPException 
     * @throws OperatorCreationException 
     * 
     */
    public SignatureAnalyzer(byte[] signatureData) throws CMSException, IOException, TSPException, OperatorCreationException
    {
        signedData = new CMSSignedData(signatureData);
        
        for (SignerInformation signerInfo : (Collection<SignerInformation>)signedData.getSignerInfos().getSigners())
        {
            System.out.printf("\nSignerInfo: %s / %s\n", signerInfo.getSID().getIssuer(), signerInfo.getSID().getSerialNumber());

            Store certificates = signedData.getCertificates();
            Collection certs = certificates.getMatches(new SignerId(signerInfo.getSID().getIssuer(), signerInfo.getSID().getSerialNumber()));
            
            System.out.print("Certificate: ");
            
            if (certs.size() != 1)
            {
                System.out.printf("Could not identify, %s candidates\n", certs.size());
            }
            else
            {
                X509CertificateHolder cert = (X509CertificateHolder) certs.iterator().next();
                System.out.printf("%s\n", cert.getSubject());
            }

            Map<ASN1ObjectIdentifier, ?> attributes = signerInfo.getUnsignedAttributes().toHashtable();
            
            for (Map.Entry<ASN1ObjectIdentifier, ?> attributeEntry : attributes.entrySet())
            {
                System.out.printf("Attribute %s", attributeEntry.getKey());
                
                if (attributeEntry.getKey().equals(/*SIGNATURE_TIME_STAMP_OID*/PKCSObjectIdentifiers.id_aa_signatureTimeStampToken))
                {
                    System.out.println(" (Signature Time Stamp)");
                    Attribute attribute = (Attribute) attributeEntry.getValue();
                    
                    for (ASN1Encodable encodable : attribute.getAttrValues().toArray())
                    {
                        ContentInfo contentInfo = ContentInfo.getInstance(encodable);
                        TimeStampToken timeStampToken = new TimeStampToken(contentInfo);
                        TimeStampTokenInfo tstInfo = timeStampToken.getTimeStampInfo();

                        System.out.printf("Authority/SN %s / %s\n", tstInfo.getTsa(), tstInfo.getSerialNumber());
                        
                        DigestCalculator digCalc = digCalcProvider .get(tstInfo.getHashAlgorithm());

                        OutputStream dOut = digCalc.getOutputStream();

                        dOut.write(signerInfo.getSignature());
                        dOut.close();

                        byte[] expectedDigest = digCalc.getDigest();
                        boolean matches =  Arrays.constantTimeAreEqual(expectedDigest, tstInfo.getMessageImprintDigest());
                        
                        System.out.printf("Digest match? %s\n", matches);

                        System.out.printf("Signer %s / %s\n", timeStampToken.getSID().getIssuer(), timeStampToken.getSID().getSerialNumber());
                        
                        Store tstCertificates = timeStampToken.getCertificates();
                        Collection tstCerts = tstCertificates.getMatches(new SignerId(timeStampToken.getSID().getIssuer(), timeStampToken.getSID().getSerialNumber()));
                        
                        System.out.print("Certificate: ");
                        
                        if (tstCerts.size() != 1)
                        {
                            System.out.printf("Could not identify, %s candidates\n", tstCerts.size());
                        }
                        else
                        {
                            X509CertificateHolder tstCert = (X509CertificateHolder) tstCerts.iterator().next();
                            System.out.printf("%s\n", tstCert.getSubject());
                            
                            int version = tstCert.toASN1Structure().getVersionNumber();
                            System.out.printf("Version: %s\n", version);
                            if (version != 3)
                                System.out.println("Error: Certificate must be version 3 to have an ExtendedKeyUsage extension.");
                            
                            Extension ext = tstCert.getExtension(Extension.extendedKeyUsage);
                            if (ext == null)
                                System.out.println("Error: Certificate must have an ExtendedKeyUsage extension.");
                            else
                            {
                                if (!ext.isCritical())
                                {
                                    System.out.println("Error: Certificate must have an ExtendedKeyUsage extension marked as critical.");
                                }
                                
                                ExtendedKeyUsage    extKey = ExtendedKeyUsage.getInstance(ext.getParsedValue());
                                if (!extKey.hasKeyPurposeId(KeyPurposeId.id_kp_timeStamping) || extKey.size() != 1)
                                {
                                    System.out.println("Error: ExtendedKeyUsage not solely time stamping.");
                                }                             
                            }
                        }
                    }
                    
                }
                else
                    System.out.println();
            }
        } 
    }

    final CMSSignedData signedData;
    final static ASN1ObjectIdentifier SIGNATURE_TIME_STAMP_OID = new ASN1ObjectIdentifier("1.2.840.113549.1.9.16.2.14");
}
