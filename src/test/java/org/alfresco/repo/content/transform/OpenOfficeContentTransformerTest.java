/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.repo.content.transform;

import java.io.File;
import java.net.URL;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.filestore.FileContentReader;
import org.alfresco.repo.content.filestore.FileContentWriter;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.util.TempFileProvider;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * We no longer use ooo.direct in Community. This test class now is connnected up to the JODConverter which was moved
 * from the Enterprise Edition.
 * 
 * @author Derek Hulley
 */
public class OpenOfficeContentTransformerTest extends AbstractContentTransformerTest
{
    private static String MIMETYPE_RUBBISH = "text/rubbish";
    
    private ContentTransformerWorker worker;
    private ProxyContentTransformer transformer;
    private ContentService contentService;
    
    Logger transformerDebugLogger;
    Level origTransformerDebugLoggerLevel;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        
        this.worker = (ContentTransformerWorker) ctx.getBean("transformer.worker.JodConverter");
        transformer = new ProxyContentTransformer();
        transformer.setMimetypeService(mimetypeService);
        transformer.setTransformerDebug(transformerDebug);
        transformer.setTransformerConfig(transformerConfig);
        transformer.setWorker(this.worker);

        contentService = (ContentService) ctx.getBean("contentService");

        transformerDebugLogger = Logger.getLogger(TransformerDebug.class);
        origTransformerDebugLoggerLevel = transformerDebugLogger.getLevel();
        transformerDebugLogger.setLevel(Level.DEBUG);
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();

        transformerDebugLogger.setLevel(origTransformerDebugLoggerLevel);
    }

    /**
     * @return Returns the same transformer regardless - it is allowed
     */
    protected ContentTransformer getTransformer(String sourceMimetype, String targetMimetype)
    {
        return transformer;
    }

    public void testSetUp() throws Exception
    {
        super.testSetUp();
        assertNotNull(mimetypeService);
    }
    
    public void testReliability() throws Exception
    {
        if (!isOpenOfficeWorkerAvailable())
        {
            // no connection
            System.err.println("ooWorker not available - skipping testReliability !!");
            return;
        }
        boolean reliability = transformer.isTransformable(MIMETYPE_RUBBISH, -1, MimetypeMap.MIMETYPE_TEXT_PLAIN, new TransformationOptions());
        assertEquals("Mimetype should not be supported", false, reliability);
        reliability = transformer.isTransformable(MimetypeMap.MIMETYPE_TEXT_PLAIN, -1, MIMETYPE_RUBBISH, new TransformationOptions());
        assertEquals("Mimetype should not be supported", false, reliability);
        reliability = transformer.isTransformable(MimetypeMap.MIMETYPE_TEXT_PLAIN, -1, MimetypeMap.MIMETYPE_XHTML, new TransformationOptions());
        assertEquals("Mimetype should not be supported", false, reliability);
        reliability = transformer.isTransformable(MimetypeMap.MIMETYPE_TEXT_PLAIN, -1, MimetypeMap.MIMETYPE_WORD, new TransformationOptions());
        assertEquals("Mimetype should be supported", true, reliability);
        reliability = transformer.isTransformable(MimetypeMap.MIMETYPE_WORD, -1, MimetypeMap.MIMETYPE_TEXT_PLAIN, new TransformationOptions());
        assertEquals("Mimetype should be supported", true, reliability);
    }
    
    /**
     * Test what is up with HTML to PDF
     */
    public void testHtmlToPdf() throws Exception
    {
        if (!isOpenOfficeWorkerAvailable())
        {
            // no connection
            System.err.println("ooWorker not available - skipping testHtmlToPdf !!");
            return;
        }
        File htmlSourceFile = loadQuickTestFile("html");
        File pdfTargetFile = TempFileProvider.createTempFile(getName() + "-target-", ".pdf");
        ContentReader reader = new FileContentReader(htmlSourceFile);
        reader.setMimetype(MimetypeMap.MIMETYPE_HTML);
        ContentWriter writer = new FileContentWriter(pdfTargetFile);
        writer.setMimetype(MimetypeMap.MIMETYPE_PDF);
    }
    
    /**
     * Test what is up with HTML to PDF again in 6.0 REPO-3033
     * In this case we use the full set of transformers as we would in a live system.
     */
    public void testHtmlToPdfRepo3033() throws Exception
    {
        // Before fixing the problem this test failed just like the live system with the following output.

        // The source html file is passed to a JodConverter to turn it into an odt file.
        // This should then be converted to pdf by a second JodConverter.

        // The second conversion fails when AbstractConversionTask (in the alfresco-jodconverter-core-3.0.1
        // execute method calls loadDocument(context, inputFile) as the soffice crashes and is restarted.

        // This may be related to the fact that we have given the file an odt extension and where

        // The intermediate mimetype is application/vnd.oasis.opendocument.text-web and we have given it an odt
        // extension. The mimetype of odt does not normally have -web on the end.


        // jurt 3.2.1

        // 2017-12-08 16:41:45,319  DEBUG [content.transform.TransformerDebug] [main] 1             html pdf  1.6 KB JodConverter.Html2Pdf<<Complex>>
        // 2017-12-08 16:41:45,319  DEBUG [content.transform.TransformerDebug] [main] 1
        // 2017-12-08 16:41:45,370  DEBUG [content.transform.TransformerDebug] [main] 1.1           html odt  <<TemporaryFile>> 1.6 KB JodConverter<<Proxy>>
        // 2017-12-08 16:41:46,408  DEBUG [content.transform.TransformerDebug] [main] 1.2           odt  pdf  <<TemporaryFile>> 7.8 KB JodConverter<<Proxy>>
        //Dec 08, 2017 4:41:56 PM org.artofsolving.jodconverter.office.OfficeConnection$1 disposing
        //INFO: disconnected: 'socket,host=127.0.0.1,port=8100,tcpNoDelay=1'
        //Dec 08, 2017 4:41:56 PM org.artofsolving.jodconverter.office.PooledOfficeManager$1 disconnected
        //WARNING: connection lost unexpectedly; attempting restart
        //Dec 08, 2017 4:41:56 PM org.artofsolving.jodconverter.office.ManagedOfficeProcess doEnsureProcessExited
        //INFO: process exited with code 0
        // 2017-12-08 16:41:56,668  DEBUG [content.transform.TransformerDebug] [main] 1.2                     Failed: Mime type was 'application/vnd.oasis.opendocument.text-web' 11080018 OpenOffice server conversion failed
        // 2017-12-08 16:41:56,670  DEBUG [content.transform.TransformerDebug] [main] 1.2                     Failed 11080018 OpenOffice server conversion failed
        // 2017-12-08 16:41:56,680  DEBUG [content.transform.TransformerDebug] [main] 1                       Failed 11080019 Content conversion failed
        // 2017-12-08 16:41:56,682  DEBUG [content.transform.TransformerDebug] [main] 1             Finished in 11,362 ms
        //
        //org.alfresco.service.cmr.repository.ContentIOException: 11080020 Content conversion failed:
        //   reader: ContentAccessor[ contentUrl=store://C:\noscan\bat1\alfresco-repository\target\classes\quick\repo3033.html, mimetype=text/html, size=1689, encoding=UTF-8, locale=en_GB]
        //   writer: ContentAccessor[ contentUrl=store://C:\Users\adavis\AppData\Local\Temp\Alfresco\testHtmlToPdfRepo3033-target-3408472472346063367.pdf, mimetype=application/pdf, size=0, encoding=UTF-8, locale=en_GB]
        //   options: {use=null, contentReaderNodeRef=null, sourceContentProperty=null, contentWriterNodeRef=null, targetContentProperty=null, includeEmbedded=null}
        //   limits: {timeoutMs=120000}
        //
        //	at org.alfresco.repo.content.transform.AbstractContentTransformer2.transform(AbstractContentTransformer2.java:354)
        //	at org.alfresco.repo.content.transform.OpenOfficeContentTransformerTest.testHtmlToPdfRepo3033(OpenOfficeContentTransformerTest.java:161)
        //  ...
        //Caused by: org.alfresco.service.cmr.repository.ContentIOException: 11080019 Content conversion failed:
        //   reader: ContentAccessor[ contentUrl=store://C:\Users\adavis\AppData\Local\Temp\Alfresco\ComplextTransformer_intermediate_html_6778438399165479185.odt, mimetype=application/vnd.oasis.opendocument.text-web, size=8061, encoding=UTF-8, locale=en_GB]
        //   writer: ContentAccessor[ contentUrl=store://C:\Users\adavis\AppData\Local\Temp\Alfresco\testHtmlToPdfRepo3033-target-3408472472346063367.pdf, mimetype=application/pdf, size=0, encoding=UTF-8, locale=en_GB]
        //   options: {use=null, contentReaderNodeRef=null, sourceContentProperty=null, contentWriterNodeRef=null, targetContentProperty=null, includeEmbedded=null}
        //   limits: {timeoutMs=120000}
        //   claimed mime type: application/vnd.oasis.opendocument.text-web
        //   detected mime type: application/vnd.oasis.opendocument.text-web
        //   transformer not found
        //
        //	at org.alfresco.repo.content.transform.AbstractContentTransformer2.transform(AbstractContentTransformer2.java:385)
        //	at org.alfresco.repo.content.transform.ComplexContentTransformer.transformInternal(ComplexContentTransformer.java:492)
        //	at org.alfresco.repo.content.transform.AbstractContentTransformer2.transform(AbstractContentTransformer2.java:272)
        //	... 19 more

        if (!isOpenOfficeWorkerAvailable())
        {
            // no connection
            System.err.println("ooWorker not available - skipping testHtmlToPdf !!");
            return;
        }

        File htmlSourceFile = loadNamedQuickTestFile("repo3033.html");
        File pdfTargetFile = TempFileProvider.createTempFile(getName() + "-target-", ".pdf");
        ContentReader reader = new FileContentReader(htmlSourceFile);
        reader.setMimetype(MimetypeMap.MIMETYPE_HTML);
        ContentWriter writer = new FileContentWriter(pdfTargetFile);
        writer.setMimetype(MimetypeMap.MIMETYPE_PDF);

        ContentTransformer contentTransformer = contentService.getTransformer(MimetypeMap.MIMETYPE_HTML, MimetypeMap.MIMETYPE_PDF);

        assertEquals("transformer.JodConverter.Html2Pdf", contentTransformer.getName());

        TransformationOptions options = new TransformationOptions();
        contentTransformer.transform(reader, writer, options);
    }

    /**
     * ALF-219. Transforamtion from .html to .pdf for empty file.
     * @throws Exception
     */
    public void testEmptyHtmlToEmptyPdf() throws Exception
    {
        if (!isOpenOfficeWorkerAvailable())
        {
            // no connection
            System.err.println("ooWorker not available - skipping testEmptyHtmlToEmptyPdf !!");
            return;
        }
        URL url = this.getClass().getClassLoader().getResource("misc/empty.html");
        assertNotNull("URL was unexpectedly null", url);

        File htmlSourceFile = new File(url.getFile());
        assertTrue("Test file does not exist.", htmlSourceFile.exists());
        
        File pdfTargetFile = TempFileProvider.createTempFile(getName() + "-target-", ".pdf");
        
        ContentReader reader = new FileContentReader(htmlSourceFile);
        reader.setMimetype(MimetypeMap.MIMETYPE_HTML);
        ContentWriter writer = new FileContentWriter(pdfTargetFile);
        writer.setMimetype(MimetypeMap.MIMETYPE_PDF);
        
        transformer.transform(reader, writer);
    }
    
    /**
     * Some transformations fail intermittently within OOo on our test server.
     * Rather than exclude these transformations from product code, where they
     * may work (e.g. due to different OOo version installed), they are excluded
     * from this test.
     */
    @Override
    protected boolean isTransformationExcluded(String sourceExtension, String targetExtension)
    {
        return ((sourceExtension.equals("doc") && targetExtension.equals("docx")) ||
        		(sourceExtension.equals("doc") && targetExtension.equals("html")) ||
        		(sourceExtension.equals("doc") && targetExtension.equals("odt")) ||
        		(sourceExtension.equals("doc") && targetExtension.equals("rtf")) ||
        		(sourceExtension.equals("doc") && targetExtension.equals("sxw")) ||
        		(sourceExtension.equals("doc") && targetExtension.equals("txt")) ||
        		(sourceExtension.equals("docx") && targetExtension.equals("sxw")) ||
        		(sourceExtension.equals("html") && targetExtension.equals("docx")) ||
        		(sourceExtension.equals("odp") && targetExtension.equals("pptx")) ||
        		(sourceExtension.equals("ods") && targetExtension.equals("html")) ||
        		(sourceExtension.equals("ods") && targetExtension.equals("sxc")) ||
        		(sourceExtension.equals("ods") && targetExtension.equals("xlsx")) ||
        		(sourceExtension.equals("ods") && targetExtension.equals("xls")) ||
        		(sourceExtension.equals("odt") && targetExtension.equals("docx")) ||
        		(sourceExtension.equals("odt") && targetExtension.equals("txt")) ||
        		(sourceExtension.equals("ppt") && targetExtension.equals("html")) ||
        		(sourceExtension.equals("ppt") && targetExtension.equals("pptx")) ||
        		(sourceExtension.equals("sxc") && targetExtension.equals("xlsx")) ||
        		(sourceExtension.equals("sxi") && targetExtension.equals("odp")) ||
        		(sourceExtension.equals("sxi") && targetExtension.equals("pptx")) ||
        		(sourceExtension.equals("sxw") && targetExtension.equals("docx")) ||
        		(sourceExtension.equals("txt") && targetExtension.equals("docx")) ||
        		(sourceExtension.equals("txt") && targetExtension.equals("html")) ||
        		(sourceExtension.equals("txt") && targetExtension.equals("odt")) ||
        		(sourceExtension.equals("txt") && targetExtension.equals("pdf")) ||
        		(sourceExtension.equals("txt") && targetExtension.equals("rtf")) ||
        		(sourceExtension.equals("txt") && targetExtension.equals("sxw")) ||
        		(sourceExtension.equals("wpd") && targetExtension.equals("docx")) ||
        		(sourceExtension.equals("xls") && targetExtension.equals("ods")) ||
        		(sourceExtension.equals("xls") && targetExtension.equals("pdf")) ||
        		(sourceExtension.equals("xls") && targetExtension.equals("sxc")) ||
        		(sourceExtension.equals("xls") && targetExtension.equals("xlsx")) ||

        		(sourceExtension.equals("txt") && targetExtension.equals("doc")) ||

        		(sourceExtension.equals("pptx") && targetExtension.equals("html")));
    }

}
