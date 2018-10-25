package com.valsong.swagger.constant;

/**
 * Swagger常量
 *
 * @author: Val Song
 */
public final class SwaggerScannerConstants {

    private SwaggerScannerConstants() {
    }


    public static String URL_PATTERN = "/swagger_scanner";

    /**
     * 页面显示的swagger扫描的接口的前缀
     */
    public static String PATH_PREFIX = "/swagger_scanner?";

    /**
     * COLLECTION_FIRST_INDEX
     */
    public static final int COLLECTION_FIRST_INDEX = 0;

    /**
     * COLLECTION_SECOND_INDEX
     */
    public static final int COLLECTION_SECOND_INDEX = 1;

    /**
     * ONE_ELEMENT_COLLECTION_LENGTH
     */
    public static final int ONE_ELEMENT_COLLECTION_LENGTH = 1;

    /**
     * EMPTY_COLLECTION_LENGTH
     */
    public static final int EMPTY_COLLECTION_LENGTH = 0;

    /**
     * REQUIRED_TRUE
     */
    public static final String REQUIRED_TRUE = "[必填]";

    /**
     * BODY
     */
    public static final String BODY = "body";

    /**
     * access
     */
    public static final String ACCESS = "access";

    /**
     * <pre>
     */
    public static final String OPEN_PRE = "<pre>";

    /**
     * <pre style='font-style: normal;'>
     */
    public static final String OPEN_PRE_FONT_NORMAL = "<pre style='font-style: normal;'>";

    /**
     * </pre>
     */
    public static final String CLOSE_PRE = "</pre>";

}
