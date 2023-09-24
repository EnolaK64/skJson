package cz.coffee.skjson.api.Update;

import com.google.gson.*;
import cz.coffee.skjson.api.FileWrapper;
import cz.coffee.skjson.skript.requests.Requests;
import cz.coffee.skjson.utils.TimerWrapper;
import cz.coffee.skjson.utils.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static cz.coffee.skjson.api.Config.*;

/**
 * The type Http wrapper.
 */
public class HttpWrapper implements AutoCloseable {

    /**
     * The constant GSON.
     */
    final static Gson GSON = new GsonBuilder().serializeNulls().disableHtmlEscaping().setLenient().create();

    @Override
    public String toString() {
        return "HttpWrapper{" +
                "_headers=" + _headers +
                ", method=" + method +
                ", client=" + client +
                ", builder=" + builder +
                ", content=" + content +
                ", timer=" + timer +
                ", request=" + request +
                ", attachments=" + attachments +
                ", requestUrl='" + requestUrl + '\'' +
                '}';
    }

    @Override
    public void close() {
        if (client == null && builder == null) return;
        client = null;
        builder = null;
        method = null;
    }

    /**
     * The type Header.
     */
    @SuppressWarnings("unused")
    public static class Header {
        private final HttpHeaders headers;

        /**
         * Instantiates a new Header.
         *
         * @param headers the headers
         */
        public Header(HttpHeaders headers) {
            this.headers = headers;
        }

        /**
         * Json json element.
         *
         * @return the json element
         */
        public JsonElement json() {
            return GSON.toJsonTree(headers.map());
        }

        /**
         * Text string.
         *
         * @return the string
         */
        public String text() {
            return GSON.toJson(headers.map());
        }

        /**
         * Raw http headers.
         *
         * @return the http headers
         */
        public HttpHeaders raw() {
            return headers;
        }

    }


    /**
     * The interface Response.
     */

    private record JsonFixer(String json) {

        String removeTrailingComma() {
            return json.replaceAll(",\\s*}", "}").replaceAll(",\\s*]", "]");
        }
    }

    @SuppressWarnings("unused")
    public interface Response {

        /**
         * Of response.
         *
         * @param requestHeaders  the request headers
         * @param responseHeaders the response headers
         * @param uri             the uri
         * @param body            the body
         * @param statusCode      the status code
         * @return the response
         */
        static Response of(HttpHeaders requestHeaders, HttpHeaders responseHeaders, URI uri, String body, int statusCode, boolean lenient) {
            return new Response() {
                @Override
                public Header getRequestHeaders() {
                    return new Header(requestHeaders);
                }

                @Override
                public Header getResponseHeader() {
                    return new Header(responseHeaders);
                }

                @Override
                public int getStatusCode() {
                    return statusCode;
                }

                @Override
                public Object getBodyContent(boolean saveIncorrect) {
                    if (statusCode >= 200 && statusCode <= 340) {
                        try {
                            if (lenient) {
                                JsonFixer fixer = new JsonFixer(body);
                                String finalBody = fixer.removeTrailingComma();
                                return JsonParser.parseString(finalBody);
                            } else {
                                return JsonParser.parseString(body);
                            }
                        } catch (Exception e) {
                            if (PROJECT_DEBUG) {
                                Util.error(true, e.getMessage());
                                if (LOGGING_LEVEL > 2) Util.enchantedError(e, e.getStackTrace(), "Invalid JSON");
                            }
                        }
                        return JsonNull.INSTANCE;
                    } else {
                        if (saveIncorrect) {
                            try {
                                return body;
                            } catch (Exception e) {
                                if (PROJECT_DEBUG) Util.error(e.getMessage());
                            }
                        }
                    }
                    return null;
                }

                @Override
                public Optional<URL> getRequestURL() {
                    try {
                        return Optional.of(uri.toURL());
                    } catch (MalformedURLException exception) {
                        if (PROJECT_DEBUG) Util.enchantedError(exception, exception.getStackTrace(), "Invalid URL");
                        return Optional.empty();
                    }
                }

                @Override
                public boolean isSuccessfully() {
                    return statusCode >= 200 && statusCode < 230;
                }
            };
        }

        /**
         * Gets request headers.
         *
         * @return the request headers
         */
        Header getRequestHeaders();

        /**
         * Gets response header.
         *
         * @return the response header
         */
        Header getResponseHeader();

        /**
         * Gets status code.
         *
         * @return the status code
         */
        int getStatusCode();


        Object getBodyContent(boolean saveIncorrect);

        /**
         * Gets request url.
         *
         * @return the request url
         */
        Optional<URL> getRequestURL();

        /**
         * Is successfully boolean.
         *
         * @return the boolean
         */
        boolean isSuccessfully();
    }

    @SuppressWarnings("unused")
    private final ConcurrentHashMap<String, String> _headers = new ConcurrentHashMap<>();
    private Requests.RequestMethods method;
    private HttpClient client;
    private HttpRequest.Builder builder;
    private JsonObject content = new JsonObject();
    private TimerWrapper timer;
    private HttpRequest request;
    private final LinkedList<File> attachments = new LinkedList<>();

    /**
     * Instantiates a new Http wrapper.
     *
     * @param URL    the url
     * @param method the method
     */
    @SuppressWarnings("unused")
    public HttpWrapper(String URL, String method) {
        this(URL, Requests.RequestMethods.valueOf(method.toUpperCase()));
    }

    /**
     * Instantiates a new Http wrapper.
     *
     * @param URL    the url
     * @param method the method
     */
    public HttpWrapper(String URL, Requests.RequestMethods method) {
        if (method == null) {
            if (PROJECT_DEBUG) Util.error("HttpWrapper: The method cannot be null");
            return;
        }
        this.method = method;


        // Initialized http client
        client = HttpClient.newHttpClient();
        timer = new TimerWrapper(0);

        // create URL from string
        URI requestLink;
        try {
            requestLink = new URI(sanitizeLinkSpaces(URL));
        } catch (Exception e) {
            if (PROJECT_DEBUG) Util.enchantedError(e, e.getStackTrace(), "HttpWrapper:sanitizeLinkSpaces");
            return;
        }
        builder = HttpRequest.newBuilder().uri(requestLink);
        requestUrl = requestLink.toString();
    }

    public void postAttachments(String body) {
        AtomicInteger i = new AtomicInteger(0);
        MimeMultipartData data;
        var mmd = MimeMultipartData.newBuilder().withCharset(StandardCharsets.UTF_8);
        attachments.forEach(attachment -> {
            try {
                mmd.addFile(String.valueOf(i.incrementAndGet()), attachment.toPath(), Files.probeContentType(attachment.toPath()));
            } catch (Exception e) {
                if (PROJECT_DEBUG) Util.error(e.getMessage());
            }
        });
        mmd.addText("payload_json", body);
        try {
            data = mmd.build();
            builder.header("Content-Type", data.getContentType());
            request = builder.POST(data.getBodyPublisher()).build();
        } catch (Exception ex) {
            if (PROJECT_DEBUG) Util.log(ex);
        }
    }

    /**
     * Request http wrapper.
     *
     * @return the http wrapper
     */
    public HttpWrapper request() {
        if (builder != null) {
            switch (method) {
                case GET -> request = builder.GET().build();
                case POST -> {
                    String convertedBody = GSON.toJson(content);
                    if (!attachments.isEmpty()) {
                        postAttachments(convertedBody);
                    } else {
                        HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.ofString(convertedBody);
                        request = builder.POST(body).build();
                    }
                }
                case PUT -> {
                    String convertedBody = GSON.toJson(content);
                    if (!attachments.isEmpty()) {
                        postAttachments(convertedBody);
                    } else {
                        HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.ofString(convertedBody);
                        request = builder.PUT(body).build();
                    }
                }
                case DELETE -> request = builder.DELETE().build();
                case HEAD -> {
                    HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.noBody();
                    request = builder.method("HEAD", body).build();
                }
                case PATCH -> {
                    // need to be JsonEncoded
                    String convertedBody = GSON.toJson(content);
                    HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.ofString(convertedBody);
                    request = builder.method("PATCH", body).build();
                }
                default -> request = builder.build();
            }
        }
        return this;
    }

    /**
     * Colorized method string.
     *
     * @param method the method
     * @return the string
     */
    public String colorizedMethod(Requests.RequestMethods method) {
        StringBuilder sb = new StringBuilder();
        sb.append("&l");
        switch (method) {
            case GET -> sb.append("&aGET");
            case POST -> sb.append("&bPOST");
            case PUT -> sb.append("&7PUT");
            case DELETE -> sb.append("&cDELETE");
            case HEAD -> sb.append("&3HEAD");
            case PATCH -> sb.append("&ePATCH");
            case MOCK -> sb.append("&5MOCK");
        }
        sb.append("&r");
        return sb.toString();
    }

    public static File changeExtension(File f, String newExtension) throws IOException {
        int i = f.getName().lastIndexOf('.');
        String name = f.getName().substring(0, i);
        File tempFile = File.createTempFile(name + ".sk -- ", newExtension);

        try (FileInputStream fis = new FileInputStream(f);
             FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }

        return tempFile;
    }

    @SuppressWarnings("all")
    public HttpWrapper addAttachment(String pathToAttachment) {
        File file;
        if (pathToAttachment.startsWith("*")) {
            file = FileWrapper.serchFile(pathToAttachment.replaceAll("[*/]", ""));
        } else file = new File(pathToAttachment);
        try {
            if (file.getName().endsWith(".sk")) file = changeExtension(file, ".vb");
        } catch (IOException exception) {
            if (PROJECT_DEBUG) Util.requestLog(exception.getMessage());
        }
        if (file.exists()) attachments.add(file);
        return this;
    }

    private String requestUrl;

    /**
     * Process response.
     *
     * @return the response
     */
    public Response process(boolean... _lenient) {
        boolean lenient = _lenient != null && _lenient.length > 0 && _lenient[0];
        try (var timer = new TimerWrapper(0)) {
            this.timer = timer;
            HttpResponse.BodyHandler<String> body = HttpResponse.BodyHandlers.ofString();
            return client.sendAsync(request, body).thenApply(future -> {
                requestUrl = future.uri().toString();
                if (LOGGING_LEVEL > 1)
                    Util.log(String.format(
                            REQUESTS_PREFIX + ": " + colorizedMethod(method) + " request was send to &b'%s'&r and takes %s", requestUrl, timer.toHumanTime()));
                return Response.of(request.headers(), future.headers(), future.uri(), future.body(), future.statusCode(), lenient);
            }).get();
        } catch (Exception e) {
            if (PROJECT_DEBUG) Util.error(e.getMessage());
            return null;
        }
    }

    /**
     * Gets time.
     *
     * @return the time
     */
    @SuppressWarnings("unused")
    public String getTime() {
        return timer.toHumanTime();
    }

    /**
     * Sets content.
     *
     * @param body the body
     * @return the content
     */
    public HttpWrapper setContent(final JsonElement body) {
        if (body.isJsonObject()) {
            content = body.getAsJsonObject();
        }
        return this;
    }

    /**
     * Sets headers.
     *
     * @param headers the headers
     * @return the headers
     */
    @SuppressWarnings("all")
    public HttpWrapper setHeaders(final JsonElement headers) {
        if (!headers.isJsonNull()) {
            if (headers.isJsonObject()) {
                JsonObject object = headers.getAsJsonObject();
                object.entrySet().forEach(entry -> {
                    String value = null;
                    if (entry.getValue() instanceof JsonPrimitive primitive) {
                        if (primitive.isString()) {
                            value = primitive.getAsString();
                        } else {
                            value = primitive.toString();
                        }
                    }
                    if (value != null) builder.setHeader(entry.getKey(), value);
                });
            }
        }
        return this;
    }

    /**
     * Add header http wrapper.
     *
     * @param name  the name
     * @param value the value
     * @return the http wrapper
     */
    public HttpWrapper addHeader(final String name, final String value) {
        if (name != null && value != null) {
            builder.header(name, value);
        }
        return this;
    }

    private String sanitizeLinkSpaces(String url) {
        return url.replace(" ", "%20");
    }
}
