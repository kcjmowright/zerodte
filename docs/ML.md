# ML Notes

## Data Cleanup

```bash

SELECT symbol, created FROM totalgex WHERE created::TIME < '08:30:00'::TIME;
SELECT symbol, created FROM totalgex WHERE created::TIME > '15:00:00'::TIME;

DELETE FROM totalgex WHERE created::TIME < '08:30:00'::TIME;
DELETE FROM totalgex WHERE created::TIME > '15:00:00'::TIME;
```

