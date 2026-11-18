
using System.ComponentModel.DataAnnotations;

namespace FoodShareConnect.Api.Models;

public class FoodRequest
{
    public int Id { get; set; }

    [Required]
    public int BeneficiaryOrganisationId { get; set; }

    public BeneficiaryOrganisation? BeneficiaryOrganisation { get; set; }

    [Required, MaxLength(100)]
    public string Category { get; set; } = "";

    [Range(typeof(decimal), "0.01", "1000000")]
    public decimal RequestedQuantityKg { get; set; }

    [Range(1, int.MaxValue)]
    public int NumberOfBeneficiaries { get; set; }

    public DateTime NeededBy { get; set; }

    [MaxLength(2000)]
    public string? Motivation { get; set; }

    [Required, MaxLength(30)]
    public string Status { get; set; } = "SUBMITTED";

    [MaxLength(2000)]
    public string? AdminNote { get; set; }

    public int? LinkedDonationId { get; set; }

    public FoodDonation? LinkedDonation { get; set; }

    public DateTime SubmittedAt { get; set; } = DateTime.UtcNow;

    public DateTime? ReviewedAt { get; set; }
}
