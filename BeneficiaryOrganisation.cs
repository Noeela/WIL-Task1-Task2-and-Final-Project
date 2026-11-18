
using System.ComponentModel.DataAnnotations;

namespace FoodShareConnect.Api.Models;

public class BeneficiaryOrganisation
{
    public int Id { get; set; }

    [Required, MaxLength(150)]
    public string OrganisationName { get; set; } = "";

    [Required, MaxLength(150)]
    public string ContactPersonName { get; set; } = "";

    [Required, EmailAddress, MaxLength(255)]
    public string Email { get; set; } = "";

    [MaxLength(30)]
    public string PhoneNumber { get; set; } = "";

    [MaxLength(300)]
    public string PhysicalAddress { get; set; } = "";

    [MaxLength(50)]
    public string Province { get; set; } = "";

    [MaxLength(100)]
    public string NpoRegistrationNumber { get; set; } = "";

    [Range(0, int.MaxValue)]
    public int EstimatedBeneficiaries { get; set; }

    [Required, MaxLength(30)]
    public string VettingStatus { get; set; } = "PENDING_REVIEW";

    /// <summary>Note written by FoodForward SA staff when vetting the organisation.</summary>
    [MaxLength(500)]
    public string? AdminNote { get; set; }

    public DateTime RegisteredAt { get; set; } = DateTime.UtcNow;

    public ICollection<FoodRequest> FoodRequests { get; set; }
        = new List<FoodRequest>();
}
