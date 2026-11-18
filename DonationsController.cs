using FoodShareConnect.Api.Data;
using FoodShareConnect.Api.Dtos;
using FoodShareConnect.Api.Models;
using FoodShareConnect.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace FoodShareConnect.Api.Controllers;

[Route("api")]
public class DonationsController : ApiControllerBase
{
    private readonly FoodShareDbContext _db;

    public DonationsController(FoodShareDbContext db)
    {
        _db = db;
    }

    /// <summary>Available stock (not expired, something left). Soonest-to-expire first so it is used before it spoils.</summary>
    [HttpGet("stock")]
    [Authorize]
    public async Task<IActionResult> Stock()
    {
        var today = SaTime.Today;
        var list = await _db.FoodDonations.AsNoTracking()
            .Where(d => d.RemainingQuantityKg > 0 && d.ExpiryDate > today)
            .OrderBy(d => d.ExpiryDate).ThenBy(d => d.Id)
            .ToListAsync();
        return Ok(list.Select(Mappers.ToDto));
    }

    /// <summary>Admin: donations that could cover a request (same category, enough left).</summary>
    [HttpGet("stock/matching")]
    [Authorize(Roles = Roles.Admin)]
    public async Task<IActionResult> Matching([FromQuery] string? category, [FromQuery] string? qty)
    {
        if (string.IsNullOrEmpty(category) || !Validation.Categories.Contains(category) || !Validation.TryParseDecimal(qty, out var quantity))
            return Fail(400, "Category and quantity are required.");

        var today = SaTime.Today;
        var list = await _db.FoodDonations.AsNoTracking()
            .Where(d => d.Category == category && d.RemainingQuantityKg >= quantity && d.ExpiryDate > today)
            .OrderBy(d => d.ExpiryDate).ThenBy(d => d.Id)
            .ToListAsync();
        return Ok(list.Select(Mappers.ToDto));
    }

    /// <summary>Admin: capture a donation. The record is saved straight into Azure SQL.</summary>
    [HttpPost("donations")]
    [Authorize(Roles = Roles.Admin)]
    public async Task<IActionResult> Create([FromBody] DonationCreateDto dto)
    {
        var errors = Validation.ValidateDonation(dto, out var quantity, out var expiry);
        if (errors.Count > 0) return FieldErrors(errors);

        var donation = new FoodDonation
        {
            DonorName = Validation.Clean(dto.DonorName),
            Category = Validation.Clean(dto.Category),
            QuantityKg = Math.Round(quantity, 2),
            RemainingQuantityKg = Math.Round(quantity, 2),
            ExpiryDate = expiry,
            Notes = Validation.Clean(dto.Notes),
            ReceivedAt = DateTime.UtcNow
        };
        _db.FoodDonations.Add(donation);
        await _db.SaveChangesAsync();
        return StatusCode(201, Mappers.ToDto(donation));
    }
}
