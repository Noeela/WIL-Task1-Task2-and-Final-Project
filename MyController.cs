using FoodShareConnect.Api.Data;
using FoodShareConnect.Api.Dtos;
using FoodShareConnect.Api.Models;
using FoodShareConnect.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace FoodShareConnect.Api.Controllers;

/// <summary>Everything a Beneficiary Organisation can see and do. The organisation always comes from the signed-in account, never from the request.</summary>
[Route("api/my")]
[Authorize(Roles = Roles.BeneficiaryOrg)]
public class MyController : ApiControllerBase
{
    private readonly FoodShareDbContext _db;

    public MyController(FoodShareDbContext db)
    {
        _db = db;
    }

    [HttpGet("org")]
    public async Task<IActionResult> MyOrganisation()
    {
        var orgId = CurrentOrgId;
        if (orgId == null) return Fail(404, "Organisation not found.");
        var org = await _db.BeneficiaryOrganisations.AsNoTracking().FirstOrDefaultAsync(o => o.Id == orgId.Value);
        return org == null ? Fail(404, "Organisation not found.") : Ok(Mappers.ToDto(org));
    }

    [HttpGet("requests")]
    public async Task<IActionResult> MyRequests()
    {
        var orgId = CurrentOrgId;
        if (orgId == null) return Ok(Array.Empty<FoodRequestDto>());
        var list = await _db.FoodRequests.AsNoTracking()
            .Include(r => r.BeneficiaryOrganisation)
            .Where(r => r.BeneficiaryOrganisationId == orgId.Value)
            .OrderByDescending(r => r.SubmittedAt).ThenByDescending(r => r.Id)
            .ToListAsync();
        return Ok(list.Select(Mappers.ToDto));
    }

    /// <summary>Submit a food request. Only organisations that FoodForward SA has approved may do this.</summary>
    [HttpPost("requests")]
    public async Task<IActionResult> Submit([FromBody] FoodRequestCreateDto dto)
    {
        var orgId = CurrentOrgId;
        var org = orgId == null ? null : await _db.BeneficiaryOrganisations.FirstOrDefaultAsync(o => o.Id == orgId.Value);
        if (org == null) return Fail(400, "Your account is not linked to an organisation. Please contact FoodForward SA.");

        // Core data-integrity rule, enforced on the server so it cannot be bypassed from the browser.
        if (org.VettingStatus != VettingStatuses.Approved)
            return Fail(403, "Your organisation must be approved by FoodForward SA before you can submit food requests.");

        var errors = Validation.ValidateRequest(dto, out var quantity, out var beneficiaries, out var neededBy);
        if (errors.Count > 0) return FieldErrors(errors);

        var request = new FoodRequest
        {
            BeneficiaryOrganisationId = org.Id,
            Category = Validation.Clean(dto.Category),
            RequestedQuantityKg = Math.Round(quantity, 2),
            NumberOfBeneficiaries = beneficiaries,
            NeededBy = neededBy,
            Motivation = Validation.Clean(dto.Motivation),
            Status = RequestStatuses.Submitted,
            SubmittedAt = DateTime.UtcNow
        };
        _db.FoodRequests.Add(request);
        await _db.SaveChangesAsync();

        request.BeneficiaryOrganisation = org;
        return StatusCode(201, Mappers.ToDto(request));
    }
}
