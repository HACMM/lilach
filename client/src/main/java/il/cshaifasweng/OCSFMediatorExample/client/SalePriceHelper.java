package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.*;

import java.util.Date;

/**
 * Utility class for calculating sale prices and checking if sales are active.
 */
public class SalePriceHelper {
    
    /**
     * Checks if a sale is currently active (between start and end dates, and status is not Stashed).
     * Sales with status "Announced" or "Ongoing" are considered active if within date range.
     */
    public static boolean isSaleActive(Sale sale) {
        if (sale == null) {
            return false;
        }
        
        // Sale must not be Stashed
        if (sale.getStatus() == SaleStatus.Stashed) {
            return false;
        }
        
        Date now = new Date();
        Date startDate = sale.getStartDate();
        Date endDate = sale.getEndDate();
        
        if (startDate == null || endDate == null) {
            return false;
        }
        
        // Sale is active if current time is between start and end dates
        // Allow both Announced and Ongoing statuses (Announced sales can still apply discounts)
        boolean isInDateRange = now.compareTo(startDate) >= 0 && now.compareTo(endDate) <= 0;
        
        return isInDateRange;
    }
    
    /**
     * Checks if an ItemSale is currently active.
     */
    public static boolean isItemSaleActive(ItemSale itemSale) {
        if (itemSale == null) {
            return false;
        }
        try {
            Sale sale = itemSale.getSale();
            if (sale == null) {
                return false;
            }
            return isSaleActive(sale);
        } catch (Exception e) {
            // Sale might not be loaded (lazy loading issue)
            System.err.println("SalePriceHelper: Error checking ItemSale - Sale may not be loaded: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Calculates the sale price for an item, considering all active sales.
     * Returns the lowest price from all active sales, or the original price if no active sales.
     * 
     * @param item The item to calculate the sale price for
     * @return A SalePriceResult containing the original price, sale price, and whether a sale is active
     */
    public static SalePriceResult calculateSalePrice(Item item) {
        if (item == null) {
            return new SalePriceResult(0.0, 0.0, false);
        }
        
        double originalPrice = item.getPrice();
        double finalPrice = originalPrice;
        boolean hasActiveSale = false;
        
        if (item.getSales() != null && !item.getSales().isEmpty()) {
            for (ItemSale itemSale : item.getSales()) {
                try {
                    if (isItemSaleActive(itemSale)) {
                        hasActiveSale = true;
                        double discount = itemSale.getDiscount();
                        double discountedPrice = originalPrice;
                        
                        if (itemSale.getDiscountType() == DiscountType.PercentDiscount) {
                            discountedPrice = originalPrice * (1 - discount / 100);
                        } else if (itemSale.getDiscountType() == DiscountType.FlatDiscount) {
                            discountedPrice = originalPrice - discount;
                        }
                        
                        // Take the lowest price (best discount)
                        if (discountedPrice < finalPrice) {
                            finalPrice = discountedPrice;
                        }
                    }
                } catch (Exception e) {
                    // Sale might not be loaded - skip this ItemSale
                    System.err.println("SalePriceHelper: Error processing ItemSale (Sale may not be loaded): " + e.getMessage());
                }
            }
        }
        
        return new SalePriceResult(originalPrice, finalPrice, hasActiveSale);
    }
    
    /**
     * Result class containing price information for an item with potential sale.
     */
    public static class SalePriceResult {
        private final double originalPrice;
        private final double salePrice;
        private final boolean hasActiveSale;
        
        public SalePriceResult(double originalPrice, double salePrice, boolean hasActiveSale) {
            this.originalPrice = originalPrice;
            this.salePrice = salePrice;
            this.hasActiveSale = hasActiveSale;
        }
        
        public double getOriginalPrice() {
            return originalPrice;
        }
        
        public double getSalePrice() {
            return salePrice;
        }
        
        public boolean hasActiveSale() {
            return hasActiveSale;
        }
    }
}

