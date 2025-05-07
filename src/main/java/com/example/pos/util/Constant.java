package com.example.pos.util;


public final class Constant {

    public static final String QR_CODE_BASE_PATH = "QrCode";
    public static final String SLASH = "/";
    public static final String IMAGE_EXTENSION = ".png";
    public static final String IMAGE_JPEG = ".jpeg";
    public static final long ONE_THOUSAND = 1000;
    public static final long SIXTY = 60;
    public static final long FIFTEEN = 15;
    public static final int FIVE_THOUSAND = 5000;
    public static final int FOUR_THOUSAND_NITYNINE = 4999;
    public static final int TEN = 10;
    public static final String ERROR_CODE = "500";
    public static final String DEBIT = "DEBIT";
    public static final String CREDIT = "CREDIT";
    public static final String QR_PAYMENT = "QR_PAYMENT";
    public static final String JAZZCASH = "JAZZCASH";
    public static final String EP_STATIC_QR = "EP_STATIC_QR";
    public static final String JC_STATIC_QR = "JC_STATIC_QR";
    public static final String EASYPAISA = "EASYPAISA";
    public static final String JAZZCASH_QR_PAYMENT = "JAZZCASH_QR_PAYMENT";
    public static final String IBM_CLIENT_ID = "X-IBM-CLIENT-ID";
    public static final String IBM_CLIENT_SECRET = "X-IBM-CLIENT-SECRET";
    public static final String NAMESPACE = "namespace";
    public static final String COOKIE = "Cookie";
    public static final String AES = "AES";
    public static final int HEX_BASE = 16;
    public static final int FOUR = 4;
    public static final long CORS_MAX_AGE = 10L;
    public static final String CLIENT_NAME = "clientName";
    public static final String TILL_NUMBER = "TillNumber";
    public static final String IBM_CLIENT_ID_VALUE = "5ef5a981c1aa06f5103cdefeec2d990b";
    public static final String IBM_CLIENT_SECRET_VALUE = "ba3d0dff0b40af95c042e2a8f724c24c";
    public static final String COOKIE_VALUE = "TS01b2d23d=019409637ed8594dd478f03a18e89126bf40e08ef61bc055ddf0372ce8c"
            + "00f5811ea3d8d0c6958dbbc56d9edf41a307ac888a9390b";
    public static final String KARACHI_TIME_ZONE = "Asia/Karachi";

    public static final String X_CHANNEL = "merchantApp";
    public static final String JAZZCASH_GENERATE_QRCODE_URL_STATIC =
            "/rest/api/v1/thirdparty/generate/static/qrcode";

    public static final String JAZZCASH_GENERATE_QRCODE_URL_DYNAMIC =
            "/rest/api/v1/thirdparty/generate/dynamic/qrcode";

    public static final String STATUS_TRUE = "true";
    public static final String TRANSACTION_STATUS_PAID = "PAID";
    public static final String TRANSACTION_STATUS_UNPAID = "UNPAID";



    private Constant() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

}
