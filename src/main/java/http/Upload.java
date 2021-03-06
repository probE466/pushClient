package http;


import com.google.gson.Gson;
import config.Config;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.log4j.Logger;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Scanner;

public class Upload {

    private static final Logger LOGGER = Logger.getLogger(Upload.class.getName());

    public static final String POST = "/api/post";

    public static void uploadTempContent(final File file, final String target, final Config config) throws IOException {
        LOGGER.info("Uploading Temp Content");
        uploadDataToServer(file, target, config);
        file.delete();
    }

    public static void uploadFile(final File file, final String target, final Config config) throws IOException {
        LOGGER.info("Uploading File");
        uploadDataToServer(file, target, config);
    }

    private static void uploadDataToServer(File file, String target, final Config config) throws IOException {
        LOGGER.info("Uploading Data. File: " + file.getName() + " Target: " + target);
        CloseableHttpClient httpClient = null;
        try {
            httpClient = HttpClients.custom().setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, (TrustStrategy) (arg0, arg1) -> true).build()).build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            e.printStackTrace();
        }
        String key = config.getProperties().getProperty("key");
        HttpPost httpPost = new HttpPost(target + POST);
        StringBody keyBody = new StringBody(key, ContentType.TEXT_PLAIN);
        HttpEntity httpEntity = MultipartEntityBuilder.create().addPart("file", new FileBody(file)).addPart("key", keyBody).build();
        httpPost.setEntity(httpEntity);
        HttpResponse response = httpClient.execute(httpPost);

        if (response.getStatusLine().getStatusCode() == 200) {
            final UrlDTO urlDTO = parseResponse(response);
            StringSelection stringSelection = new StringSelection(target + urlDTO.getFileViewUrl());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, stringSelection);
            LOGGER.info("Pasted link to clipboard: " + target + urlDTO.getFileViewUrl());
            LOGGER.info("Delete link: " + target + urlDTO.getFileDeleteUrl());
            response.getEntity().getContent().close();
        } else {
            throw new IOException("Statuscode: " + response.getStatusLine().getStatusCode());
        }
    }

    private static UrlDTO parseResponse(HttpResponse response) throws IOException {
        final Gson gson = new Gson();

        final Scanner s = new Scanner(response.getEntity().getContent()).useDelimiter("\\A");
        if (!s.hasNext()) {
            return null;
        }

        final String jsonString = s.next();
        s.close();
        return gson.fromJson(jsonString, UrlDTO.class);
    }
}
