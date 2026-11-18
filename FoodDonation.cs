
using System.ComponentModel.DataAnnotations;

namespace FoodShareConnect.Api.Models;

public class FoodDonation
{
    public int Id { get; set; }

    [Required, MaxLength(150)]
    public string DonorName { get; set; } = "";

    [Required, MaxLength(100)]
    public string Category { get; set; } = "";

    [Range(typeof(decimal), "0.01", "1000000")]
    public decimal QuantityKg { get; set; }

    [Range(typeof(decimal), "0", "1000000")]
    public decimal RemainingQuantityKg { get; set; }

    public DateTime ExpiryDate { get; set; }

    [MaxLength(1000)]
    public string? Notes { get; set; }

    public DateTime ReceivedAt { get; set; } = DateTime.UtcNow;

    public ICollection<FoodRequest> FoodRequests { get; set; }
        = new List<FoodRequest>();
}
