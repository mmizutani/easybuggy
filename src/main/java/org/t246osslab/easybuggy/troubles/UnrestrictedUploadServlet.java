package org.t246osslab.easybuggy.troubles;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.t246osslab.easybuggy.utils.Closer;
import org.t246osslab.easybuggy.utils.HTTPResponseCreator;
import org.t246osslab.easybuggy.utils.MessageUtils;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/urupload" })
// 2MB, 10MB, 50MB
// @MultipartConfig(fileSizeThreshold = 1024 * 1024 * 2, maxFileSize = 1024 * 1024 * 10, maxRequestSize = 1024 * 1024 *
// 50)
@MultipartConfig
public class UnrestrictedUploadServlet extends HttpServlet {

    private static Logger log = LoggerFactory.getLogger(DBConnectionLeakServlet.class);

    // Name of the directory where uploaded files is saved
    private static final String SAVE_DIR = "uploadFiles";

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        StringBuilder bodyHtml = new StringBuilder();
        bodyHtml.append("<form method=\"post\" action=\"urupload\" enctype=\"multipart/form-data\">");
        bodyHtml.append("Select file to upload: <input type=\"file\" name=\"file\" size=\"60\" /><br>");
        bodyHtml.append("<br><input type=\"submit\" value=\"Upload\" />");
        bodyHtml.append("</form>");
        HTTPResponseCreator.createSimpleResponse(res, "File Upload", bodyHtml.toString());
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        Locale locale = req.getLocale();

        // Get absolute path of the web application
        String appPath = req.getServletContext().getRealPath("");

        // Create directory to save uploaded file if it does not exists
        String savePath = appPath + File.separator + SAVE_DIR;
        File fileSaveDir = new File(savePath);
        if (!fileSaveDir.exists()) {
            fileSaveDir.mkdir();
        }

        OutputStream out = null;
        InputStream in = null;
        final Part filePart = req.getPart("file");
        try {
            String fileName = getFileName(filePart);
            out = new FileOutputStream(savePath + File.separator + fileName);
            in = filePart.getInputStream();
            int read = 0;
            final byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }

            // Revere color of the upload image
            revereColor(new File(savePath + File.separator + fileName).getAbsolutePath());

            StringBuilder bodyHtml = new StringBuilder();
            bodyHtml.append(MessageUtils.getMsg("msg.reverse.color.complete", locale));
            bodyHtml.append("<br><br>");
            bodyHtml.append("<img src=\"" + SAVE_DIR + "/" + fileName + "\">");
            bodyHtml.append("<br><br>");
            bodyHtml.append("<INPUT type=\"button\" onClick='history.back();' value=\""
                    + MessageUtils.getMsg("label.history.back", locale) + "\">");
            HTTPResponseCreator.createSimpleResponse(res, "Upload", bodyHtml.toString());

        } catch (Exception e) {
            log.error("Exception occurs: ", e);
        } finally {
            Closer.close(out, in);
        }
    }

    private String getFileName(final Part part) {
        for (String content : part.getHeader("content-disposition").split(";")) {
            if (content.trim().startsWith("filename")) {
                return content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }

    // Revere color of the image file
    private void revereColor(String fileName) throws IOException {
        try {
            BufferedImage image = ImageIO.read(new File(fileName));
            WritableRaster raster = image.getRaster();
            int[] pixelBuffer = new int[raster.getNumDataElements()];
            for (int y = 0; y < raster.getHeight(); y++) {
                for (int x = 0; x < raster.getWidth(); x++) {
                    raster.getPixel(x, y, pixelBuffer);
                    pixelBuffer[0] = ~pixelBuffer[0];
                    pixelBuffer[1] = ~pixelBuffer[1];
                    pixelBuffer[2] = ~pixelBuffer[2];
                    raster.setPixel(x, y, pixelBuffer);
                }
            }
            // Output the image
            ImageIO.write(image, "png", new File(fileName));
        } catch (Exception e) {
            // Log and ignore the exception
            log.error("Exception occurs: ", e);
        }
    }
}
