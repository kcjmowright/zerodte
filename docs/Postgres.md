# Postgres

## PG_DUMP

Here's how to run `pg_dump` inside a Docker container and save the output externally:

### Basic approach

The simplest method is to run `pg_dump` and redirect the output to a file on your host machine:

```bash
docker exec <container_name> pg_dump -U <username> <database_name> > /path/on/host/backup.sql
```

For example:
```bash
docker exec my-postgres pg_dump -U postgres mydb > ~/backups/mydb-backup.sql
```

### Using docker cp (alternative method)

If you prefer to create the dump file inside the container first, then copy it out:

```bash
# Create dump inside container
docker exec <container_name> pg_dump -U <username> <database_name> -f /tmp/backup.sql

# Copy it to host
docker cp <container_name>:/tmp/backup.sql /path/on/host/backup.sql
```

### Best practice: Using a volume mount

For regular backups, you can mount a host directory as a volume and dump directly into it:

```bash
docker exec <container_name> pg_dump -U <username> <database_name> -f /backups/backup.sql
```

This assumes you started your container with a volume mount like `-v /host/backup/path:/backups`.

### Compressed backup

To save space, use the custom format with compression:

```bash
docker exec my-postgres pg_dump -U postgres -Fc mydb > ~/backups/mydb-backup.dump
```

The `-Fc` flag creates a compressed custom format that can be restored with `pg_restore`.

### Including password

If your database requires a password, set the `PGPASSWORD` environment variable:

```bash
docker exec -e PGPASSWORD=yourpassword my-postgres pg_dump -U postgres mydb > backup.sql
```