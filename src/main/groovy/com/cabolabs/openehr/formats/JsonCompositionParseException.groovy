package com.cabolabs.openehr.formats

class JsonParseException extends RuntimeException {

   JsonParseException(String message)
   {
      super(message)
   }

   JsonParseException(String message, Throwable cause)
   {
      super(message, cause)
   }
}