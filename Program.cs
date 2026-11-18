using FoodShareConnect.Api.Data;
using FoodShareConnect.Api.Models;
using FoodShareConnect.Api.Services;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container
builder.Services.AddControllers()
    .ConfigureApiBehaviorOptions(options =>
    {
        // If the browser sends something that is not valid JSON, answer with a friendly message instead of a technical one.
        options.InvalidModelStateResponseFactory = context =>
            new BadRequestObjectResult(new { message = "That request could not be read. Please check your entries and try again." });
    });
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// Database registration using the "FoodShare" connection string from appsettings.json
builder.Services.AddDbContext<FoodShareDbContext>(options =>
    options.UseSqlServer(builder.Configuration.GetConnectionString("FoodShare")));

// Security: salted password hashing, tamper-proof sign-in tokens, and a role check on every protected endpoint
builder.Services.AddDataProtection();
builder.Services.AddSingleton<TokenService>();
builder.Services.AddSingleton<IPasswordHasher<User>, PasswordHasher<User>>();
builder.Services.AddAuthentication(TokenAuthenticationHandler.SchemeName)
    .AddScheme<AuthenticationSchemeOptions, TokenAuthenticationHandler>(TokenAuthenticationHandler.SchemeName, null);
builder.Services.AddAuthorization();

var app = builder.Build();

// Any unexpected error becomes a friendly message (the real details go to the log, never to the browser)
app.UseExceptionHandler(errorApp => errorApp.Run(async context =>
{
    context.Response.StatusCode = StatusCodes.Status500InternalServerError;
    await context.Response.WriteAsJsonAsync(new { message = "Something went wrong on our side. Please try again in a moment." });
}));

// Automatically apply database migrations / ensure tables exist on startup, then create the admin login on first run
using (var scope = app.Services.CreateScope())
{
    var dbContext = scope.ServiceProvider.GetRequiredService<FoodShareDbContext>();
    dbContext.Database.Migrate();

    var hasher = scope.ServiceProvider.GetRequiredService<IPasswordHasher<User>>();
    await SeedData.EnsureAsync(dbContext, hasher, app.Configuration);
}

// Configure the HTTP request pipeline
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

// Serve static HTML, CSS, JS, and image files before HTTPS redirection & routing
app.UseDefaultFiles();
app.UseStaticFiles();

app.UseHttpsRedirection();
app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

app.Run();
