
using FoodShareConnect.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace FoodShareConnect.Api.Data;

public class FoodShareDbContext : DbContext
{
    public FoodShareDbContext(
        DbContextOptions<FoodShareDbContext> options)
        : base(options)
    {
    }

    public DbSet<BeneficiaryOrganisation> BeneficiaryOrganisations
        => Set<BeneficiaryOrganisation>();

    public DbSet<FoodDonation> FoodDonations
        => Set<FoodDonation>();

    public DbSet<FoodRequest> FoodRequests
        => Set<FoodRequest>();

    public DbSet<User> Users
        => Set<User>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        modelBuilder.Entity<BeneficiaryOrganisation>()
            .ToTable("BeneficiaryOrganisations");

        modelBuilder.Entity<FoodDonation>()
            .ToTable("FoodDonations");

        modelBuilder.Entity<FoodRequest>()
            .ToTable("FoodRequests");

        modelBuilder.Entity<FoodRequest>()
            .HasOne(r => r.BeneficiaryOrganisation)
            .WithMany(o => o.FoodRequests)
            .HasForeignKey(r => r.BeneficiaryOrganisationId)
            .OnDelete(DeleteBehavior.Restrict);

        modelBuilder.Entity<FoodRequest>()
            .HasOne(r => r.LinkedDonation)
            .WithMany(d => d.FoodRequests)
            .HasForeignKey(r => r.LinkedDonationId)
            .OnDelete(DeleteBehavior.Restrict);

        modelBuilder.Entity<FoodDonation>()
            .Property(d => d.QuantityKg)
            .HasPrecision(12, 2);

        modelBuilder.Entity<FoodDonation>()
            .Property(d => d.RemainingQuantityKg)
            .HasPrecision(12, 2);

        modelBuilder.Entity<FoodRequest>()
            .Property(r => r.RequestedQuantityKg)
            .HasPrecision(12, 2);

        modelBuilder.Entity<BeneficiaryOrganisation>()
            .Property(o => o.RegisteredAt)
            .HasDefaultValueSql("SYSUTCDATETIME()");

        modelBuilder.Entity<User>()
            .ToTable("Users");

        modelBuilder.Entity<User>()
            .HasIndex(u => u.Email)
            .IsUnique();

        modelBuilder.Entity<User>()
            .HasOne(u => u.BeneficiaryOrganisation)
            .WithMany()
            .HasForeignKey(u => u.BeneficiaryOrganisationId)
            .OnDelete(DeleteBehavior.Restrict);
    }
}
