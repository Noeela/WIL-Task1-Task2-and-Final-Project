using FoodShareConnect.Api.Data;
using FoodShareConnect.Api.Dtos;
using FoodShareConnect.Api.Models;
using FoodShareConnect.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace FoodShareConnect.Api.Controllers;

[Route("api/auth")]
public class AuthController : ApiControllerBase
{
    private const int MaxFailedAttempts = 5;
    private static readonly TimeSpan LockDuration = TimeSpan.FromMinutes(10);
    private const string LockMessage = "This account is temporarily locked due to repeated failed sign-in attempts. Please try again in 10 minutes.";

    private readonly FoodShareDbContext _db;
    private readonly IPasswordHasher<User> _hasher;
    private readonly TokenService _tokens;

    public AuthController(FoodShareDbContext db, IPasswordHasher<User> hasher, TokenService tokens)
    {
        _db = db;
        _hasher = hasher;
        _tokens = tokens;
    }

    /// <summary>Step 1 of sign-in: email + password. Admin accounts with 2FA get a short-lived token for step 2.</summary>
    [HttpPost("login")]
    [AllowAnonymous]
    public async Task<IActionResult> Login([FromBody] LoginDto dto)
    {
        var email = Validation.Clean(dto.Email);
        if (!Validation.IsValidEmail(email) || string.IsNullOrEmpty(dto.Password))
            return Fail(400, "Please enter your email and password.");

        var user = await _db.Users.FirstOrDefaultAsync(u => u.Email.ToLower() == email.ToLower());
        if (user == null)
        {
            _hasher.HashPassword(new User(), "timing-equaliser");   // same work as a real check, so the response time does not reveal whether the email exists
            return Fail(401, "Invalid email or password.");
        }

        if (IsLocked(user)) return Fail(423, LockMessage);

        var check = _hasher.VerifyHashedPassword(user, user.PasswordHash, dto.Password);
        if (check == PasswordVerificationResult.Failed)
        {
            var nowLocked = await RecordFailureAsync(user);
            return nowLocked ? Fail(423, LockMessage) : Fail(401, "Invalid email or password.");
        }

        if (user.Role == Roles.Admin && user.TwoFactorEnabled)
            return Ok(new { status = "2fa", pendingToken = _tokens.CreatePendingTwoFactorToken(user) });

        await ClearFailuresAsync(user);
        return Ok(new { status = "ok", token = _tokens.CreateAccessToken(user), user = Mappers.ToDto(user) });
    }

    /// <summary>Step 2 of sign-in for Admin accounts that have two-factor authentication switched on.</summary>
    [HttpPost("2fa")]
    [AllowAnonymous]
    public async Task<IActionResult> CompleteTwoFactor([FromBody] TwoFactorLoginDto dto)
    {
        var payload = _tokens.Read(dto.PendingToken ?? "", TokenPurposes.TwoFactor);
        if (payload == null) return Fail(410, "Your sign-in session expired. Please sign in again.");

        var user = await _db.Users.FirstOrDefaultAsync(u => u.Id == payload.UserId);
        if (user == null) return Fail(410, "Please sign in again.");
        if (IsLocked(user)) return Fail(423, LockMessage);

        if (!TotpService.Verify(user.TwoFactorSecret, dto.Code))
        {
            var nowLocked = await RecordFailureAsync(user);
            return nowLocked ? Fail(423, LockMessage) : Fail(401, "Invalid authenticator code. Please try again.");
        }

        await ClearFailuresAsync(user);
        return Ok(new { status = "ok", token = _tokens.CreateAccessToken(user), user = Mappers.ToDto(user) });
    }

    [HttpGet("me")]
    [Authorize]
    public async Task<IActionResult> Me()
    {
        var user = await _db.Users.AsNoTracking().FirstOrDefaultAsync(u => u.Id == CurrentUserId);
        return user == null ? Fail(401, "Please sign in to continue.") : Ok(Mappers.ToDto(user));
    }

    /// <summary>Starts two-factor set-up: returns the secret to type into an authenticator app.</summary>
    [HttpPost("2fa/setup")]
    [Authorize(Roles = Roles.Admin)]
    public async Task<IActionResult> SetupTwoFactor()
    {
        var user = await _db.Users.FirstOrDefaultAsync(u => u.Id == CurrentUserId);
        if (user == null) return Fail(401, "Please sign in to continue.");

        if (string.IsNullOrEmpty(user.TwoFactorSecret) || !user.TwoFactorEnabled)
        {
            user.TwoFactorSecret = TotpService.GenerateSecret();
            user.TwoFactorEnabled = false;
            await _db.SaveChangesAsync();
        }
        return Ok(new { secret = user.TwoFactorSecret, uri = TotpService.BuildUri(user.TwoFactorSecret!, user.Email), enabled = user.TwoFactorEnabled });
    }

    [HttpPost("2fa/confirm")]
    [Authorize(Roles = Roles.Admin)]
    public async Task<IActionResult> ConfirmTwoFactor([FromBody] CodeDto dto)
    {
        var user = await _db.Users.FirstOrDefaultAsync(u => u.Id == CurrentUserId);
        if (user == null || string.IsNullOrEmpty(user.TwoFactorSecret)) return Fail(400, "Start the two-factor setup again.");

        if (!TotpService.Verify(user.TwoFactorSecret, dto.Code))
            return BadRequest(new { message = "Verification code is incorrect.", fieldErrors = new Dictionary<string, string> { ["code"] = "Verification code is incorrect." } });

        user.TwoFactorEnabled = true;
        await _db.SaveChangesAsync();
        return Ok(new { ok = true });
    }

    private static bool IsLocked(User user) => user.LockedUntilUtc.HasValue && user.LockedUntilUtc.Value > DateTime.UtcNow;

    /// <summary>Counts a failed attempt; after five in a row the account is locked for ten minutes.</summary>
    private async Task<bool> RecordFailureAsync(User user)
    {
        user.FailedAttempts++;
        var locked = user.FailedAttempts >= MaxFailedAttempts;
        if (locked)
        {
            user.LockedUntilUtc = DateTime.UtcNow.Add(LockDuration);
            user.FailedAttempts = 0;
        }
        await _db.SaveChangesAsync();
        return locked;
    }

    private async Task ClearFailuresAsync(User user)
    {
        if (user.FailedAttempts == 0 && user.LockedUntilUtc == null) return;
        user.FailedAttempts = 0;
        user.LockedUntilUtc = null;
        await _db.SaveChangesAsync();
    }
}
