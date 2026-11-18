using FoodShareConnect.Api.Data;
using FoodShareConnect.Api.Dtos;
using FoodShareConnect.Api.Models;
using FoodShareConnect.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace FoodShareConnect.Api.Controllers;

[Route("api")]
public class StatsController : ApiControllerBase
{
    private readonly FoodShareDbContext _db;

    public StatsController(FoodShareDbContext db)
    {
        _db = db;
    }

    /// <summary>Admin dashboard numbers, counted live from Azure SQL.</summary>
    [HttpGet("stats")]
    [Authorize(Roles = Roles.Admin)]
    public async Task<IActionResult> Stats()
    {
        var today = SaTime.Today;
        var stock = await _db.FoodDonations
            .Where(d => d.RemainingQuantityKg > 0 && d.ExpiryDate > today)
            .SumAsync(d => (decimal?)d.RemainingQuantityKg);

        return Ok(new StatsDto
        {
            PendingOrgs = await _db.BeneficiaryOrganisations.CountAsync(o => o.VettingStatus == VettingStatuses.PendingReview),
            ApprovedOrgs = await _db.BeneficiaryOrganisations.CountAsync(o => o.VettingStatus == VettingStatuses.Approved),
            SubmittedRequests = await _db.FoodRequests.CountAsync(r => r.Status == RequestStatuses.Submitted),
            StockKg = Math.Round(stock ?? 0m, 2)
        });
    }

    /// <summary>Shows in the page footer which database the site is really using (proof for the lecturer).</summary>
    [HttpGet("health")]
    [AllowAnonymous]
    public async Task<IActionResult> Health()
    {
        bool connected;
        try { connected = await _db.Database.CanConnectAsync(); }
        catch (Exception) { connected = false; }
        if (!connected) return Ok(new { ok = false, database = "unreachable" });

        var connection = _db.Database.GetDbConnection();
        var server = connection.DataSource ?? "";
        var azure = server.Contains("database.windows.net", StringComparison.OrdinalIgnoreCase);
        return Ok(new { ok = true, database = azure ? "azure-sql" : "sql-server", name = connection.Database ?? "" });
    }
}
