# MEDIATRON merge notes

## What this merged candidate keeps
- **Frontend and dashboard/admin UI** from the GitHub version.
- **Publications import backend** from the PC version.
- **PhD students backend endpoints** added into the PC backend.
- **Flyway migrations continue from the PC version** with a new `V5__add_phd_students.sql` migration.

## Important choices made
- The existing backend `members.position` field was kept in the database and exposed to the frontend as **grade**.
- Member email remains stored on the linked `users` table and is exposed to the frontend through DTOs.
- Docker and application configuration were changed to use **environment variables** instead of hard-coded Neon/JWT secrets.

## What you should verify locally
1. `backend/Lab_Management` compiles with Maven.
2. Login works against your Neon database.
3. Admin can open `/members`, create/edit researchers, and manage PhD students.
4. Publication import still works for a member with ORCID / scholar data.
5. Run Flyway migration `V5__add_phd_students.sql` on Neon.

## Recommended Git workflow
1. Create a new branch from the GitHub repo, for example `merge/publications-and-dashboard`.
2. Copy the contents of this merged candidate into that branch.
3. Run local validation.
4. Push and open a pull request instead of merging directly into main.
