package org.cryptocoinpartners.util;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.ObjectUtils;
import org.cryptocoinpartners.schema.Amount;
import org.cryptocoinpartners.schema.Asset;
import org.cryptocoinpartners.schema.DiscreteAmount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class describing a set of currencies and all the cross rates between them. */
public class ListingsMatrix {
  protected static boolean seedUSDT =
      (ConfigUtil.combined() != null)
          ? ConfigUtil.combined().getBoolean("marketdata.implied.usdt", true)
          : false;
  /** The map between the currencies and their order. */
  private final Map<Asset, Map<Asset, DiscreteAmount>> listings;

  protected static Logger log = LoggerFactory.getLogger("org.cryptocoinpartners.ListingsMatrix");

  /** Constructor with no currency. The ListingsMatrix constructed has no currency and no rates. */
  public ListingsMatrix() {
    listings = new ConcurrentHashMap<>();
  }

  /**
   * Constructor with one currency. The ListingsMatrix has one currency with a 1.0 exchange rate to
   * itself.
   *
   * @param ccy The currency.
   */
  public ListingsMatrix(final Asset ccy) {
    ArgumentChecker.notNull(ccy, "Asset");
    listings = new ConcurrentHashMap<>();
    listings.put(ccy, new ConcurrentHashMap<Asset, DiscreteAmount>());
    listings
        .get(ccy)
        .put(
            ccy,
            new DiscreteAmount(
                DiscreteAmount.roundedCountForBasis(BigDecimal.ONE, ccy.getBasis()),
                ccy.getBasis()));
  }

  /**
   * Constructor with an initial currency pair.
   *
   * @param ccy1 The first currency.
   * @param ccy2 The second currency.
   * @param rate TheListings rate between ccy1 and the ccy2. It is 1 ccy1 = rate * ccy2. The
   *     Listings matrix will be completed with the ccy2/ccy1 rate.
   */
  public ListingsMatrix(final Asset ccy1, final Asset ccy2, final DiscreteAmount rate) {
    listings = new ConcurrentHashMap<>();

    addAsset(ccy1, ccy2, rate);
  }

  /**
   * Constructor from an existing ListingsMatrix. A new map and array are created.
   *
   * @param ListingsMatrix The ListingsMatrix.
   */
  public ListingsMatrix(final ListingsMatrix ListingsMatrix) {
    ArgumentChecker.notNull(ListingsMatrix, "ListingsMatrix");

    listings = new ConcurrentHashMap<>(ListingsMatrix.listings);
  }

  /**
   * Add a new currency to the Listings matrix.
   *
   * @param ccyToAdd The currency to add. Should not be in the Listings matrix already.
   * @param ccyReference The reference currency used to compute the cross rates with the new
   *     currency. Should already be in the matrix, except if the matrix is empty. IF the Listings
   *     matrix is empty, the reference currency will be used as currency 0.
   * @param rate TheListings rate between the new currency and the reference currency. It is 1
   *     ccyToAdd = rate ccyReference. The Listings matrix will be completed using cross rate
   *     coherent with the data provided.
   */
  public synchronized void addAsset(Asset ccyToAdd, Asset ccyReference, DiscreteAmount rate) {
    ArgumentChecker.notNull(ccyToAdd, "Asset to add to the Listings matrix should not be null");
    ArgumentChecker.notNull(ccyReference, "Reference currency should not be null");
    ArgumentChecker.isTrue(!ccyToAdd.equals(ccyReference), "Currencies should be different");
    // both row and colum are empty so we can add them safley

    if (!rate.isZero()) {

      // we invert we need to convert bsdid.

      //	DiscreteAmount inverseRate = rate.invertAsDiscreteAmount();
      DiscreteAmount inverseRate =
          new DiscreteAmount(
              DiscreteAmount.roundedCountForBasis(
                  rate.invert().asBigDecimal(), ccyToAdd.getBasis()),
              ccyToAdd.getBasis());

      inverseRate.toBasis((long) ccyToAdd.getBasis(), Remainder.ROUND_EVEN);
      if (listings.get(ccyReference) == null)
        listings.put(ccyReference, new ConcurrentHashMap<Asset, DiscreteAmount>());
      if (listings.get(ccyToAdd) == null)
        listings.put(ccyToAdd, new ConcurrentHashMap<Asset, DiscreteAmount>());
      listings.get(ccyToAdd).put(ccyReference, rate);
      listings
          .get(ccyToAdd)
          .put(
              ccyToAdd,
              new DiscreteAmount(
                  DiscreteAmount.roundedCountForBasis(BigDecimal.ONE, ccyToAdd.getBasis()),
                  ccyToAdd.getBasis()));

      listings.get(ccyReference).put(ccyToAdd, inverseRate);
      listings
          .get(ccyReference)
          .put(
              ccyReference,
              new DiscreteAmount(
                  DiscreteAmount.roundedCountForBasis(BigDecimal.ONE, ccyReference.getBasis()),
                  ccyReference.getBasis()));
    }

    // updateRates(ccyToAdd, ccyReference, rate);
    /*// ArgumentChecker.isTrue(listings.containsKey(ccyReference), " {} not in the Listings matrix", ccyReference);
    //ArgumentChecker.isTrue(!listings.containsKey(ccyToAdd), "New currency {} already in the Listings matrix", ccyToAdd);

    Iterator<Asset> lit = listings.keySet().iterator();

    while (lit.hasNext()) {
    	Asset ccy = lit.next();
    	if (!ccyToAdd.equals(ccy)) {

    		long inverseCrossRate = 0;
    		long crossRate = 0;
    		// new matrix create of rates that is _currenciesLookup (size) x _currenciesLookup.sise()
    		// loop over each of the quote currencies and get the cross rate, converting to th the baiss of the new curency
    		//   if (listings.get(ccyReference)==null)
    		//     listings.put(ccyReference, value)
    		if (listings.get(ccyReference).get(ccy) == null) {
    			BigDecimal inverseRateBD = (((BigDecimal.valueOf(1.0 / (ccyReference.getBasis()))).divide(BigDecimal.valueOf(rate), ccyToAdd.getScale(),
    					RoundingMode.HALF_EVEN)).divide(BigDecimal.valueOf(ccy.getBasis())));
    			inverseCrossRate = inverseRateBD.longValue();
    			//listings.put(ccyReference, new ConcurrentHashMap<Asset, Long>());
    			//listings.put(ccy, new ConcurrentHashMap<Asset, Long>());

    			listings.get(ccy).put(ccyReference, rate);
    			listings.get(ccy).put(ccy, (long) (1.0 / ccy.getBasis()));
    			listings.get(ccyReference).put(ccy, inverseCrossRate);
    			listings.get(ccyReference).put(ccyReference, (long) (1.0 / ccyReference.getBasis()));

    		}

    		// break;

    		crossRate = Math.round((rate * listings.get(ccyReference).get(ccy).longValue() * (ccyToAdd.getBasis())));
    		// get the rate for the
    		if (crossRate != 0) {
    			BigDecimal crossRateBD = BigDecimal.valueOf(crossRate);
    			// calculate the inverse by getting the basis of the currenct rate and setting the scale to ttha tof the quote currency.
    			BigDecimal inverseCrossRateBD = ((BigDecimal.valueOf(1.0 / (ccy.getBasis()))).divide(crossRateBD, ccyToAdd.getScale(),
    					RoundingMode.HALF_EVEN));
    			// divine the rate by the basis fo the currency to be added
    			inverseCrossRateBD = inverseCrossRateBD.divide(BigDecimal.valueOf(ccyToAdd.getBasis()));

    			inverseCrossRate = inverseCrossRateBD.longValue();
    			// update the base currecny vs the quote
    			if (listings.get(ccyToAdd) == null) {
    				listings.put(ccyToAdd, new ConcurrentHashMap<Asset, Long>());

    			}
    		}

    		if (this.listings.get(ccyToAdd) == null)
    			this.listings.put(ccyToAdd, new ConcurrentHashMap<Asset, Long>());
    		listings.get(ccyToAdd).put(ccy, Long.valueOf(crossRate));
    		if (this.listings.get(ccy) == null)
    			this.listings.put(ccy, new ConcurrentHashMap<Asset, Long>());
    		listings.get(ccy).put(ccyToAdd, Long.valueOf(inverseCrossRate));

    	}

    }
    listings.get(ccyToAdd).put(ccyToAdd, (long) (1.0 / ccyToAdd.getBasis()));

    }*/
  }

  /**
   * Return the exchange rate between two currencies.
   *
   * @param ccy1 The first currency.
   * @param ccy2 The second currency.
   * @return The exchange rate: 1.0 * ccy1 = x * ccy2.
   */
  public DiscreteAmount getRate(final Asset ccy1, final Asset ccy2) {
    if (ccy1.equals(ccy2)) {
      return new DiscreteAmount(
          DiscreteAmount.roundedCountForBasis(BigDecimal.ONE, ccy1.getBasis()), ccy1.getBasis());
    }
    final Map<Asset, DiscreteAmount> index1 = listings.get(ccy1);
    final Map<Asset, DiscreteAmount> index2 = listings.get(ccy2);
    // if (listings.get(ccy1) == null) return null;
    // if (listings.get(ccy1).get(ccy2) == null) return null;
    ArgumentChecker.isTrue(
        listings.get(ccy1) != null, "Asset {} is  not in the Listings Matrix", ccy1);
    ArgumentChecker.isTrue(
        listings.get(ccy1).get(ccy2) != null,
        "Asset {} and {}  not in the Listings Matrix",
        ccy1,
        ccy2);

    return listings.get(ccy1).get(ccy2);
  }

  /**
   * @param ccy1 The first currency
   * @param ccy2 The second currency
   * @return True if the matrix contains both currencies
   */
  public boolean containsPair(final Asset ccy1, final Asset ccy2) {
    return listings.containsKey(ccy1) && listings.containsKey(ccy2);
  }

  /**
   * Reset the exchange rate of a given currency.
   *
   * @param ccyToUpdate The currency for which the exchange rats should be updated. Should be in the
   *     Listings matrix already.
   * @param ccyReference The reference currency used to compute the cross rates with the new
   *     currency. Should already be in the matrix.
   * @param rate TheListings rate between the new currency and the reference currency. It is 1.0 *
   *     ccyToAdd = rate * ccyReference. The Listings matrix will be changed for currency1 using
   *     cross rate coherent with the data provided.
   */
  public void updateRates(
      final Asset ccyToUpdate, final Asset ccyReference, final DiscreteAmount rate) {

    ArgumentChecker.isTrue(
        listings.get(ccyReference) != null, "Reference Asset not in the Listings matrix");
    ArgumentChecker.isTrue(
        listings.get(ccyReference).get(ccyToUpdate) != null,
        "Asset to update not in the Listings matrix");

    if (!rate.isZero()) {

      // TODO we get an exepction here if we have two exchanges with balacnes.
      Set<Asset> test = listings.keySet();
      // Iterator<Asset> lit = listings.keySet().iterator();
      listings.get(ccyToUpdate).put(ccyReference, rate);
      // inverse rate
      // Amount inverseRate = rate.invert();
      DiscreteAmount inverseRate =
          new DiscreteAmount(
              DiscreteAmount.roundedCountForBasis(
                  rate.invert().asBigDecimal(), ccyToUpdate.getBasis()),
              ccyToUpdate.getBasis());
      listings.get(ccyReference).put(ccyToUpdate, inverseRate);

      for (Asset ccy : listings.keySet()) {

        if (!ccy.getSymbol().equals(ccyReference.getSymbol())
            && !ccy.getSymbol().equals(ccyToUpdate.getSymbol())
            && listings.get(ccyReference).get(ccy) != null
            && !listings.get(ccyReference).get(ccy).isZero()) {
          //	if (ccyToUpdate.getSymbol().equals("USD") || ccyReference.getSymbol().equals("USD"))
          //		log.debug("tester");

          //	BigDecimal.valueOf(rate).multiply(BigDecimal.valueOf(listings.get(ccyReference).get(ccy)));
          DiscreteAmount inverseCrossRateDiscrete;
          DiscreteAmount crossRateDiscerte;

          if (seedUSDT
              && (("USD".equals(ccyToUpdate.getSymbol()) && "USDT".equals(ccy.getSymbol()))
                  || ("USD".equals(ccy.getSymbol()) && "USDT".equals(ccyToUpdate.getSymbol())))) {

            crossRateDiscerte =
                new DiscreteAmount(
                    DiscreteAmount.roundedCountForBasis(BigDecimal.ONE, ccy.getBasis()),
                    ccy.getBasis());

            // calculate the inverse by getting the basis of the currenct rate and setting the scale
            // to ttha tof the quote currency.
            inverseCrossRateDiscrete =
                new DiscreteAmount(
                    DiscreteAmount.roundedCountForBasis(BigDecimal.ONE, ccyToUpdate.getBasis()),
                    ccyToUpdate.getBasis());
            log.trace(
                "updateRates set USD/USDT rates to crossRateDiscerte={} inverseCrossRateDiscrete={}",
                crossRateDiscerte,
                inverseCrossRateDiscrete);
          } else {

            Amount crossRate =
                rate.times(listings.get(ccyReference).get(ccy), Remainder.ROUND_EVEN);
            crossRateDiscerte =
                new DiscreteAmount(
                    DiscreteAmount.roundedCountForBasis(crossRate.asBigDecimal(), ccy.getBasis()),
                    ccy.getBasis());

            // calculate the inverse by getting the basis of the currenct rate and setting the scale
            // to ttha tof the quote currency.
            inverseCrossRateDiscrete =
                new DiscreteAmount(
                    DiscreteAmount.roundedCountForBasis(
                        crossRate.invert().asBigDecimal(), ccyToUpdate.getBasis()),
                    ccyToUpdate.getBasis());
          }

          // divine the rate by the basis fo the currency to be added
          // inverseCrossRateBD =
          // inverseCrossRateBD.divide(BigDecimal.valueOf(ccyToUpdate.getBasis()));

          // DiscreteAmount.roundedCountForBasis(crossRateBD, ccy.getBasis());
          // DiscreteAmount.roundedCountForBasis(inverseCrossRateBD, ccyToUpdate.getBasis());

          listings.get(ccyToUpdate).put(ccy, crossRateDiscerte);
          listings.get(ccy).put(ccyToUpdate, inverseCrossRateDiscrete);
        }
      }
    }
  }

  /*			for (Asset ccy : listings.keySet()) {
  	//  while (lit.hasNext()) {
  	//BITFINEX:XMR.USD
  	//find our row to update for the base currency
  	if (ccyToUpdate.equals(ccy) || ccyReference.equals(ccy)) {
  		for (Asset pairCcy : listings.get(ccy).keySet()) {
  			if (ccyToUpdate.equals(pairCcy) || ccyReference.equals(pairCcy)) {

  				long inverseCrossRate = 0;
  				long crossRate = 0;
  				if ((this.listings.get(ccyReference) != null) && (((Map) this.listings.get(ccyReference)).get(ccy) != null)) {
  				//	need to update 4 values

  					listings.get(ccyToUpdate).get(ccyReference) with the rate
  					listings.get(ccyToUpdate).get(ccyReference) with the 1/rate
  					listings.get(ccyToUpdate).get(ccyToUpdate) with the 1
  					listings.get(ccyReference).get(ccyReference) with the 1
  					crossRate = Math.round((rate * listings.get(ccyReference).get(ccy).longValue() * (ccyReference.getBasis())));
  					// get the rate for the
  					if (crossRate != 0) {
  						BigDecimal crossRateBD = BigDecimal.valueOf(crossRate);
  						// calculate the inverse by getting the basis of the currenct rate and setting the scale to ttha tof the quote currency.
  						BigDecimal inverseCrossRateBD = ((BigDecimal.valueOf(1.0 / (ccy.getBasis()))).divide(crossRateBD, ccyToUpdate.getScale(),
  								RoundingMode.HALF_EVEN));
  						// divine the rate by the basis fo the currency to be added
  						inverseCrossRateBD = inverseCrossRateBD.divide(BigDecimal.valueOf(ccyToUpdate.getBasis()));

  						inverseCrossRate = inverseCrossRateBD.longValue();
  					}
  				}

  				listings.get(ccyToUpdate).put(ccy, crossRate);
  				listings.get(ccy).put(ccyToUpdate, inverseCrossRate);

  			}
  		}
  		listings.get(ccyToUpdate).put(ccyToUpdate, (long) (1.0 / ccyToUpdate.getBasis()));
  		log.debug(this.getClass().getSimpleName() + ":updateRates -  updated rates to " + listings);
  	}
  }*/

  //	log.debug(this.getClass().getSimpleName() + ":updateRates -  updated rates to " + listings);

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    int count = 0;
    stringBuilder.append("{");
    for (Asset ccy : listings.keySet()) {
      if (count == 0)
        // crossCcy = listings.get(ccy);
        stringBuilder.append("{" + ccy + "=>");
      else stringBuilder.append("},{" + ccy + "=>");
      count++;

      for (Asset crossCcy : listings.get(ccy).keySet()) {
        stringBuilder.append(crossCcy + ":" + listings.get(ccy).get(crossCcy) + ", ");
      }
    }
    stringBuilder.append("}}");
    return stringBuilder.toString();
  }

  @Override
  public int hashCode() {

    return listings.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final ListingsMatrix other = (ListingsMatrix) obj;
    if (!ObjectUtils.equals(listings.keySet(), other.listings.keySet())) {
      return false;
    }

    return true;
  }
}
