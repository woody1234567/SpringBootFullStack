---
name: oracle-naming-convention
description: Oracle Database object naming convention (TB_/SP_/FN_/PK_ prefixes, UPPER_SNAKE_CASE). Use this skill WHENEVER creating or modifying ANY Oracle database object — tables, views, indexes, constraints, procedures, functions, packages, sequences, triggers, materialized views, jobs, or types — including writing CREATE/ALTER DDL, PL/SQL, or reviewing Oracle SQL naming. Applies even if the user doesn't mention "naming convention" explicitly.
---

# Oracle Database Naming Convention

## Purpose

This skill defines the Oracle Database object naming convention for this project.

Whenever creating or modifying any Oracle database object, follow this specification exactly. Consistent naming lets anyone identify an object's type and owning table at a glance, and keeps generated SQL compatible with the team's existing schema.

---

## General Rules

- Keep database object names <= 60 characters. Never exceed Oracle naming limits.
- Use UPPER_SNAKE_CASE for all database object names.
- Do not use camelCase.
- Do not use spaces.
- Do not use Chinese characters.
- Use singular nouns whenever possible (`TB_USER`, not `TB_USERS`).
- Avoid abbreviations unless they are industry standard.
- Keep names meaningful and concise.
- Procedure names start with a verb.
- Function names describe the returned value.

---

## Object Prefix Rules

Every database object MUST begin with the prefix for its type:

| Object Type | Prefix |
|-------------|--------|
| Table | TB |
| View | VW *(temporarily not recommended)* |
| Index | IX |
| Primary Key | PK |
| Unique Key | UK |
| Foreign Key | FK |
| Trigger | TR *(currently discouraged)* |
| Procedure | SP |
| Package | PG |
| Function | FN |
| Directory | DR |
| Sequence | SQ |
| Object Type | TO |
| Table Type | TT |
| Job | JB |
| Materialized View | MV |

Never invent your own prefixes, never omit prefixes, and never mix different naming styles in the same schema.

---

## Naming Formats

### Table

```text
TB_<TABLE_NAME>
```

Examples: `TB_USER`, `TB_CUSTOMER`, `TB_ORDER`, `TB_EMPLOYEE`

### Procedure

Procedures perform actions, so the name starts with a verb:

```text
SP_<ACTION>_<OBJECT>
```

Examples: `SP_GET_USER`, `SP_CREATE_ORDER`, `SP_UPDATE_CUSTOMER`, `SP_DELETE_EMPLOYEE`, `SP_GET_BILLAMT`

### Function

Functions return a value, so the name describes what is returned:

```text
FN_<ACTION>_<OBJECT>
```

Examples: `FN_GET_USER_NAME`, `FN_CALCULATE_TAX`, `FN_IS_VALID_USER`

### Package

```text
PG_<MODULE>
```

Examples: `PG_USER`, `PG_ORDER`, `PG_FINANCE`

### Sequence

Name the sequence after the table it serves:

```text
SQ_<TABLE_NAME>
```

Examples: `SQ_USER`, `SQ_ORDER`

### Materialized View

```text
MV_<NAME>
```

Examples: `MV_MONTHLY_REPORT`, `MV_USER_STATISTICS`

### Directory

```text
DR_<NAME>
```

Examples: `DR_IMPORT`, `DR_EXPORT`

### Object Type

```text
TO_<NAME>
```

Example: `TO_ADDRESS`

### Table Type

```text
TT_<NAME>
```

Example: `TT_ADDRESS`

### Job

```text
JB_<JOB_NAME>
```

Examples: `JB_DAILY_BACKUP`, `JB_SYNC_CUSTOMER`

---

## Constraint and Index Naming

Constraint names must align with their owning table: take the table name without the `TB_` prefix and apply the constraint prefix.

For table `TB_BASE`:

```text
PK_BASE    -- Primary Key
UK_BASE    -- Unique Key
FK_BASE    -- Foreign Key
TR_BASE    -- Trigger
IX_BASE    -- Index
```

### Multiple Constraints on the Same Table

When a table has more than one index, unique key, or foreign key, append a numeric serial suffix (`_1`, `_2`, ...) so each name stays unique while remaining traceable to its table:

```text
IX_<TABLE_NAME>_1
IX_<TABLE_NAME>_2

UK_<TABLE_NAME>_1
UK_<TABLE_NAME>_2

FK_<TABLE_NAME>_1
FK_<TABLE_NAME>_2
```

Examples:

```text
IX_USER_1
IX_USER_2

UK_USER_1

FK_ORDER_1
FK_ORDER_2
```

---

## Requirements

When generating Oracle SQL:

- ALWAYS use the prefixes defined in this document.
- ALWAYS keep object names <= 60 characters.
- ALWAYS keep Primary Key / Unique Key / Foreign Key names aligned with their table name.
- ALWAYS append numeric suffixes (`_1`, `_2`, ...) when multiple IX / UK / FK objects exist on one table.
- NEVER invent your own prefixes.
- NEVER omit prefixes.
- NEVER mix different naming styles.

---

## Examples

### Good

```text
✅ TB_CUSTOMER
✅ SP_CREATE_ORDER
✅ FN_GET_BALANCE
```

### Bad

```text
❌ TbCustomer        -- camelCase, wrong case style
❌ customer_table    -- lowercase, suffix instead of prefix
❌ UserTable         -- no prefix, PascalCase
❌ GetBalance        -- no FN_ prefix, camelCase
```

### Full Example

```sql
CREATE TABLE TB_BASE
(
    ID NUMBER PRIMARY KEY,
    NAME VARCHAR2(100)
);
```

Related objects for `TB_BASE`:

```text
Primary Key: PK_BASE
Index:       IX_BASE_1
Sequence:    SQ_BASE
Package:     PG_BASE
Procedure:   SP_GET_BILLAMT
Function:    FN_GET_USER_NAME
```

---

## Summary

| Object | Format |
|--------|--------|
| Table | `TB_<NAME>` |
| View | `VW_<NAME>` |
| Index | `IX_<NAME>[_N]` |
| Primary Key | `PK_<TABLE>` |
| Unique Key | `UK_<TABLE>[_N]` |
| Foreign Key | `FK_<TABLE>[_N]` |
| Trigger | `TR_<TABLE>` |
| Procedure | `SP_<ACTION>_<OBJECT>` |
| Package | `PG_<MODULE>` |
| Function | `FN_<ACTION>_<OBJECT>` |
| Directory | `DR_<NAME>` |
| Sequence | `SQ_<NAME>` |
| Object Type | `TO_<NAME>` |
| Table Type | `TT_<NAME>` |
| Job | `JB_<NAME>` |
| Materialized View | `MV_<NAME>` |
