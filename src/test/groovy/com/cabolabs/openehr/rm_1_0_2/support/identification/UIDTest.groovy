package com.cabolabs.openehr.rm_1_0_2.support.identification

import spock.lang.Specification
import spock.lang.Unroll

class UIDTest extends Specification {

    def "should create UUID with random value"() {
        when:
        def uuid = new UUID()

        then:
        uuid.value != null
        UID.isUUID(uuid.value.toString())
    }

    def "should create UUID from valid string"() {
        given:
        def validUUID = "550e8400-e29b-41d4-a716-446655440000"

        when:
        def uuid = new UUID(validUUID)

        then:
        uuid.value.toString() == validUUID
    }

    def "should fail creating UUID from invalid string"() {
        when:
        new UUID("invalid-uuid-string")

        then:
        thrown(IllegalArgumentException)
    }

    def "should create OID from valid string"() {
        given:
        def validOID = "2.16.840.1.113883.3"

        when:
        def oid = new OID(validOID)

        then:
        oid.value.toString() == validOID
    }

    def "should fail creating OID from invalid string"() {
        when:
        new OID("invalid.oid..string")

        then:
        thrown(IllegalArgumentException)
    }

    def "should create InternetId from valid string"() {
        given:
        def validDomain = "example.org"

        when:
        def internetId = new InternetId(validDomain)

        then:
        internetId.value.toString() == validDomain
    }

    def "should fail creating InternetId from invalid string"() {
        when:
        new InternetId("-invalid-.domain")

        then:
        thrown(IllegalArgumentException)
    }

    @Unroll
    def "UID.build should create correct instance for #description"() {
        when:
        def uid = UID.build(input)

        then:
        uid.class == expectedClass
        uid.value.toString() == input

        where:
        description      | input                                              | expectedClass
        "UUID"          | "550e8400-e29b-41d4-a716-446655440000"           | UUID
        "OID"           | "2.16.840.1.113883.3"                             | OID
        "Internet ID"   | "openehr.org"                                      | InternetId
    }

    def "UID.build should throw exception for invalid input"() {
        when:
        UID.build(input)

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains("does not match any known format")

        where:
        input << [
            null,
            "",
            "invalid-id",
            "not.a.valid..oid",
            "-invalid.domain",
            "550e8400-e29b-41d4-a716-INVALID"
        ]
    }

    @Unroll
    def "isUUID should correctly identify #description"() {
        expect:
        UID.isUUID(input) == expected

        where:
        description             | input                                              | expected
        "valid UUID"           | "550e8400-e29b-41d4-a716-446655440000"           | true
        "invalid UUID format"  | "550e8400-e29b-41d4-a716"                        | false
        "OID format"           | "2.16.840.1.113883.3"                            | false
        "Internet ID format"   | "openehr.org"                                     | false
        "null input"           | null                                              | false
        "empty string"         | ""                                                | false
    }

    @Unroll
    def "isOID should correctly identify #description"() {
        expect:
        UID.isOID(input) == expected

        where:
        description             | input                    | expected
        "valid OID"            | "2.16.840.1.113883.3"   | true
        "single number OID"    | "1"                      | true
        "invalid OID format"   | "2..16.840"             | false
        "UUID format"          | "550e8400-e29b-41d4-a716-446655440000" | false
        "Internet ID format"   | "openehr.org"           | false
        "null input"           | null                     | false
        "empty string"         | ""                       | false
    }

    @Unroll
    def "isInternetId should correctly identify #description"() {
        expect:
        UID.isInternetId(input) == expected

        where:
        description                 | input                 | expected
        "valid domain"             | "openehr.org"         | true
        "domain with hyphen"       | "open-ehr.org"       | true
        "single label domain"      | "localhost"           | true
        "invalid domain format"    | "-invalid.org"        | false
        "UUID format"              | "550e8400-e29b-41d4-a716-446655440000" | false
        "OID format"               | "2.16.840.1.113883.3" | false
        "null input"               | null                  | false
        "empty string"             | ""                    | false
    }

    @Unroll
    def "isUID should correctly identify #description"() {
        expect:
        UID.isUID(input) == expected

        where:
        description                 | input                                              | expected
        "valid UUID"               | "550e8400-e29b-41d4-a716-446655440000"           | true
        "valid OID"                | "2.16.840.1.113883.3"                            | true
        "valid Internet ID"        | "openehr.org"                                     | true
        "invalid format"           | "not-a-valid-id"                                  | false
        "null input"               | null                                              | false
        "empty string"             | ""                                                | false
    }
}
