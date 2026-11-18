using FoodShareConnect.Api.Data;
using FoodShareConnect.Api.Dtos;
using FoodShareConnect.Api.Models;
using FoodShareConnect.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace FoodShareConnect.Api.Controllers;

[Route("api/requests")]
[Authorize(Roles = Roles.Admin)]
public class FoodRequestsController : ApiControllerBase
{
    private readonly FoodShareDbContext _db;

    public FoodRequestsController(FoodShareDbContext db)
    {
        _db = db;
    }

    [HttpGet]
    public async Task<IActionResult> List([FromQuery] string? status)
    {
        var query = _db.FoodRequests.AsNoTracking().Include(r => r.BeneficiaryOrganisation).AsQueryable();
        if (!string.IsNullOrEmpty(status) && RequestStatuses.All.Contains(status))
            query = query.Where(r => r.Status == status);

        var list = await query.OrderBy(r => r.NeededBy).ThenBy(r => r.Id).ToListAsync();
        return Ok(list.Select(Mappers.ToDto));
    }

    [HttpGet("{id:int}")]
    public async Task<IActionResult> Get(int id)
    {
        var request = await LoadAsync(id);
        return request == null ? Fail(404, "Food request not found.") : Ok(Mappers.ToDto(request));
    }

    /// <summary>
    /// Approves a request and takes the stock out of the chosen donation. Both changes are saved to Azure SQL together,
    /// and the stock update only succeeds if enough un-expired stock is still there - so two admins acting at the same
    /// moment can never allocate the same kilograms twice or push stock below zero.
    /// </summary>
    [HttpPost("{id:int}/approve")]
    public async Task<IActionResult> Approve(int id, [FromBody] ReviewDto dto)
    {
        var request = await LoadAsync(id);
        if (request == null) return Fail(404, "Food request not found.");
        if (request.Status != RequestStatuses.Submitted) return Fail(409, "Only submitted requests can be approved.");

        if (!Validation.TryParseWhole(dto.DonationId, out var donationId))
            return Fail(400, "Select a donation to link before approving this request.");
        var donation = await _db.FoodDonations.AsNoTracking().FirstOrDefaultAsync(d => d.Id == donationId);
        if (donation == null) return Fail(400, "Select a donation to link before approving this request.");

        var today = SaTime.Today;
        if (donation.ExpiryDate <= today) return Fail(409, "The selected donation has expired.");
        if (donation.RemainingQuantityKg < request.RequestedQuantityKg)
            return Fail(409, $"Insufficient stock: only {donation.RemainingQuantityKg:0.##} kg remains for this donation.");

        var note = Validation.Clean(dto.Note);
        if (note.Length > 500) return Fail(400, "The note must be 500 characters or fewer.");

        var quantity = request.RequestedQuantityKg;
        var reviewedAt = DateTime.UtcNow;

        await using var transaction = await _db.Database.BeginTransactionAsync();

        // 1. Claim the request (only if it is still SUBMITTED).
        var claimed = await _db.Database.ExecuteSqlInterpolatedAsync(
            $"UPDATE FoodRequests SET Status = 'APPROVED', AdminNote = {note}, LinkedDonationId = {donationId}, ReviewedAt = {reviewedAt} WHERE Id = {id} AND Status = 'SUBMITTED'");
        if (claimed != 1)
        {
            await transaction.RollbackAsync();
            return Fail(409, "This request has already been reviewed.");
        }

        // 2. Take the stock (only if enough un-expired stock is still there).
        var updated = await _db.Database.ExecuteSqlInterpolatedAsync(
            $"UPDATE FoodDonations SET RemainingQuantityKg = RemainingQuantityKg - {quantity} WHERE Id = {donationId} AND RemainingQuantityKg >= {quantity} AND ExpiryDate > {today}");
        if (updated != 1)
        {
            await transaction.RollbackAsync();
            return Fail(409, "The stock changed while you were reviewing - please refresh and try again.");
        }

        await transaction.CommitAsync();

        var saved = await LoadAsync(id);
        return Ok(Mappers.ToDto(saved!));
    }

    [HttpPost("{id:int}/decline")]
    public async Task<IActionResult> Decline(int id, [FromBody] ReviewDto dto)
    {
        var request = await LoadAsync(id);
        if (request == null) return Fail(404, "Food request not found.");
        if (request.Status != RequestStatuses.Submitted) return Fail(409, "Only submitted requests can be declined.");

        var note = Validation.Clean(dto.Note);
        if (note.Length > 500) return Fail(400, "The note must be 500 characters or fewer.");

        var reviewedAt = DateTime.UtcNow;
        var changed = await _db.Database.ExecuteSqlInterpolatedAsync(
            $"UPDATE FoodRequests SET Status = 'DECLINED', AdminNote = {note}, ReviewedAt = {reviewedAt} WHERE Id = {id} AND Status = 'SUBMITTED'");
        if (changed != 1) return Fail(409, "This request has already been reviewed.");

        var saved = await LoadAsync(id);
        return Ok(Mappers.ToDto(saved!));
    }

    private Task<FoodRequest?> LoadAsync(int id) =>
        _db.FoodRequests.AsNoTracking().Include(r => r.BeneficiaryOrganisation).FirstOrDefaultAsync(r => r.Id == id);
}
