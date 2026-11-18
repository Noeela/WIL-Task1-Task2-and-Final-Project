using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace FoodShareConnect.Api.Migrations
{
    /// <inheritdoc />
    public partial class InitialFoodShareSchema : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "BeneficiaryOrganisations",
                columns: table => new
                {
                    Id = table.Column<int>(type: "int", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    OrganisationName = table.Column<string>(type: "nvarchar(150)", maxLength: 150, nullable: false),
                    ContactPersonName = table.Column<string>(type: "nvarchar(150)", maxLength: 150, nullable: false),
                    Email = table.Column<string>(type: "nvarchar(255)", maxLength: 255, nullable: false),
                    PhoneNumber = table.Column<string>(type: "nvarchar(30)", maxLength: 30, nullable: false),
                    PhysicalAddress = table.Column<string>(type: "nvarchar(300)", maxLength: 300, nullable: false),
                    Province = table.Column<string>(type: "nvarchar(50)", maxLength: 50, nullable: false),
                    NpoRegistrationNumber = table.Column<string>(type: "nvarchar(100)", maxLength: 100, nullable: false),
                    EstimatedBeneficiaries = table.Column<int>(type: "int", nullable: false),
                    VettingStatus = table.Column<string>(type: "nvarchar(30)", maxLength: 30, nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_BeneficiaryOrganisations", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "FoodDonations",
                columns: table => new
                {
                    Id = table.Column<int>(type: "int", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    DonorName = table.Column<string>(type: "nvarchar(150)", maxLength: 150, nullable: false),
                    Category = table.Column<string>(type: "nvarchar(100)", maxLength: 100, nullable: false),
                    QuantityKg = table.Column<decimal>(type: "decimal(12,2)", precision: 12, scale: 2, nullable: false),
                    RemainingQuantityKg = table.Column<decimal>(type: "decimal(12,2)", precision: 12, scale: 2, nullable: false),
                    ExpiryDate = table.Column<DateTime>(type: "datetime2", nullable: false),
                    Notes = table.Column<string>(type: "nvarchar(1000)", maxLength: 1000, nullable: true),
                    ReceivedAt = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_FoodDonations", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "FoodRequests",
                columns: table => new
                {
                    Id = table.Column<int>(type: "int", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    BeneficiaryOrganisationId = table.Column<int>(type: "int", nullable: false),
                    Category = table.Column<string>(type: "nvarchar(100)", maxLength: 100, nullable: false),
                    RequestedQuantityKg = table.Column<decimal>(type: "decimal(12,2)", precision: 12, scale: 2, nullable: false),
                    NumberOfBeneficiaries = table.Column<int>(type: "int", nullable: false),
                    NeededBy = table.Column<DateTime>(type: "datetime2", nullable: false),
                    Motivation = table.Column<string>(type: "nvarchar(2000)", maxLength: 2000, nullable: true),
                    Status = table.Column<string>(type: "nvarchar(30)", maxLength: 30, nullable: false),
                    AdminNote = table.Column<string>(type: "nvarchar(2000)", maxLength: 2000, nullable: true),
                    LinkedDonationId = table.Column<int>(type: "int", nullable: true),
                    SubmittedAt = table.Column<DateTime>(type: "datetime2", nullable: false),
                    ReviewedAt = table.Column<DateTime>(type: "datetime2", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_FoodRequests", x => x.Id);
                    table.ForeignKey(
                        name: "FK_FoodRequests_BeneficiaryOrganisations_BeneficiaryOrganisationId",
                        column: x => x.BeneficiaryOrganisationId,
                        principalTable: "BeneficiaryOrganisations",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Restrict);
                    table.ForeignKey(
                        name: "FK_FoodRequests_FoodDonations_LinkedDonationId",
                        column: x => x.LinkedDonationId,
                        principalTable: "FoodDonations",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Restrict);
                });

            migrationBuilder.CreateIndex(
                name: "IX_FoodRequests_BeneficiaryOrganisationId",
                table: "FoodRequests",
                column: "BeneficiaryOrganisationId");

            migrationBuilder.CreateIndex(
                name: "IX_FoodRequests_LinkedDonationId",
                table: "FoodRequests",
                column: "LinkedDonationId");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "FoodRequests");

            migrationBuilder.DropTable(
                name: "BeneficiaryOrganisations");

            migrationBuilder.DropTable(
                name: "FoodDonations");
        }
    }
}
