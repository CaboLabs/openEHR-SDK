# openEHR SDK CLI

Refactored CLI using picocli for better usability and maintainability.

## Build

```bash
gradle clean fatJar
```

## Commands

### uigen - Generate UI from OPT

Generate HTML forms from Operational Templates.

```bash
java -jar build/libs/opt-x.y.z.jar uigen -s path/to/template.opt -d output/folder [--bootstrap bs5] [--type full]
```

Options:
- `-s, --source`: Path to OPT file (required)
- `-d, --dest`: Destination folder (required)
- `--bootstrap`: Bootstrap version: bs4 or bs5 (default: bs5)
- `--type`: Generation type: full (complete HTML page) or form (form only) (default: full)

### ingen - Generate Instances from OPT

Generate XML or JSON instances with random data from Operational Templates.

```bash
java -jar build/libs/opt-x.y.z.jar ingen -s path/to/template.opt -d output/folder [-n 1] [-f json] [-t locatable] [--with-participations] [--flavor rm]
```

Options:
- `-s, --source`: Path to OPT file or folder (required)
- `-d, --dest`: Destination folder (required)
- `-n, --amount`: Number of instances to generate (default: 1)
- `-f, --format`: Output format: json or xml (default: json)
- `-t, --type`: Type: version or locatable (default: locatable)
- `--with-participations`: Add participations (only for COMPOSITION templates)
- `--flavor`: Data structure flavor: rm or api (default: rm)

### optval - Validate OPT

Validate an Operational Template against XSD schema.

```bash
java -jar build/libs/opt-x.y.z.jar optval -s path/to/template.opt
```

Options:
- `-s, --source`: Path to OPT file (required)

### inval - Validate Instances

Validate XML or JSON instances against schemas.

```bash
java -jar build/libs/opt-x.y.z.jar inval -s path/to/instance [--flavor rm] [--semantic]
```

Options:
- `-s, --source`: Path to instance file or folder (required)
- `--flavor`: Data structure flavor: rm or api (default: rm)
- `--semantic`: Perform semantic validation against OPT

### trans - Transform Formats

Transform OPT or Locatable between formats.

#### Transform OPT (XML to JSON)

```bash
java -jar build/libs/opt-x.y.z.jar trans opt -s path/to/template.opt -d output/folder
```

#### Transform Locatable (XML ↔ JSON)

```bash
java -jar build/libs/opt-x.y.z.jar trans locatable -s path/to/composition.xml -d output/folder
```

Options:
- `-s, --source`: Path to source file (required)
- `-d, --dest`: Destination folder (required)

### adl2opt - Generate OPT from ADL

Generate an Operational Template from an ADL archetype.

```bash
java -jar build/libs/opt-x.y.z.jar adl2opt -s path/to/archetype.adl -d output/folder
```

Options:
- `-s, --source`: Path to ADL file (required)
- `-d, --dest`: Destination folder (required)

## Help

Get help for any command:

```bash
java -jar build/libs/opt-x.y.z.jar --help
java -jar build/libs/opt-x.y.z.jar uigen --help
java -jar build/libs/opt-x.y.z.jar ingen --help
```

## Architecture

The CLI has been refactored with:

- **Service Layer** (`cli/services/`): Business logic separated from CLI
- **Command Layer** (`cli/commands/`): picocli command implementations
- **Main Entry Point** (`MainCli.groovy`): picocli application setup

This separation allows for:
- Better testability
- Cleaner code organization
- Easier maintenance
- Consistent error handling
