using System.Globalization;
using System.Text.RegularExpressions;
using FoodShareConnect.Api.Dtos;

namespace FoodShareConnect.Api.Services;

/// <summary>Every input rule, enforced on the server whatever the browser sends.</summary>
public static class Validation
{
    public static readonly string[] Categories =
    {
        "FRESH_PRODUCE", "BAKERY", "DAIRY_AND_EGGS", "MEAT_AND_PROTEIN", "DRY_GOODS_AND_STAPLES", "BEVERAGES", "NON_FOOD_GROCERIES"
    };

    public static readonly string[] Provinces =
    {
        "Eastern Cape", "Free State", "Gauteng", "KwaZulu-Natal", "Limpopo", "Mpumalanga", "Northern Cape", "North West", "Western Cape"
    };

    private static readonly Regex PhoneRx = new(@"^(\+27|0)[6-8][0-9]{8}$", RegexOptions.Compiled);
    private static readonly Regex EmailRx = new(@"^[^\s@]+@[^\s@]+\.[^\s@]+$", RegexOptions.Compiled);
    private static readonly Regex NumberRx = new(@"^-?\d+(\.\d+)?$", RegexOptions.Compiled);
    private static readonly Regex WholeRx = new(@"^-?\d{1,9}$", RegexOptions.Compiled);

    public static string Clean(string? s) => (s ?? "").Trim();

    public static bool IsValidEmail(string? email)
    {
        var e = Clean(email);
        return e.Length <= 200 && EmailRx.IsMatch(e);
    }

    public static string? PasswordError(string? p)
    {
        if (p == null || p.Length < 8) return "Password must be at least 8 characters long.";
        if (p.Length > 100) return "Password must be 100 characters or fewer.";
        if (!p.Any(char.IsDigit)) return "Password must contain at least one digit.";
        if (!p.Any(char.IsUpper)) return "Password must contain at least one uppercase letter.";
        if (p.All(char.IsLetterOrDigit)) return "Password must contain at least one symbol (e.g. ! @ # $).";
        return null;
    }

    public static bool TryParseDecimal(string? s, out decimal value)
    {
        value = 0;
        var t = Clean(s).Replace(',', '.');
        return NumberRx.IsMatch(t) && decimal.TryParse(t, NumberStyles.Number, CultureInfo.InvariantCulture, out value);
    }

    public static bool TryParseWhole(string? s, out int value)
    {
        value = 0;
        var t = Clean(s);
        return WholeRx.IsMatch(t) && int.TryParse(t, NumberStyles.Integer, CultureInfo.InvariantCulture, out value);
    }

    /// <summary>Accepts only a real calendar date written as yyyy-MM-dd.</summary>
    public static bool TryParseDate(string? s, out DateTime date) =>
        DateTime.TryParseExact(Clean(s), "yyyy-MM-dd", CultureInfo.InvariantCulture, DateTimeStyles.None, out date);

    public static Dictionary<string, string> ValidateRegistration(OrganisationRegisterDto d, out int beneficiaries)
    {
        var e = new Dictionary<string, string>();
        var name = Clean(d.OrganisationName);
        if (name.Length < 2 || name.Length > 150) e["organisationName"] = "Enter your organisation's full name (2-150 characters).";
        var contact = Clean(d.ContactPersonName);
        if (contact.Length == 0 || contact.Length > 120) e["contactPersonName"] = "Enter a contact person's name.";
        if (!IsValidEmail(d.Email)) e["email"] = "Enter a valid email address.";
        if (!PhoneRx.IsMatch(Clean(d.PhoneNumber))) e["phoneNumber"] = "Enter a valid South African phone number, e.g. 0821234567.";
        var address = Clean(d.PhysicalAddress);
        if (address.Length == 0 || address.Length > 200) e["physicalAddress"] = "Enter your organisation's physical address.";
        if (!Provinces.Contains(Clean(d.Province))) e["province"] = "Select a province.";
        var npo = Clean(d.NpoRegistrationNumber);
        if (npo.Length == 0 || npo.Length > 50) e["npoRegistrationNumber"] = "Enter your NPO registration number.";
        if (!TryParseWhole(d.EstimatedBeneficiaries, out beneficiaries) || beneficiaries < 1 || beneficiaries > 1000000)
            e["estimatedBeneficiaries"] = "Enter a whole number between 1 and 1,000,000.";
        var pe = PasswordError(d.Password);
        if (pe != null) e["password"] = pe;
        if (d.Password != d.ConfirmPassword) e["confirmPassword"] = "Passwords do not match.";
        return e;
    }

    public static Dictionary<string, string> ValidateDonation(DonationCreateDto d, out decimal quantity, out DateTime expiry)
    {
        var e = new Dictionary<string, string>();
        var donor = Clean(d.DonorName);
        if (donor.Length == 0 || donor.Length > 150) e["donorName"] = "Enter the donor's name (up to 150 characters).";
        if (!Categories.Contains(Clean(d.Category))) e["category"] = "Select a category.";
        if (!TryParseDecimal(d.QuantityKg, out quantity) || quantity < 0.1m || quantity > 50000m)
            e["quantityKg"] = "Quantity must be between 0.1 and 50,000 kg.";
        if (!TryParseDate(d.ExpiryDate, out expiry) || expiry <= SaTime.Today)
            e["expiryDate"] = "Expiry date must be a valid date in the future.";
        if (Clean(d.Notes).Length > 300) e["notes"] = "Notes must be 300 characters or fewer.";
        return e;
    }

    public static Dictionary<string, string> ValidateRequest(FoodRequestCreateDto d, out decimal quantity, out int beneficiaries, out DateTime neededBy)
    {
        var e = new Dictionary<string, string>();
        if (!Categories.Contains(Clean(d.Category))) e["category"] = "Select a category.";
        if (!TryParseDecimal(d.QuantityKg, out quantity) || quantity < 0.1m || quantity > 5000m)
            e["quantityKg"] = "Quantity must be between 0.1 and 5,000 kg.";
        if (!TryParseWhole(d.Beneficiaries, out beneficiaries) || beneficiaries < 1 || beneficiaries > 100000)
            e["beneficiaries"] = "Enter a whole number between 1 and 100,000.";
        if (!TryParseDate(d.NeededBy, out neededBy) || neededBy <= SaTime.Today)
            e["neededBy"] = "Needed-by date must be a valid date in the future.";
        if (Clean(d.Motivation).Length > 400) e["motivation"] = "The motivation must be 400 characters or fewer.";
        return e;
    }
}
