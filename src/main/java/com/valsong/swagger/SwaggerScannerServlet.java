package com.valsong.swagger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.valsong.swagger.support.SwaggerScannerInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 拦截swagger的请求
 */
public class SwaggerScannerServlet implements Servlet {

    private static final Logger logger = LoggerFactory.getLogger(SwaggerScannerServlet.class);

    private static final String ENCODING = "UTF-8";

    private static final String CONTENT_TYPE = "Content-Type";

    private static final String APPLICATION_JSON = "application/json;charset=UTF-8";

    /**
     * 美化Json的Gson
     */
    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting()
            .registerTypeAdapter(Double.class, (JsonSerializer<Double>) (src, typeOfSrc, context) -> {
                if (src == src.longValue()) {
                    return new JsonPrimitive(src.longValue());
                }
                return new JsonPrimitive(src);
            })
            .setDateFormat("yyyy-MM-dd HH:mm:ss").create();


    @Override
    public void init(ServletConfig config) throws ServletException {

    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        StringBuilder bodyBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), ENCODING))) {
            String s;
            while ((s = reader.readLine()) != null) {
                bodyBuilder.append(s);
            }
        } catch (IOException e) {
            throw e;
        }

        //请求Json
        String requestJson = bodyBuilder.toString();

        logger.info("swagger responseJson : {}", requestJson);

        String methodName = request.getParameterNames().nextElement();

        Object returnVal = SwaggerScannerInvoker.invoke(methodName, requestJson);

        String responseJson = GSON_PRETTY.toJson(returnVal);

        logger.info("swagger responseJson : {}", responseJson);

        response.setCharacterEncoding(ENCODING);
        response.setHeader(CONTENT_TYPE, APPLICATION_JSON);
        response.getWriter().write(responseJson);


    }

    @Override
    public String getServletInfo() {
        return null;
    }

    @Override
    public void destroy() {

    }
}
