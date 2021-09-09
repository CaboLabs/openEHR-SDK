package com.cabolabs.openehr.formats

class JsonCompositionParseException extends RuntimeException {

   JsonCompositionParseException(String message)
   {
      super(message)
   }

   JsonCompositionParseException(String message, Throwable cause)
   {
      super(message, cause)
   }
}