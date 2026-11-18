namespace FoodShareConnect.Api.Services;

/// <summary>South African date helpers (South Africa has no daylight saving: always UTC+2).</summary>
public static class SaTime
{
    public static DateTime Today => DateTime.UtcNow.AddHours(2).Date;
}
