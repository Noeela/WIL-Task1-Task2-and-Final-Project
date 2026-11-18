using System.ComponentModel.DataAnnotations;

namespace FoodShareConnect.Api.Models;

/// <summary>
/// A login account stored in Azure SQL. Admin accounts (FoodForward SA staff) have no organisation;
/// Beneficiary Organisation accounts are linked to their organisation.
/// The password is never stored - only a salted hash.
/// </summary>
public class User
{
    public int Id { get; set; }

    [Required, MaxLength(255)]
    public string Email { get; set; } = "";

    [Required, MaxLength(150)]
    public string DisplayName { get; set; } = "";

    /// <summary>"ADMIN" or "BENEFICIARY_ORG" (see <see cref="Roles"/>).</summary>
    [Required, MaxLength(30)]
    public string Role { get; set; } = "";

    public int? BeneficiaryOrganisationId { get; set; }

    public BeneficiaryOrganisation? BeneficiaryOrganisation { get; set; }

    [Required, MaxLength(500)]
    public string PasswordHash { get; set; } = "";

    public bool TwoFactorEnabled { get; set; }

    [MaxLength(100)]
    public string? TwoFactorSecret { get; set; }

    public int FailedAttempts { get; set; }

    public DateTime? LockedUntilUtc { get; set; }
}

public static class Roles
{
    public const string Admin = "ADMIN";
    public const string BeneficiaryOrg = "BENEFICIARY_ORG";
}

public static class VettingStatuses
{
    public const string PendingReview = "PENDING_REVIEW";
    public const string Approved = "APPROVED";
    public const string Rejected = "REJECTED";
    public const string Suspended = "SUSPENDED";
    public static readonly string[] All = { PendingReview, Approved, Rejected, Suspended };
}

public static class RequestStatuses
{
    public const string Submitted = "SUBMITTED";
    public const string Approved = "APPROVED";
    public const string Declined = "DECLINED";
    public const string Cancelled = "CANCELLED";
    public static readonly string[] All = { Submitted, Approved, Declined, Cancelled };
}
