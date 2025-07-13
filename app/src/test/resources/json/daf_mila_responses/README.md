# DafMila JSON Test Files

This directory contains JSON test files for testing the `DafMilaResponse` parsing functionality.

## Files

- `complex_munnahim_list.json` - Complex case with full MunnahimList structure that was previously failing
- `simple_response.json` - Simple test case with basic structure
- `null_munnahim_list.json` - Test case with null MunnahimList

## Adding New Test Cases

To add a new test case:

1. Create a new `.json` file in this directory
2. Add your JSON content (should be a valid `DafMilaResponse` structure)
3. The test `testParseDafMilaJsonFromAllFiles()` will automatically pick up and test your new file

## Test Structure

The test will automatically:
- Load all `.json` files from this directory
- Parse each one using the `parseDafMilaJson` method
- Verify that parsing succeeds
- Validate basic structure requirements

## File Format

Each JSON file should represent the `data` field content from a `DafMilaAjaxResponse`, not the full AJAX response wrapper.

Example structure:
```json
{
    "ReuGam": "",
    "Koteret": "...",
    "MillonHaHoveList": [...],
    "ErekhHismagList": [...],
    "HalufonList": [...],
    "MunnahimList": {...},
    "IndexName": "...",
    "related_posts": [...],
    "related_hahlata": [...],
    "acf_categories": null
}
``` 