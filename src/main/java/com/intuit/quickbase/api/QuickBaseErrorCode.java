/*
 * Copyright (c) 2015 Cartera Commerce Inc. All Rights reserved.
 * -------------------------------------------------------------------------------------------------
 *
 * File name  : QuickBaseErrorCode.java
 * @author Cristian Baciu
 * -------------------------------------------------------------------------------------------------
 *
 *
 * *************************************************************************************************
 */

package com.intuit.quickbase.api;


/**
 * The enumeration {@link QuickBaseErrorCode} provides a list of all known error codes produced 
 * by a call to the QuickBase HTTP API.
 *
 * @author Cristian Baciu
 * @version $Revision$
 */
public enum QuickBaseErrorCode 
{
    ERROR_CODE_NOT_RECOGNIZED(-1),
    
    OK(0),
    UNKNOWN(1),
    INVALID_INPUT(2),
    INSUFFICIENT_PERMISSIONS(3),
    INVALID_TICKET(4),
    UNIMPLEMENTED_OP(5),
    SYNTAX_ERROR(6),
    API_NO_ALLOWED(7),
    SSL_REQUIRED(8),
    INVALID_CHOICE(9),
    INVALID_FIELD_TYPE(10),
    XML_PARSE_ERROR(11),
    INVALID_SOURCE_DBID(12),
    INVALID_ACCT(13),
    DBID_ERROR(14),
    INVALID_HOSTNAME(15),
    UNAUTHORIZED_IP(19),
    INVALID_CREDENTIALS(20),
    INVALID_USER(21),
    SIGNIN_REQUIRED(22),
    FEATURE_NOT_SUPPORTED(23),
    INVALID_APPTOKEN(24),
    DUPLICATE_APPTOKEN(25),
    MAX_COUNT(26),
    REGISTRATION_REQUIRED(27),
    MANAGED_BY_LDAP(28),
    DENIED(29),
    NO_RECORD(30),
    NO_FIELD(31),
    NO_APP(32),
    NO_QUERY(33),
    CANNOT_CHANGE_VALUE(34),
    NO_DATA(35),
    CLONING_ERROR(36),
    NO_REPORT(37),
    RESTRICTED_FIELD_IN_REPORT(38),
    REQUIRED_FIELD(50),
    UNIQ_ERROR(51),
    DUPLICATE_FIELD(52),
    MISSING_FIELDS(53),
    CACHE_NOT_FOUND(54),
    UPDATE_ERROR(60),
    SCHEMA_LOCKED(61),
    OVER_ACCT_SIZE_LIMIT(70),
    OVER_DB_SIZE_LIMIT(71),
    ACCT_SUSPENDED(73),
    CREATE_APP_DENIED(74),
    VIEW_TOO_LARGE(75),
    TOO_MANY_CRITERIA(76),
    REQUEST_LIMIT(77),
    DATA_LIMIT(78),
    OVERFLOW(80),
    NOT_FOUND(81),
    TIMEOUT(82),
    ACCESS_DENIED(82),
    DB_ERROR(84),
    SCHEMA_UPDATE_ERROR(85),
    INVALID_GROUP(87),
    TRY_LATER(100),
    TEMPORARILY_UNAVAILABLE(101),
    INVALID_REQUEST(102),
    INVALID_SRVR(103),
    HEAVY_TRAFFIC(104),
    TECHNICAL_ISSUES(105),
    INVALID_ROLE(110),
    USER_EXISTS(111),
    NO_USER_IN_ROLE(112),
    USER_HAS_ROLE(113),
    ADMIN_REQUIRED(114),
    UPGRADE_PLAN(150),
    EXPIRED_PLAN(151),
    APP_SUSPENDED(152),
    ;
    
    private int code = -1;
    
    private QuickBaseErrorCode(int code) {
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
    
    public static QuickBaseErrorCode valueOf(int code) {
        for (QuickBaseErrorCode errorCode : QuickBaseErrorCode.values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        return ERROR_CODE_NOT_RECOGNIZED;
    }
    
    @Override
    public String toString() {
        return name() + "(" + getCode() + ")";
    }
}
