/*
 * Copyright (c) 2009 Intuit Inc. All Rights reserved.
 * -------------------------------------------------------------------------------------------------
 *
 * File name  : QueryExecutionMode.java
 * -------------------------------------------------------------------------------------------------
 *
 *
 * *************************************************************************************************
 */

package com.intuit.quickbase.api.query;

/**
 * Enum to indicate whether a QuickBase query should be processed synchronously
 * or asynchronously.
 * 
 * @author Brad Brown
 */
public enum QueryExecutionMode {
    synchronous,
    asynchronous;
}
