# WIL-Task1-Task2-and-Final-Project

| Admin dashboard action | Azure table | API endpoint |
|---|---|---|
| Organisation registers | BeneficiaryOrganisations + **Users** (new) | `POST /api/Organisations` |
| Vet an organisation (approve / reject / suspend, with note) | BeneficiaryOrganisations (VettingStatus, AdminNote) | `PUT /api/orgs/{id}/vet` |
| Capture a donation | FoodDonations | `POST /api/donations` |
| Organisation submits a food request | FoodRequests | `POST /api/my/requests` |
| Approve a request (stock is deducted) | FoodRequests + FoodDonations, in one transaction | `POST /api/requests/{id}/approve` |
| Decline a request | FoodRequests | `POST /api/requests/{id}/decline` |
| Admin dashboard numbers | counted live from the tables | `GET /api/stats` |
| Admin two-factor authentication | Users (TwoFactorEnabled / TwoFactorSecret) | `POST /api/auth/2fa/setup` and `/confirm` |
| Sign in (with 5-attempt lockout) | Users (hashed password, FailedAttempts, LockedUntilUtc) | `POST /api/auth/login` |

The page footer shows "Database: Azure SQL (FoodShareConnectDB) - connected" so you can show your lecturer the data is really in Azure.

 First run
1. Open the solution in Visual Studio and press **F5** (it now opens the website, not Swagger).
2. On start-up the app automatically (a) applies the new migration `AddUsersAndAdminFields` to your Azure database
   (adds the **Users** table and the AdminNote / RegisteredAt columns), and (b) creates the admin login and three sample donations.
3. Sign in as admin: admin@foodforwardsa.org.za / ChangeMe!2026 (change it via `SeedAdmin:Password` in appsettings).
4. In the Azure portal > your database > Query editor, run `SELECT * FROM Users;` `SELECT * FROM FoodDonations;` etc. to see the records appear.

If Visual Studio complains about the migration files, delete the two files named `20261016120000_AddUsersAndAdminFields*.cs`
in the Migrations folder, then in Package Manager Console run `Add-Migration AddUsersAndAdminFields` and `Update-Database`.

## Old test organisations (registered before logins were saved)
Organisations registered earlier have a row in Azure but no login, so they cannot sign in and their emails show as "already exists".
Either register them again with a new email, or (after the first run) run this in the Azure Query editor to remove the old test rows:





