using FoodShareConnect.Api.Data;
using FoodShareConnect.Api.Models;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;

namespace FoodShareConnect.Api.Services;

/// <summary>
/// Runs at start-up. Admin accounts can never be self-registered, so the first time the app starts it
/// creates the FoodForward SA admin login (and a few sample donations so the dashboard is not empty).
/// </summary>
public static class SeedData
{
    public const string DefaultAdminEmail = "admin@foodforwardsa.org.za";
    public const string DefaultAdminPassword = "ChangeMe!2026";

    public static async Task EnsureAsync(FoodShareDbContext db, IPasswordHasher<User> hasher, IConfiguration config)
    {
        if (await db.Users.AnyAsync(u => u.Role == Roles.Admin)) return;

        var email = config["SeedAdmin:Email"] ?? DefaultAdminEmail;
        var password = config["SeedAdmin:Password"] ?? DefaultAdminPassword;

        var admin = new User { Email = email, DisplayName = "FoodForward SA Admin", Role = Roles.Admin };
        admin.PasswordHash = hasher.HashPassword(admin, password);
        db.Users.Add(admin);

        if (!await db.FoodDonations.AnyAsync())
        {
            var today = SaTime.Today;
            db.FoodDonations.Add(NewDonation("Woolworths Distribution Centre", "FRESH_PRODUCE", 200m, today.AddDays(6)));
            db.FoodDonations.Add(NewDonation("Pick n Pay Bakery Surplus", "BAKERY", 80m, today.AddDays(2)));
            db.FoodDonations.Add(NewDonation("Tiger Brands Warehouse", "DRY_GOODS_AND_STAPLES", 500m, today.AddDays(60)));
        }

        await db.SaveChangesAsync();
    }

    private static FoodDonation NewDonation(string donor, string category, decimal kg, DateTime expiry) => new FoodDonation
    {
        DonorName = donor,
        Category = category,
        QuantityKg = kg,
        RemainingQuantityKg = kg,
        ExpiryDate = expiry,
        Notes = "Sample stock",
        ReceivedAt = DateTime.UtcNow
    };
}
