# MEDIATRON Grafana Statistics

This feature provisions Grafana against the PostgreSQL datasource already used by the MEDIATRON platform and exposes a public React statistics page at `/statistics`.

## What is included

- Docker Compose Grafana service on `http://localhost:3000`
- Provisioned PostgreSQL datasource with UID `mediatron-postgres`
- Provisioned dashboard:
  - `MEDIATRON Publication Statistics`
  - UID: `mediatron-publication-statistics`
- SQL query reference: [infra/grafana/sql/mediatron_statistics_queries.sql](../infra/grafana/sql/mediatron_statistics_queries.sql)
- React public page embedding Grafana through iframes

## Grafana setup

Start the stack:

```bash
docker compose up -d grafana
```

Grafana will be available at:

```text
http://localhost:3000
```

Default credentials:

- Username: `admin`
- Password: `admin123!`

Anonymous viewer access is enabled so the React public page can embed the dashboards without requiring login.

## Refresh policy

- Dashboard refresh interval: `24h`
- Provisioning update interval: `86400s`

This aligns with the requirement to refresh statistics every 24 hours while keeping PostgreSQL load predictable.

## Performance notes

- Dashboard queries are aggregated in PostgreSQL and use existing publication indexes.
- Extra indexes were added in `V24__statistics_indexes.sql` for:
  - `publications(status, publication_year)`
  - `publications(status, type, publication_year)`
  - `members_publications(publication_id)`
- The React page lazy-loads the secondary iframe panels.

## Advanced Grafana API integration later

If you later want tighter React integration than iframe embeds, the next step is to use:

1. Grafana service accounts and signed API calls
2. Dashboard snapshot or rendered image APIs for server-side caching
3. Short-lived embed tokens or reverse-proxy based authentication
4. A backend stats proxy to centralize access control and query-level caching

For now, the iframe approach keeps the implementation simple, public, and production-friendly.
