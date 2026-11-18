using FoodShareConnect.Api.Data;
using FoodShareConnect.Api.Dtos;
using FoodShareConnect.Api.Models;
using FoodShareConnect.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace FoodShareConnect.Api.Controllers;

// "api/Organisations" is the original address of the registration form; "api/orgs" is the short form used by the admin screens.
[Route("api/orgs")]
[Route("api/Organisations")]
public class OrganisationsController : ApiControllerBase
{
    private readonly FoodShareDbContext _db;
    private readonly IPasswordHasher<User> _hasher;
    private readonly ILogger<OrganisationsController> _logger;

    public OrganisationsController(FoodShareDbContext db, IPasswordHasher<User> hasher, ILogger<OrganisationsController> logger)
    {
        _db = db;
        _hasher = hasher;
        _logger = logger;
    }

    /// <summary>
    /// Public registration. Saves the organisation AND its login to Azure SQL in one step
    /// (both are saved together or neither is). The organisation starts as PENDING_REVIEW.
    /// </summary>
    [HttpPost]
    [AllowAnonymous]
    public async Task<IActionResult> Register([FromBody] OrganisationRegisterDto dto)
    {
        var errors = Validation.ValidateRegistration(dto, out var beneficiaries);
        if (errors.Count > 0) return FieldErrors(errors);

        var email = Validation.Clean(dto.Email);
        var npo = Validation.Clean(dto.NpoRegistrationNumber);

        var emailTaken = await _db.BeneficiaryOrganisations.AnyAsync(o => o.Email.ToLower() == email.ToLower())
                      || await _db.Users.AnyAsync(u => u.Email.ToLower() == email.ToLower());
        if (emailTaken)
            return FieldErrors(new Dictionary<string, string> { ["email"] = "An account already exists for this email address." });

        var npoTaken = await _db.BeneficiaryOrganisations.AnyAsync(o => o.NpoRegistrationNumber.ToLower() == npo.ToLower());
        if (npoTaken)
            return FieldErrors(new Dictionary<string, string> { ["npoRegistrationNumber"] = "An organisation with this NPO registration number is already registered." });

        var organisation = new BeneficiaryOrganisation
        {
            OrganisationName = Validation.Clean(dto.OrganisationName),
            ContactPersonName = Validation.Clean(dto.ContactPersonName),
            Email = email,
            PhoneNumber = Validation.Clean(dto.PhoneNumber),
            PhysicalAddress = Validation.Clean(dto.PhysicalAddress),
            Province = Validation.Clean(dto.Province),
            NpoRegistrationNumber = npo,
            EstimatedBeneficiaries = beneficiaries,
            VettingStatus = VettingStatuses.PendingReview,
            RegisteredAt = DateTime.UtcNow
        };

        var user = new User
        {
            Email = email,
            DisplayName = organisation.ContactPersonName,
            Role = Roles.BeneficiaryOrg,
            BeneficiaryOrganisation = organisation
        };
        user.PasswordHash = _hasher.HashPassword(user, dto.Password!);

        _db.Users.Add(user);   // also adds the organisation through the relationship; one SaveChanges = one database transaction
        try
        {
            await _db.SaveChangesAsync();
        }
        catch (DbUpdateException ex)
        {
            _logger.LogError(ex, "Could not save registration for {Email}", email);
            return Fail(409, "We could not save your registration. If you have already registered, please sign in; otherwise try again.");
        }

        return Ok(new { message = "Registration successful", id = organisation.Id });
    }

    /// <summary>Admin: every registered organisation, optionally filtered by vetting status.</summary>
    [HttpGet]
    [Authorize(Roles = Roles.Admin)]
    public async Task<IActionResult> List([FromQuery] string? status)
    {
        var query = _db.BeneficiaryOrganisations.AsNoTracking().AsQueryable();
        if (!string.IsNullOrEmpty(status) && VettingStatuses.All.Contains(status))
            query = query.Where(o => o.VettingStatus == status);

        var orgs = await query.OrderByDescending(o => o.RegisteredAt).ThenByDescending(o => o.Id).ToListAsync();
        return Ok(orgs.Select(Mappers.ToDto));
    }

    [HttpGet("{id:int}")]
    [Authorize(Roles = Roles.Admin)]
    public async Task<IActionResult> Get(int id)
    {
        var org = await _db.BeneficiaryOrganisations.AsNoTracking().FirstOrDefaultAsync(o => o.Id == id);
        return org == null ? Fail(404, "Organisation not found.") : Ok(Mappers.ToDto(org));
    }

    [HttpGet("{id:int}/requests")]
    [Authorize(Roles = Roles.Admin)]
    public async Task<IActionResult> Requests(int id)
    {
        var requests = await _db.FoodRequests.AsNoTracking()
            .Include(r => r.BeneficiaryOrganisation)
            .Where(r => r.BeneficiaryOrganisationId == id)
            .OrderByDescending(r => r.SubmittedAt).ThenByDescending(r => r.Id)
            .ToListAsync();
        return Ok(requests.Select(Mappers.ToDto));
    }

    /// <summary>Admin: approve, reject or suspend an organisation (saved to Azure SQL).</summary>
    [HttpPut("{id:int}/vet")]
    [Authorize(Roles = Roles.Admin)]
    public async Task<IActionResult> Vet(int id, [FromBody] VetDto dto)
    {
        var status = Validation.Clean(dto.Status);
        if (!VettingStatuses.All.Contains(status)) return Fail(400, "Select a valid vetting status.");
        var note = Validation.Clean(dto.Note);
        if (note.Length > 500) return Fail(400, "The note must be 500 characters or fewer.");

        var org = await _db.BeneficiaryOrganisations.FirstOrDefaultAsync(o => o.Id == id);
        if (org == null) return Fail(404, "Organisation not found.");

        org.VettingStatus = status;
        org.AdminNote = note;
        await _db.SaveChangesAsync();
        return Ok(Mappers.ToDto(org));
    }
}
