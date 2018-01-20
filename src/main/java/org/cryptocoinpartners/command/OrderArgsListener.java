package org.cryptocoinpartners.command;

import java.math.BigDecimal;

import javax.inject.Inject;

import org.antlr.v4.runtime.misc.NotNull;
import org.cryptocoinpartners.enumeration.PositionEffect;
import org.cryptocoinpartners.schema.Listing;
import org.cryptocoinpartners.schema.Market;
import org.cryptocoinpartners.schema.Tradeable;

/**
 * @author Tim Olson
 */
@SuppressWarnings("UnusedDeclaration")
public class OrderArgsListener extends OrderBaseListener {

	@Override
	public void exitStopPrice(@NotNull OrderParser.StopPriceContext ctx) {
		BigDecimal amount = Parse.amount(ctx.Amount());
		command.setStop(amount);
	}

	@Override
	public void exitVolume(@NotNull OrderParser.VolumeContext ctx) {
		BigDecimal volume = Parse.amount(ctx.Amount());
		command.setVolume(volume);
	}

	@Override
	public void exitLimitPrice(@NotNull OrderParser.LimitPriceContext ctx) {
		BigDecimal limit = Parse.amount(ctx.Amount());
		command.setLimit(limit);
	}

	@Override
	public void exitPositionEffect(@NotNull OrderParser.PositionEffectContext ctx) {
		PositionEffect position = Parse.positionEffect(ctx.PositionEffect());
		command.setPosition(position);
	}

	@Override
	public void exitMarket(@NotNull OrderParser.MarketContext ctx) {
		Tradeable market = Parse.market(ctx.Market());
		command.setMarket((Market) market);
	}

	@Override
	public void exitListing(@NotNull OrderParser.ListingContext ctx) {
		Listing listing = Parse.listing(ctx.Listing());
		command.setListing(listing);
	}

	@Inject
	private OrderCommand command; // this gets injected by AntlrCommandBase.
}
