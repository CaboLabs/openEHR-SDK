package com.cabolabs.openehr.rm_1_0_2.support.identification

import java.util.regex.Pattern

/**
 * Represents an Internet ID (subdomain) as defined in RFC 1034.
 *
 * An Internet ID consists of one or more labels separated by dots, where each label
 * must start and end with an alphanumeric character, and can contain hyphens and underscores.
 *
 * Example: "example.com", "sub.domain.org", "my-site_123.net"
 *
 * <domain> ::= <subdomain> | " "
 *
 * <subdomain> ::= <label> | <subdomain> "." <label>
 *
 * <label> ::= <letter> [ [ <ldh-str> ] <let-dig> ]
 *
 * <ldh-str> ::= <let-dig-hyp> | <let-dig-hyp> <ldh-str>
 *
 * <let-dig-hyp> ::= <let-dig> | "-"
 *
 * <let-dig> ::= <letter> | <digit>
 *
 * <letter> ::= any one of the 52 alphabetic characters A through Z in
 * upper case and a through z in lower case
 *
 * <digit> ::= any one of the ten digits 0 through 9
 */
class InternetId extends UID {

   // Pattern for a single label according to the grammar
   private static final String LABEL_PATTERN =
      /(?:[a-zA-Z0-9]|[a-zA-Z][a-zA-Z0-9_-]*[a-zA-Z0-9])/

   // Pattern for internet_id (subdomain)
   private static final String INTERNET_ID_PATTERN =
      /^${LABEL_PATTERN}(?:\.${LABEL_PATTERN})*$/

   private static final Pattern COMPILED_PATTERN =
      Pattern.compile(INTERNET_ID_PATTERN)

   String value

   InternetId()
   {
   }

   InternetId(String value)
   {
      if (!value) {
         throw new IllegalArgumentException("InternetId value cannot be null or empty")
      }

      if (!(value ==~ COMPILED_PATTERN)) {
         throw new IllegalArgumentException("Invalid internet_id format: '$value'")
      }

      this.value = value
   }

   String toString()
   {
      return this.value
   }
}
