using System.Security.Claims;
using System.Text.Encodings.Web;
using FoodShareConnect.Api.Data;
using Microsoft.AspNetCore.Authentication;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;

namespace FoodShareConnect.Api.Services;

/// <summary>
/// Reads "Authorization: Bearer ..." on every API request, checks the token AND that the account still exists,
/// and tells ASP.NET Core who the user is and which role they have (so [Authorize(Roles = "ADMIN")] works).
/// </summary>
public class TokenAuthenticationHandler : AuthenticationHandler<AuthenticationSchemeOptions>
{
    public const string SchemeName = "FoodShareToken";

    private readonly TokenService _tokens;
    private readonly FoodShareDbContext _db;

    public TokenAuthenticationHandler(
        IOptionsMonitor<AuthenticationSchemeOptions> options,
        ILoggerFactory logger,
        UrlEncoder encoder,
        TokenService tokens,
        FoodShareDbContext db) : base(options, logger, encoder)
    {
        _tokens = tokens;
        _db = db;
    }

    protected override async Task<AuthenticateResult> HandleAuthenticateAsync()
    {
        var header = Request.Headers.Authorization.ToString();
        if (!header.StartsWith("Bearer ", StringComparison.OrdinalIgnoreCase))
            return AuthenticateResult.NoResult();

        var payload = _tokens.Read(header.Substring(7).Trim(), TokenPurposes.Access);
        if (payload == null)
            return AuthenticateResult.Fail("Your session has expired. Please sign in again.");

        var user = await _db.Users.AsNoTracking().FirstOrDefaultAsync(u => u.Id == payload.UserId);
        if (user == null)
            return AuthenticateResult.Fail("Unknown account.");

        var claims = new List<Claim>
        {
            new Claim(ClaimTypes.NameIdentifier, user.Id.ToString()),
            new Claim(ClaimTypes.Name, user.DisplayName),
            new Claim(ClaimTypes.Email, user.Email),
            new Claim(ClaimTypes.Role, user.Role)
        };
        if (user.BeneficiaryOrganisationId.HasValue)
            claims.Add(new Claim("orgId", user.BeneficiaryOrganisationId.Value.ToString()));

        var principal = new ClaimsPrincipal(new ClaimsIdentity(claims, SchemeName));
        return AuthenticateResult.Success(new AuthenticationTicket(principal, SchemeName));
    }

    protected override async Task HandleChallengeAsync(AuthenticationProperties properties)
    {
        Response.StatusCode = StatusCodes.Status401Unauthorized;
        await Response.WriteAsJsonAsync(new { message = "Please sign in to continue." });
    }

    protected override async Task HandleForbiddenAsync(AuthenticationProperties properties)
    {
        Response.StatusCode = StatusCodes.Status403Forbidden;
        await Response.WriteAsJsonAsync(new { message = "You do not have permission to do that." });
    }
}
