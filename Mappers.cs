using FoodShareConnect.Api.Dtos;
using FoodShareConnect.Api.Models;

namespace FoodShareConnect.Api.Services;

/// <summary>Turns database records into the JSON shapes the web page expects. Never exposes password hashes or 2FA secrets.</summary>
public static class Mappers
{
    private const string DateFormat = "yyyy-MM-dd";

    public static UserDto ToDto(User u) => new UserDto
    {
        Id = u.Id,
        Email = u.Email,
        DisplayName = u.DisplayName,
        Role = u.Role,
        OrgId = u.BeneficiaryOrganisationId,
        TwoFactorEnabled = u.TwoFactorEnabled
    };

    public static OrganisationDto ToDto(BeneficiaryOrganisation o) => new OrganisationDto
    {
        Id = o.Id,
        OrganisationName = o.OrganisationName,
        ContactPersonName = o.ContactPersonName,
        Email = o.Email,
        PhoneNumber = o.PhoneNumber,
        PhysicalAddress = o.PhysicalAddress,
        Province = o.Province,
        NpoRegistrationNumber = o.NpoRegistrationNumber,
        EstimatedBeneficiaries = o.EstimatedBeneficiaries,
        VettingStatus = o.VettingStatus,
        AdminNote = o.AdminNote ?? "",
        RegisteredAt = o.RegisteredAt.ToString("yyyy-MM-ddTHH:mm:ss")
    };

    public static DonationDto ToDto(FoodDonation d) => new DonationDto
    {
        Id = d.Id,
        DonorName = d.DonorName,
        Category = d.Category,
        QuantityKg = d.QuantityKg,
        RemainingKg = d.RemainingQuantityKg,
        ExpiryDate = d.ExpiryDate.ToString(DateFormat),
        Notes = d.Notes ?? "",
        ReceivedAt = d.ReceivedAt.ToString("yyyy-MM-ddTHH:mm:ss")
    };

    public static FoodRequestDto ToDto(FoodRequest r) => new FoodRequestDto
    {
        Id = r.Id,
        OrgId = r.BeneficiaryOrganisationId,
        OrgName = r.BeneficiaryOrganisation?.OrganisationName ?? "(deleted organisation)",
        Category = r.Category,
        QuantityKg = r.RequestedQuantityKg,
        Beneficiaries = r.NumberOfBeneficiaries,
        NeededBy = r.NeededBy.ToString(DateFormat),
        Motivation = r.Motivation ?? "",
        Status = r.Status,
        AdminNote = r.AdminNote ?? "",
        DonationId = r.LinkedDonationId,
        SubmittedAt = r.SubmittedAt.ToString("yyyy-MM-ddTHH:mm:ss"),
        ReviewedAt = r.ReviewedAt?.ToString("yyyy-MM-ddTHH:mm:ss")
    };
}
