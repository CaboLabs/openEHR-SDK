package com.cabolabs.openehr.formats

class XmlParseException extends RuntimeException {

   XmlParseException(String message)
   {
      super(message)
   }

   XmlParseException(String message, Throwable cause)
   {
      super(message, cause)
   }
}