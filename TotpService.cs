using System.Security.Cryptography;
using System.Text;

namespace FoodShareConnect.Api.Services;

/// <summary>
/// Time-based one-time passwords (RFC 6238) for Admin two-factor authentication.
/// Works with Google Authenticator, Microsoft Authenticator, etc. Uses only the built-in .NET crypto.
/// </summary>
public static class TotpService
{
    private const string Base32Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private const long StepSeconds = 30;

    public static string GenerateSecret() => Base32Encode(RandomNumberGenerator.GetBytes(20));

    public static string BuildUri(string secret, string email) =>
        "otpauth://totp/FoodShareConnect:" + Uri.EscapeDataString(email) + "?secret=" + secret + "&issuer=FoodShareConnect&digits=6&period=30";

    public static string CodeAt(string secret, DateTimeOffset time) =>
        Hotp(Base32Decode(secret), time.ToUnixTimeSeconds() / StepSeconds);

    /// <summary>Accepts the current code and the ones either side of it, to allow for clock drift.</summary>
    public static bool Verify(string? secret, string? code, DateTimeOffset? now = null)
    {
        if (string.IsNullOrEmpty(secret) || code == null) return false;
        var clean = code.Replace(" ", "").Replace("-", "");
        if (clean.Length != 6 || !clean.All(char.IsDigit)) return false;
        var step = (now ?? DateTimeOffset.UtcNow).ToUnixTimeSeconds() / StepSeconds;
        var key = Base32Decode(secret);
        var ok = false;
        for (long d = -1; d <= 1; d++)
        {
            var expected = Encoding.ASCII.GetBytes(Hotp(key, step + d));
            if (CryptographicOperations.FixedTimeEquals(expected, Encoding.ASCII.GetBytes(clean))) ok = true;
        }
        return ok;
    }

    private static string Hotp(byte[] key, long counter)
    {
        var message = new byte[8];
        for (var i = 7; i >= 0; i--) { message[i] = (byte)(counter & 0xff); counter >>= 8; }
        using var hmac = new HMACSHA1(key);
        var hash = hmac.ComputeHash(message);
        var offset = hash[^1] & 0x0f;
        var binary = ((hash[offset] & 0x7f) << 24) | (hash[offset + 1] << 16) | (hash[offset + 2] << 8) | hash[offset + 3];
        return (binary % 1_000_000).ToString("D6");
    }

    private static string Base32Encode(byte[] data)
    {
        var sb = new StringBuilder();
        int bits = 0, value = 0;
        foreach (var b in data)
        {
            value = (value << 8) | b; bits += 8;
            while (bits >= 5) { sb.Append(Base32Alphabet[(value >> (bits - 5)) & 31]); bits -= 5; }
        }
        if (bits > 0) sb.Append(Base32Alphabet[(value << (5 - bits)) & 31]);
        return sb.ToString();
    }

    private static byte[] Base32Decode(string text)
    {
        var output = new List<byte>();
        int bits = 0, value = 0;
        foreach (var c in text.ToUpperInvariant())
        {
            var idx = Base32Alphabet.IndexOf(c);
            if (idx < 0) continue;
            value = (value << 5) | idx; bits += 5;
            if (bits >= 8) { output.Add((byte)((value >> (bits - 8)) & 0xff)); bits -= 8; }
        }
        return output.ToArray();
    }
}
