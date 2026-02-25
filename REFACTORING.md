# CLI Refactoring Summary

## What Was Done

### 1. Created Service Layer
Extracted all business logic from Main into dedicated service classes:

- **OptService**: OPT loading, parsing, and validation
- **UiGeneratorService**: UI generation logic
- **InstanceGeneratorService**: Instance generation logic
- **InstanceValidatorService**: Instance validation logic
- **TransformService**: Format transformation logic
- **Adl2OptService**: ADL to OPT conversion logic

### 2. Created Command Layer
Implemented picocli commands for each CLI operation:

- **UiGenCommand**: `uigen` command
- **InstanceGenCommand**: `ingen` command
- **OptValidateCommand**: `optval` command
- **InstanceValidateCommand**: `inval` command
- **TransformCommand**: `trans` parent command
  - **TransformOptCommand**: `trans opt` subcommand
  - **TransformLocatableCommand**: `trans locatable` subcommand
- **Adl2OptCommand**: `adl2opt` command

### 3. Created New Main Entry Point
- **MainCli.groovy**: picocli application setup with all commands registered

### 4. Updated Build Configuration
- Added picocli dependency (4.7.5)
- Updated main class to MainCli
- Kept old Main as MainOld for reference

## Benefits

1. **Separation of Concerns**: CLI parsing separate from business logic
2. **Testability**: Services can be unit tested independently
3. **Maintainability**: Each command in its own class
4. **Consistency**: All commands follow same pattern with named options
5. **Better UX**: 
   - Named options instead of positional arguments
   - Auto-generated help text
   - Proper exit codes (0=success, 1=error)
6. **Extensibility**: Easy to add new commands

## Migration Guide

### Old CLI
```bash
java -jar sdk.jar uigen path/to/opt output/folder bs5 full
```

### New CLI
```bash
java -jar sdk.jar uigen -s path/to/opt -d output/folder --bootstrap bs5 --type full
```

All commands now use:
- `-s` or `--source` for input files/folders
- `-d` or `--dest` for output folders
- Named options for all parameters

## File Structure

```
src/main/groovy/com/cabolabs/openehr/opt/
├── MainCli.groovy (new entry point)
├── MainOld.groovy (backup of old Main)
├── cli/
│   ├── services/
│   │   ├── OptService.groovy
│   │   ├── UiGeneratorService.groovy
│   │   ├── InstanceGeneratorService.groovy
│   │   ├── InstanceValidatorService.groovy
│   │   ├── TransformService.groovy
│   │   └── Adl2OptService.groovy
│   └── commands/
│       ├── UiGenCommand.groovy
│       ├── InstanceGenCommand.groovy
│       ├── OptValidateCommand.groovy
│       ├── InstanceValidateCommand.groovy
│       ├── TransformCommand.groovy
│       ├── TransformOptCommand.groovy
│       ├── TransformLocatableCommand.groovy
│       └── Adl2OptCommand.groovy
```

## Next Steps

1. Build and test: `gradle clean fatJar`
2. Test each command with sample data
3. Update documentation (README.md)
4. Consider removing MainOld after verification
5. Add unit tests for service classes
