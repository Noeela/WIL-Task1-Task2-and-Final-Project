using System.Security.Cryptography;
using System.Text.Json;
using FoodShareConnect.Api.Models;
using Microsoft.AspNetCore.DataProtection;

namespace FoodShareConnect.Api.Services;

public static class TokenPurposes
{
    public const string Access = "access";
    public const string TwoFactor = "2fa";
}

public class TokenPayload
{
    public int UserId { get; set; }
    public string Purpose { get; set; } = "";
}

/// <summary>
/// Creates and checks the sign-in tokens the browser sends with each request.
/// Tokens are encrypted, tamper-proof and time-limited using ASP.NET Core Data Protection.
/// </summary>
public class TokenService
{
    private static readonly TimeSpan AccessLifetime = TimeSpan.FromHours(8);
    private static readonly TimeSpan TwoFactorLifetime = TimeSpan.FromMinutes(5);
    private readonly ITimeLimitedDataProtector _protector;

    public TokenService(IDataProtectionProvider provider)
    {
        _protector = provider.CreateProtector("FoodShareConnect.SignInTokens.v1").ToTimeLimitedDataProtector();
    }

    public string CreateAccessToken(User user) => Protect(user, TokenPurposes.Access, AccessLifetime);

    /// <summary>A short-lived token issued after the password step, proving the person may now enter their 2FA code.</summary>
    public string CreatePendingTwoFactorToken(User user) => Protect(user, TokenPurposes.TwoFactor, TwoFactorLifetime);

    /// <summary>Returns the token contents, or null if it is invalid, tampered with, expired or for a different purpose.</summary>
    public TokenPayload? Read(string token, string expectedPurpose)
    {
        try
        {
            var json = _protector.Unprotect(token);
            var payload = JsonSerializer.Deserialize<TokenPayload>(json);
            return payload != null && payload.Purpose == expectedPurpose ? payload : null;
        }
        catch (CryptographicException) { return null; }
        catch (JsonException) { return null; }
    }

    private string Protect(User user, string purpose, TimeSpan lifetime)
    {
        var json = JsonSerializer.Serialize(new TokenPayload { UserId = user.Id, Purpose = purpose });
        return _protector.Protect(json, lifetime);
    }
}
