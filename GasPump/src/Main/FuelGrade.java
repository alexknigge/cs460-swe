package Main;

/**
 * A simple, immutable data carrier class representing a type of fuel.
 * Using a record is a concise way to bundle related data (name, price, octane) together.
 *
 * @param name           The display name of the fuel (e.g., "Regular").
 * @param pricePerGallon The price of the fuel per gallon.
 * @param octaneRating   The octane rating (e.g., 87).
 */
public record FuelGrade(String name, double pricePerGallon, int octaneRating) {
}
