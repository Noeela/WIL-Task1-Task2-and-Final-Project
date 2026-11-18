using System.Security.Claims;
using Microsoft.AspNetCore.Mvc;

namespace FoodShareConnect.Api.Controllers;

[ApiController]
public abstract class ApiControllerBase : ControllerBase
{
    protected int CurrentUserId => int.TryParse(User.FindFirst(ClaimTypes.NameIdentifier)?.Value, out var id) ? id : 0;

    protected int? CurrentOrgId => int.TryParse(User.FindFirst("orgId")?.Value, out var id) ? id : null;

    /// <summary>A plain-language error the web page shows to the person.</summary>
    protected IActionResult Fail(int status, string message) => StatusCode(status, new { message });

    /// <summary>Errors for individual form fields, shown under each input.</summary>
    protected IActionResult FieldErrors(Dictionary<string, string> errors) =>
        BadRequest(new { message = "Please correct the highlighted fields.", fieldErrors = errors });
}
