/*
 * Copyright (c) 2015 Cartera Commerce Inc. All Rights reserved.
 * -------------------------------------------------------------------------------------------------
 *
 * File name  : QuickBaseExceptionCode.java
 * -------------------------------------------------------------------------------------------------
 *
 *
 * *************************************************************************************************
 */

package com.intuit.quickbase.api;

public enum QuickBaseExceptionCode {
      UNKNOWN_USER(21, "Unknown user"), 
      UNKNOWN_USERNAME_PASSWD(20, "Unknown username/password");
    
      private final int code;
      private final String description;

      private QuickBaseExceptionCode(int code, String description) {
        this.code = code;
        this.description = description;
      }

      public String getDescription() {
         return description;
      }

      public int getCode() {
         return code;
      }

      @Override
      public String toString() {
        return code + ": " + description;
      }
    
    

}
