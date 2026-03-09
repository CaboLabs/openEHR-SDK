# JSON Schema Constraints Update

## Summary
Added `minLength: 1` constraint to all required string properties in the openEHR API JSON schemas.

## Files Modified
1. `src/main/resources/json_schema/openehr_api_1.0.2_all.json` - 185 constraints added
2. `src/main/resources/json_schema/openehr_api_1.0.3_all.json` - 183 constraints added
3. `src/main/resources/json_schema/openehr_api_1.0.4_all.json` - 184 constraints added
4. `src/main/resources/json_schema/openehr_api_1.1.0_all.json` - 193 constraints added

## Changes Made
For every property that is:
- Listed in the `required` array
- Has `"type": "string"`

Added:
- `"minLength": 1` - prevents empty strings
- `"not": {"type": "null"}` - explicitly rejects null values

## Effect
This ensures that:
1. Required string properties cannot be empty strings ("")
2. Required string properties cannot be null

## Example
Before:
```json
"value": {
    "type": "string"
}
```

After:
```json
"value": {
    "type": "string",
    "minLength": 1,
    "not": {
        "type": "null"
    }
}
```

## Validation Behavior
- ✅ Valid: `{"value": "some text"}`
- ❌ Invalid: `{"value": ""}`
- ❌ Invalid: `{"value": null}`
- ❌ Invalid: `{}` (missing required property)
